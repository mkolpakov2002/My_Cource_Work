package com.miem.mmkolpakov.coursework;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Arrays;

public class SendDataActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    //mac адрес устройства-робота
    String selectedDeviceId;
    //имя устройства-робота
    String selectedDeviceName;
    //отдельный поток для приёма и отправки информации
    private SendDataThread dataThreadForArduino;
    //отслеживание режима управления - с удержанием команд и без
    private boolean is_hold_command;
    // комманда посылаемая на устройство-робота
    private final byte[] message = new byte[32];
    //предыдущая команда, отправленная на робота (для отслеживания является ли посылаемая команда "новой")
    private byte prevCommand = 0;
    //Окно терминал, куда будет выводиться вся информация
    TextView outputText;
    //Диалог при соединении с устройством
    ProgressDialog progressOfConnectionDialog;
    //таг для логов
    private final String TAG = "SendDataActivity";
    //класс хранящий логику значений команд для роботов
    ProtocolRepo protocolRepo;
    //переменная для отслеживания необходимости переподключения
    boolean isNeedToRestartConnection = false;
    //Текст над seek bar, отображающий текущее время задержки команд
    TextView seekBarTextView;
    //переменная для отслеживания последнего времени нажатия на кнопку управления роботом
    long lastClickTime = 0;
    //стандартное время задержки, может изменяться пользователем с помощью seek bar
    int timeForWaiting = 2;
    //при отключенном удержании команд и неком времени задержки необходимо посылать команду стоп согласно времени задержки
    boolean isNeedSendStop = false;
    //отслеживание видимости экрана приложения
    static boolean active = false;
    //Переменная для хранения времени между нажатиями кнопки назад
    private static long back_pressed = 0;
    //отслеживание момента прекращения работы Activity
    boolean isDestroyingActivity = false;
    AlertDialog dialog = null;
    boolean isRestartDialogShown = false;
    boolean isAlreadyWaitingForBtEn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);
        // Получаем данные об устройстве
        selectedDeviceId = DeviceHandler.getDeviceId();
        selectedDeviceName = DeviceHandler.getDeviceName();
        //инициализация окна терминала
        outputText = findViewById(R.id.incoming_data);
        outputText.setMovementMethod(new ScrollingMovementMethod());
        //инициализация объекта класса с кодами команд
        protocolRepo = new ProtocolRepo("main_protocol");
        //запуск потока для общения с устройством-роботом
        dataThreadForArduino = new SendDataThread(this);
        dataThreadForArduino.setSocket(DeviceHandler.getSocket());
        dataThreadForArduino.start();
        //удержание команд по-умолчанию выключено
        is_hold_command = false;
        //инициализация кнопок управления
        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_up).setOnTouchListener(touchListener);
        findViewById(R.id.button_down).setOnTouchListener(touchListener);
        findViewById(R.id.button_left).setOnTouchListener(touchListener);
        findViewById(R.id.button_right).setOnTouchListener(touchListener);
        //кнопка стоп по-умолчанию выключена и доступна только в режиме удержания команд
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
            isNeedSendStop = false;
            refreshActivity();
        }
    };

    @Override protected void onRestart() {
        super.onRestart();
        active = true;
        isAlreadyWaitingForBtEn = false;
        refreshActivity();
    }

    synchronized void restartConnection() {
        if(!getIsActivityNeedsStopping("1") && active){
            if (dialog != null && isRestartDialogShown) {
                dialog.dismiss();
                dialog = null;
            }
            // начало показа диалога о соединении
            progressOfConnectionDialog = new ProgressDialog(this);
            progressOfConnectionDialog.setMessage(getResources().getString(R.string.connection_title));
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
    }

    // Метод для вывода всплывающих данных на экран
    public void showToast(String outputInfoString) {
        if (!getIsActivityNeedsStopping("1") && active){
            Toast outputInfoToast = Toast.makeText(this, outputInfoString, Toast.LENGTH_LONG);
            outputInfoToast.show();
        }
    }

    //Обновляем внешний вид приложения, если Bluetooth выключен завершаем Activity и Сервис
    public void refreshActivity() {
        Log.d(TAG, "Обновление состояния Activity");
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.d(TAG, "Bluetooth выключен, предложим включить");
            isNeedToRestartConnection("1");
            if(!isAlreadyWaitingForBtEn){
                isAlreadyWaitingForBtEn = true;
                //открытие системного диалога с предложением включить Bluetooth
                Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                int REQUEST_ENABLE_BT = 1;
                startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
            }
        } else if(isNeedToRestartConnection("2") && !getIsActivityNeedsStopping("1")) {
            Log.d(TAG, "Bluetooth включён, перестартуем соединение");
            connectionFailed();
        }
    }

    //для кода 2 возвращает true только для первого вызова переподключения
    //затем возвращает всё время false, т.к. окно переподключения уже вызвано
    //код 3 только для повторной попытки перподключения, если первая такая попытка неуспешна
    //код 1 установит необходимость переподключиться
    //synchronized нужен, т.к. к переменной надо обращаться последовательно, иначе БАГИ
    //на самом деле не работает как задумывалось, помогла добавка isRestartDialogShown
    public synchronized boolean isNeedToRestartConnection(String code) {

        if (code.equals("2") && isNeedToRestartConnection){
            isNeedToRestartConnection = false;
            return true;
        } else if (code.equals("2")){
            return false;
        } else if (code.equals("3")){
            isNeedToRestartConnection = false;
            return true;
        } else {
            isNeedToRestartConnection = true;
            return true;
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
        if(dialog!=null){
            dialog.dismiss();
            dialog = null;
        }
        if(progressOfConnectionDialog!=null){
            progressOfConnectionDialog.dismiss();
            progressOfConnectionDialog = null;
        }
        Log.d(TAG, "Завершение работы Activity");
    }

    void sendStopAndDisconnect() {
        if(!active && BluetoothAdapter.getDefaultAdapter().isEnabled() && isNeedSendStop){
            printDataToTextView(getResources().getString(R.string.app_not_visible_with_stop));
            Log.d(TAG, getResources().getString(R.string.motion_stop));
            printDataToTextView(getResources().getString(R.string.motion_stop));
            isNeedSendStop = false;
            makeAndSendMessage("STOP");
            try
            {
                printDataToTextView(getResources().getString(R.string.disconnecting_from_the_device));
                dataThreadForArduino.Disconnect();                 // отсоединяемся от bluetooth
                isNeedToRestartConnection("1");
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
    protected void onStop() {
        super.onStop();
        active = false;
        if(!isNeedSendStop && !isDestroyingActivity){
            isNeedSendStop = true;
            if (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()>0){
                ImageButton button = findViewById(R.id.button_stop);
                button.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendStopAndDisconnect();
                        }
                    }, lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis());
                } else {
                    sendStopAndDisconnect();
                }
            } else if(isNeedSendStop && !isDestroyingActivity && BluetoothAdapter.getDefaultAdapter().isEnabled()){
                printDataToTextView(getResources().getString(R.string.app_not_visible_with_stop));
                Log.d(TAG, "Приложение свёрнуто");
            } else {
            Log.d(TAG, "Завершение работы Activity");
        }
    }

    @Override
    public void onClick(View v) {
        //кнопка стоп, доступная только в режиме удержания команды
        if(lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis() && !isNeedSendStop){
            Log.d(TAG, getResources().getString(R.string.motion_stop));
            printDataToTextView(getResources().getString(R.string.motion_stop));
            makeAndSendMessage("STOP");
        } else if(lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis() && !isNeedSendStop){
            buttonPressedTooFast();
        } else if (isNeedSendStop){
            printWaitForStopCommand();
        }
    }
    void buttonPressedTooFast(){
        printDataToTextView(getResources().getString(R.string.wait_control) +
                (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()) / 1000
                + getResources().getString(R.string.second));
    }
    void printWaitForStopCommand(){
        printDataToTextView(getResources().getString(R.string.wait_stop) +
                (lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis()) / 1000
                + getResources().getString(R.string.second));
    };
    void buttonPressedTooFastWithoutHoldCommand(String command, String message, View v){
        if(BluetoothAdapter.getDefaultAdapter().isEnabled()){
            printWaitForStopCommand();
            isNeedSendStop = true;
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, message);
                    printDataToTextView(message);
                    isNeedSendStop = false;
                    makeAndSendMessage(command);
                    if(!active){
                        sendStopAndDisconnect();
                    }
                }
            }, lastClickTime + (long) timeForWaiting * 1000 - System.currentTimeMillis());
        }
    }
    boolean isCommandAccepted = true;
    View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_DOWN) { // если нажали на кнопку и не важно есть удержание команд или нет
                isCommandAccepted = true;
                int id = v.getId();
                if (id == R.id.button_up && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, getResources().getString(R.string.button_forward));
                    printDataToTextView(getResources().getString(R.string.button_forward));
                    makeAndSendMessage("FORWARD");
                } else if (id == R.id.button_down && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, getResources().getString(R.string.button_backward));
                    printDataToTextView(getResources().getString(R.string.button_backward));
                    makeAndSendMessage("BACK");
                } else if (id == R.id.button_left  && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, getResources().getString(R.string.button_leftward));
                    printDataToTextView(getResources().getString(R.string.button_leftward));
                    makeAndSendMessage("LEFT");
                } else if (id == R.id.button_right  && lastClickTime + (long) timeForWaiting *1000 <= System.currentTimeMillis()&&(!isNeedSendStop)) {
                    Log.d(TAG, getResources().getString(R.string.button_rightward));
                    printDataToTextView(getResources().getString(R.string.button_rightward));
                    makeAndSendMessage("RIGHT");
                } else if (lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis() &&(!isNeedSendStop)){
                    buttonPressedTooFast();
                    isCommandAccepted = false;
                } else if(isNeedSendStop){
                    printWaitForStopCommand();
                    isCommandAccepted = false;
                }
            }
            else if(event.getAction() == MotionEvent.ACTION_UP && !is_hold_command && isCommandAccepted)  // если отпустили кнопку и нет удержания команд
            {
                int id = v.getId();
                if (id == R.id.button_up) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, getResources().getString(R.string.stop_forward));
                        printDataToTextView(getResources().getString(R.string.stop_forward));
                        makeAndSendMessage("FORWARD_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("FORWARD_STOP", getResources().getString(R.string.stop_forward), v);
                    }
                } else if (id == R.id.button_down) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, getResources().getString(R.string.stop_backward));
                        printDataToTextView(getResources().getString(R.string.stop_backward));
                        makeAndSendMessage("BACK_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("BACK_STOP", getResources().getString(R.string.stop_backward), v);
                    }
                } else if (id == R.id.button_left) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, getResources().getString(R.string.stop_leftward));
                        printDataToTextView(getResources().getString(R.string.stop_leftward));
                        makeAndSendMessage("LEFT_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("LEFT_STOP", getResources().getString(R.string.stop_leftward), v);
                    }
                } else if (id == R.id.button_right) {
                    if(lastClickTime + (long) timeForWaiting * 1000 <= System.currentTimeMillis()){
                        Log.d(TAG, getResources().getString(R.string.stop_rightward));
                        printDataToTextView(getResources().getString(R.string.stop_rightward));
                        makeAndSendMessage("RIGHT_STOP");
                    } else {
                        buttonPressedTooFastWithoutHoldCommand("RIGHT_STOP", getResources().getString(R.string.stop_rightward), v);
                    }
                } else if (lastClickTime + (long) timeForWaiting *1000 > System.currentTimeMillis()){
                    buttonPressedTooFast();
                }
            }
            return false;
        }
    };


    void makeAndSendMessage(String code){
        if(!isNeedSendStop && BluetoothAdapter.getDefaultAdapter().isEnabled()){
            lastClickTime = System.currentTimeMillis();
            message[5] = ProtocolRepo.getCommandTypeByte("type_move");
            message[6] =  ProtocolRepo.getMoveCommandByte(code);
            if (prevCommand == message[6]){
                message[4] = ProtocolRepo.getCommandTypeByte("redo_command");
            } else {
                message[4] = ProtocolRepo.getCommandTypeByte("new_command");
                prevCommand = message[6];
            }
            StringBuilder bytesArray = new StringBuilder();
            for (int i = 0; i < 32; i++){
                bytesArray.append(String.format("%X", message[i]));
                bytesArray.append(" ");
            }
            printDataToTextView(getResources().getString(R.string.sending_data) + "\n"+ "[ " + bytesArray + "]");
            dataThreadForArduino.sendData(message);
        }
    }

    public synchronized void printDataToTextView(String printData){
        Log.d(TAG, getResources().getString(R.string.printed_information) + printData);
        outputText.append("\n" + "---" + "\n" + printData);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        refreshActivity();
        if (buttonView.getId() == R.id.switch_hold_command_mm) {
            is_hold_command = isChecked;
            if (is_hold_command) {
                Log.d(TAG, getResources().getString(R.string.hold_command_on));
                Toast.makeText(this, getResources().getString(R.string.hold_command_on), Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_stop).setEnabled(true);
            } else {
                Log.d(TAG, getResources().getString(R.string.hold_command_off));
                Toast.makeText(this, getResources().getString(R.string.hold_command_off), Toast.LENGTH_SHORT).show();
                findViewById(R.id.button_stop).setEnabled(false);
            }
        }
    }

    //Результат работы Service при неуспешном соединении
    private final BroadcastReceiver mMessageReceiverNotSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast(getResources().getString(R.string.connection_not_successful));
            //connectionFailed
            Log.d(TAG, "...Соединение неуспешно, результат в SendDataActivity...");
            if(isNeedToRestartConnection("3") && !getIsActivityNeedsStopping("1")) {
                //вызов окна о неуспешном соединении
                connectionFailed();
            }
        }
    };

    //результат работы Service при успешном соединении
    private final BroadcastReceiver mMessageReceiverSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "...Соединение успешно, результат в SendDataActivity...");
            Log.d(TAG, "...Создание нового потока...");
            //передача данных в поток
            dataThreadForArduino = new SendDataThread(SendDataActivity.this);
            dataThreadForArduino.setSocket(DeviceHandler.getSocket());
            dataThreadForArduino.start();
            Log.d(TAG, "...Поток пущен...");
            //Устройство подключено, Service выполнился успешно
            showToast(getResources().getString(R.string.сonnection_successful));
            //скрытие окна о соединении
            if (progressOfConnectionDialog != null && progressOfConnectionDialog.isShowing()) {
                progressOfConnectionDialog.dismiss();
                progressOfConnectionDialog = null;
            }
            if (dialog != null && !isRestartDialogShown) {
                dialog.dismiss();
                dialog = null;
            }
            printDataToTextView(getResources().getString(R.string.reconnection_successful));
        }
    };

    //диалог о неуспешном соединении
    synchronized void connectionFailed(){
        if (!getIsActivityNeedsStopping("1") && !isRestartDialogShown){
            isRestartDialogShown = true;
            // объект Builder для создания диалогового окна
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
            // добавляем различные компоненты в диалоговое окно
            builder.setTitle(getResources().getString(R.string.error_title));
            builder.setMessage(getResources().getString(R.string.question_try_to_reconnect));
            builder.setCancelable(false);
            // устанавливаем кнопку, которая отвечает за позитивный ответ
            builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
                //переподключение к устройству
                restartConnection();
                isRestartDialogShown = false;
            });
            builder.setNegativeButton(getResources().getString(R.string.exit), (dialog, which) -> {
                isRestartDialogShown = false;
                //завершение активити
                getIsActivityNeedsStopping("2");
                finish();
            });
            // объект Builder создал диалоговое окно и оно готово появиться на экране
            // Создание alert dialog и изменение цвета его кнопок
            dialog = builder.create();
            dialog.setOnShowListener(arg0 -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.colorAccent));
            });
            dialog.setOnShowListener(arg0 -> {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor( R.color.color_red));
            });
            //скрытие окна о соединении
            if (progressOfConnectionDialog != null && progressOfConnectionDialog.isShowing()) {
                progressOfConnectionDialog.dismiss();
                progressOfConnectionDialog = null;
            }
            // вызываем этот метод, чтобы показать AlertDialog на экране пользователя
            dialog.show();
        }
    }

    //метод, вызываемый при нажатии кнопки назад на главном экране
    @Override
    public void onBackPressed() {
        //иначе даём возможность выйти из приложения, но по двойному нажатию кнопки назад
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            getIsActivityNeedsStopping("2");
            active = false;
            dataThreadForArduino.Disconnect();
            finish();
        } else {
            //показ сообщения, о необходимости второго нажатия кнопки назад при выходе
            showToast(getResources().getString(R.string.press_again));
        }
        back_pressed = System.currentTimeMillis();
    }

    synchronized boolean getIsActivityNeedsStopping(String code){
        if(code.equals("2")){
            isDestroyingActivity = true;
        }
        return isDestroyingActivity;
    }

}
