package com.miem.mmkolpakov.coursework;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SendDataActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    String selectedDeviceId;
    String selectedDeviceName;
    private SendDataThread dataThreadForArduino;
    private boolean is_hold_command;
    private final byte[] message = new byte[32];      // комманда посылаемая на arduino
    private byte prevCommand = 0;
    TextView outputText;
    //Диалог при соединении с устройством
    ProgressDialog progressOfConnectionDialog;

    private final String TAG = "SendDataActivity";
    BluetoothSocket clientSocket;
    ProtocolRepo protocolRepo;
    boolean isNeedToRestartConnection = false;
    OutputStream mmOutStream;
    InputStream mmInStream;
    TextView seekBarTextView;
    long lastClickTime = 0;
    int timeForWaiting = 2;
    boolean isNeedSendStop = false;
    static boolean active = false;
    //Переменная для хранения времени между нажатиями кнопки назад
    private static long back_pressed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);

        // Получаем данные об устройстве
        selectedDeviceId = DeviceHandler.getDeviceId();
        selectedDeviceName = DeviceHandler.getDeviceName();

        outputText = findViewById(R.id.incoming_data);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        protocolRepo = new ProtocolRepo("main_protocol");
        dataThreadForArduino = new SendDataThread(this);
        clientSocket = DeviceHandler.getSocket();
        dataThreadForArduino.setSocket(clientSocket);
        dataThreadForArduino.setSelectedDevice(selectedDeviceId);
        dataThreadForArduino.start();

        is_hold_command = false;

        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_up).setOnTouchListener(touchListener);
        findViewById(R.id.button_down).setOnTouchListener(touchListener);
        findViewById(R.id.button_left).setOnTouchListener(touchListener);
        findViewById(R.id.button_right).setOnTouchListener(touchListener);

        is_hold_command = false;
        findViewById(R.id.button_stop).setEnabled(false);

        SwitchMaterial hold_command = findViewById(R.id.switch_hold_command_mm);
        hold_command.setOnCheckedChangeListener(this);

        Arrays.fill(message, (byte) 0x0);

        //получение сообщений из сервиса по подключению
        registerReceiver(mMessageReceiverNotSuccess, new IntentFilter("not_success_code_2"));
        registerReceiver(mMessageReceiverSuccess, new IntentFilter("success_code_2"));
        registerReceiver(BluetoothStateChanged, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        message[0] = ProtocolRepo.getDeviceCodeByte("class_android");
        message[1] = ProtocolRepo.getDeviceCodeByte("type_computer"); // класс и тип устройства отправки
        //TODO
        //нужен ли выбор класса устройства
        message[2] = ProtocolRepo.getDeviceCodeByte("class_arduino"); // класс и тип устройства приема
        //тип устройства
        message[3] = ProtocolRepo.getDeviceCodeByte(DeviceHandler.getDeviceType());

        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBarTextView = (TextView) findViewById(R.id.seekBarValue);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                timeForWaiting = progress;
                setSeekBarProgress(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        setSeekBarProgress("2");
        active = true;
    }

    void setSeekBarProgress(String progress){
        seekBarTextView.setText(String.format(getResources().getString(R.string.seekBarProgress), progress));
    }

    //выполняемый код при изменении состояния bluetooth
    private final BroadcastReceiver BluetoothStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshActivity();
        }
    };

    @Override protected void onRestart() {
        super.onRestart();
        active = true;
        refreshActivity();
    }

    void restartConnection() {
        // начало показа диалога о соединении
        progressOfConnectionDialog = new ProgressDialog(this);
        progressOfConnectionDialog.setMessage("Соединение...");
        progressOfConnectionDialog.setCancelable(false);
        progressOfConnectionDialog.setInverseBackgroundForced(false);
        progressOfConnectionDialog.show();
        // запускаем длительную операцию подключения в Service
        Intent startBluetoothConnectionService = new Intent(this, BluetoothConnectionService.class);
        //передача данных о адресе и имени устройства
        startBluetoothConnectionService.putExtra("idOfDevice", selectedDeviceId);
        startBluetoothConnectionService.putExtra("nameOfDevice", selectedDeviceName);
        startBluetoothConnectionService.putExtra("startCode", 2);
        startService(startBluetoothConnectionService);
    }

    protected void finishActivity() {
        // Выходим из Activity
        finish();
    }

    // Метод для вывода всплывающих данных на экран
    public void showToast(String outputInfoString) {
        Toast outputInfoToast = Toast.makeText(this, outputInfoString, Toast.LENGTH_LONG);
        outputInfoToast.show();
    }

    //Обновляем внешний вид приложения, если Bluetooth выключен завершаем Activity и Сервис
    public void refreshActivity() {
        Log.d(TAG, "Обновление состояния Activity");
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.d(TAG, "Bluetooth выключен, предложим включить");
            isNeedToRestartConnection = true;
            //открытие системного диалога с предложением включить Bluetooth
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        } else {
            if(isNeedToRestartConnection){
                isNeedToRestartConnection = false;
                Log.d(TAG, "Bluetooth включён, перестартуем соединение");
                restartConnection();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Диалог включения Bluetooth закрыт");
        refreshActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        active = false;
        Log.d(TAG, "Завершение работы Activity");
    }

    void disconnect() {
        if(!active){
            printDataToTextView("Приложение свёрнуто, согласно времени задержки посылаю команду стоп и отключаю устройство", 3);
            Log.d(TAG, "Остановка движения");
            printDataToTextView("Остановка движения", 3);
            isNeedSendStop = false;
            makeMessage("STOP");
            try
            {
                printDataToTextView("Отключение от устройства..." , 3);
                dataThreadForArduino.Disconnect();                 // отсоединяемся от bluetooth
                isNeedToRestartConnection = true;
            }
            catch (Exception e)
            {
                Log.d(TAG, "Ошибка отключения от устройства");
            }
        } else {
            isNeedSendStop = false;
            //пользователь вернулся в приложение, можно не отправлять команду стоп и не отключаться
        }
    }
    @Override
    protected void onStop()
    {
        super.onStop();
        active = false;
        if(!isNeedSendStop){
            isNeedSendStop = true;
            if (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()>0){
                ImageButton button = findViewById(R.id.button_stop);
                button.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                    }
                }, lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis());
            } else {
                disconnect();
            }
        } else {
            printDataToTextView("Приложение свёрнуто, согласно времени задержки посылаю команду стоп", 3);
        }
        Log.d(TAG, "Приложение свёрнуто");
    }

    @Override
    public void onClick(View v)
    {
        refreshActivity();
        if(lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis() && !isNeedSendStop){
            Log.d(TAG, "Остановка движения");
            printDataToTextView("Остановка движения", 2);
            makeMessage("STOP");
        } else if(lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis() && !isNeedSendStop){
            buttonPressedTooFast();
        } else if (isNeedSendStop){
            printWaitForStop();
        }
    }
    void buttonPressedTooFast(){
        printDataToTextView("Подождите до окончания блокировки управления - " +
                (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()) / 1000
                + "с.", 2);
    }
    void printWaitForStop(){
        printDataToTextView("Подождите пока отправится команда стоп, до её отправки осталось " +
                (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()) / 1000
                + "с.", 2);
    };
    void buttonPressedTooFastWithoutHoldCommand(String command, String message, View v){
        printWaitForStop();
        isNeedSendStop = true;
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, message);
                printDataToTextView(message, 3);
                isNeedSendStop = false;
                makeMessage(command);
                if(!active){
                    disconnect();
                }
            }
        }, lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis());
    }
    boolean isAccepted = true;
    View.OnTouchListener touchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)                        // если нажали на кнопку и не важно есть удержание команд или нет
            {
                isAccepted = true;
                int id = v.getId();
                if (id == R.id.button_up && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, "Движение вперед");
                    printDataToTextView("Движение вперёд", 2);
                    makeMessage("FORWARD");
                } else if (id == R.id.button_down && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, "Движение назад");
                    printDataToTextView("Движение назад", 2);
                    makeMessage("BACK");
                } else if (id == R.id.button_left  && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, "Движение влево");
                    printDataToTextView("Движение влево", 2);
                    makeMessage("LEFT");
                } else if (id == R.id.button_right  && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, "Движение вправо");
                    printDataToTextView("Движение вправо", 2);
                    makeMessage("RIGHT");
                } else if (lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis() &&(!isNeedSendStop)){
                    buttonPressedTooFast();
                    isAccepted = false;
                } else if(isNeedSendStop){
                    printWaitForStop();
                    isAccepted = false;
                }
            }
            else if(event.getAction() == MotionEvent.ACTION_UP && !is_hold_command && isAccepted)  // если отпустили кнопку и нет удержания команд
            {
                int id = v.getId();
                if (id == R.id.button_up) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, "Остановка движения вперёд");
                        printDataToTextView("Остановка движения вперёд", 2);
                        makeMessage("FORWARD_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("FORWARD_STOP", "Остановка движения вперёд", v);
                    }
                } else if (id == R.id.button_down) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, "Остановка движения назад");
                        printDataToTextView( "Остановка движения назад", 2);
                        makeMessage("BACK_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("BACK_STOP", "Остановка движения назад", v);
                    }
                } else if (id == R.id.button_left) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, "Остановка движения влево");
                        printDataToTextView("Остановка движения влево", 2);
                        makeMessage("LEFT_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("LEFT_STOP", "Остановка движения влево", v);
                    }
                } else if (id == R.id.button_right) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, "Остановка движения вправо");
                        printDataToTextView("Остановка движения вправо", 2);
                        makeMessage("RIGHT_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("RIGHT_STOP", "Остановка движения вправо", v);
                    }
                } else if (lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis()){
                    buttonPressedTooFast();
                }
            }
            return false;
        }
    };


    void makeMessage(String code){
        refreshActivity();
        if(!isNeedSendStop){
            lastClickTime = System.currentTimeMillis();
            message[5] = ProtocolRepo.getCommandTypeByte("type_move");
            message[6] =  ProtocolRepo.getMoveCommandByte(code);
            if (prevCommand == message[6]){
                message[4] = ProtocolRepo.getCommandTypeByte("redo_command");
            } else {
                message[4] = ProtocolRepo.getCommandTypeByte("new_command");
                prevCommand = message[6];
            }
            printDataToTextView("Отправляем данные:" + "\n"+ Arrays.toString(message), 2);
            dataThreadForArduino.sendData(message);
        }
    }

    public synchronized void printDataToTextView(String printData, int code){
        Log.d(TAG, "Output data: " + printData);
        outputText.append("\n" + "---" + "\n" + printData);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        refreshActivity();
        if (buttonView.getId() == R.id.switch_hold_command_mm) {
            is_hold_command = isChecked;
            if (is_hold_command) {
                Log.d(TAG, "Удерживание комманды включено");
                Toast.makeText(this, "Удерживание комманды включено: ", Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_stop).setEnabled(true);
            } else {
                Log.d(TAG, "Удерживание комманды отключено");
                Toast.makeText(this, "Удерживание комманды отключено: ", Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_stop).setEnabled(false);
            }
        }
    }

    //Результат работы Service при неуспешном соединении
    private final BroadcastReceiver mMessageReceiverNotSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast("Соединение не успешно");
            Log.d(TAG, "...Соединение неуспешно, результат в SendDataActivity...");
            //скрытие окна о соединении
            progressOfConnectionDialog.hide();
            isNeedToRestartConnection = true;
            //вызов окна о неуспешном соединении
            connectionFailed();
        }
    };

    //результат работы Service при успешном соединении
    private final BroadcastReceiver mMessageReceiverSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isNeedToRestartConnection = false;
            Log.d(TAG, "...Соединение успешно, результат в SendDataActivity...");
            Log.d(TAG, "...Создание нового потока...");
            //передача данных в поток
            dataThreadForArduino = new SendDataThread(SendDataActivity.this);
            clientSocket = DeviceHandler.getSocket();
            dataThreadForArduino.setSocket(clientSocket);
            dataThreadForArduino.setSelectedDevice(selectedDeviceId);
            dataThreadForArduino.start();
            Log.d(TAG, "...Поток пущен...");
            //Устройство подключено, Service выполнился успешно
            showToast("Соединение успешно");
            //скрытие окна о соединении
            progressOfConnectionDialog.hide();
            printDataToTextView("Переподключение успешно" , 3);
        }
    };

    //диалог о неуспешном соединении
    private void connectionFailed(){
        // объект Builder для создания диалогового окна
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        // добавляем различные компоненты в диалоговое окно
        builder.setTitle("Ошибка");
        builder.setMessage("Соединение неуспешно. Попробовать снова?");
        builder.setCancelable(false);
        // устанавливаем кнопку, которая отвечает за позитивный ответ
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            //переподключение к устройству
            restartConnection();
        });
        builder.setNegativeButton(getResources().getString(R.string.exit), (dialog, which) -> {
            //завершение активити
            finishActivity();
        });
        // объект Builder создал диалоговое окно и оно готово появиться на экране
        // Создание alert dialog и изменение цвета его кнопок
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(arg0 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.colorAccent));
        });
        dialog.setOnShowListener(arg0 -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor( R.color.color_red));
        });
        // вызываем этот метод, чтобы показать AlertDialog на экране пользователя
        dialog.show();
    }

    //метод, вызываемый при нажатии кнопки назад на главном экране
    @Override
    public void onBackPressed() {
        //иначе даём возможность выйти из приложения, но по двойному нажатию кнопки назад
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            //показ сообщения, о необходимости второго нажатия кнопки назад при выходе
            showToast(getResources().getString(R.string.press_again));
        }
        back_pressed = System.currentTimeMillis();
    }
}
