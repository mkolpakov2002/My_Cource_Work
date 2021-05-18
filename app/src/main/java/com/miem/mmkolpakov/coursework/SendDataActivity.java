package com.miem.mmkolpakov.coursework;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);

        // Получаем данные об устройстве
        selectedDeviceId = DeviceHandler.getDeviceId();
        selectedDeviceName = DeviceHandler.getDeviceName();

        outputText = findViewById(R.id.incoming_data);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        //TODO
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
        message[1] = ProtocolRepo.getDeviceCodeByte("class_computer"); // класс и тип устройства отправки
        message[2] = ProtocolRepo.getDeviceCodeByte("class_arduino"); // класс и тип устройства приема
        //тип устройства
        message[3] = ProtocolRepo.getDeviceCodeByte(DeviceHandler.getDeviceClass());
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
        Log.d(TAG, "Завершение работы Activity");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "Приложение свёрнуто");
        outputText.append("\n"+ "---");
        outputText.append("\n"+ "Приложение свёрнуто");
        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
        dataThreadForArduino.Send_Data(message);
        isNeedToRestartConnection = true;
        message[4] = (byte) 0x0a;
        message[5] = (byte) 0xa1;
        message[6] = (byte) 0x7f;

        try
        {
            dataThreadForArduino.Disconnect();                 // отсоединяемся от bluetooth
        }
        catch (Exception e)
        {
            Log.d(TAG, "Приложение свёрнуто");
        }
    }

    @Override
    public void onClick(View v)
    {
        refreshActivity();
        Log.d(TAG, "Остановка движения");
        outputText.append("\n"+ "---");
        outputText.append("\n"+ "Остановка движения");
        makeMessage("STOP");
    }

    View.OnTouchListener touchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN)                        // если нажали на кнопку и не важно есть удержание команд или нет
            {
                int id = v.getId();
                if (id == R.id.button_up) {
                    Log.d(TAG, "Движение вперед");
                    outputText.append("\n" + "---");
                    outputText.append("\n" + "Движение вперед");
                    makeMessage("FORWARD");
                } else if (id == R.id.button_down) {
                    Log.d(TAG, "Движение назад");
                    outputText.append("\n" + "---");
                    outputText.append("\n" + "Движение назад");
                    makeMessage("BACK");
                } else if (id == R.id.button_left) {
                    Log.d(TAG, "Движение влево");
                    outputText.append("\n" + "---");
                    outputText.append("\n" + "Движение влево");
                    makeMessage("LEFT");
                } else if (id == R.id.button_right) {
                    Log.d(TAG, "Движение вправо");
                    outputText.append("\n" + "---");
                    outputText.append("\n" + "Движение вправо");
                    makeMessage("RIGHT");
                }
            }
            else if(event.getAction() == MotionEvent.ACTION_UP)             // если отпустили кнопку
            {
                if(!is_hold_command)    // и нет удержания команд то все кнопки отправляют команду стоп
                {
                    int id = v.getId();
                    if (id == R.id.button_up) {
                        Log.d(TAG, "Остановка движения вперёд");
                        outputText.append("\n" + "---");
                        outputText.append("\n" + "Остановка движения вперёд");
                        makeMessage("FORWARD_STOP");
                    } else if (id == R.id.button_down) {
                        Log.d(TAG, "Остановка движения назад");
                        outputText.append("\n" + "---");
                        outputText.append("\n" + "Остановка движения назад");
                        makeMessage("BACK_STOP");
                    } else if (id == R.id.button_left) {
                        Log.d(TAG, "Остановка движения влево");
                        outputText.append("\n" + "---");
                        outputText.append("\n" + "Остановка движения влево");
                        makeMessage("LEFT_STOP");
                    } else if (id == R.id.button_right) {
                        Log.d(TAG, "Остановка движения вправо");
                        outputText.append("\n" + "---");
                        outputText.append("\n" + "Остановка движения вправо");
                        makeMessage("RIGHT_STOP");
                    }
                }
            }
            return false;
        }
    };

    void makeMessage(String code){
        refreshActivity();
        message[5] = ProtocolRepo.getCommandTypeByte("type_move");
        message[6] =  ProtocolRepo.getMoveCommandByte(code);
        if (prevCommand == message[6]){
            message[4] = ProtocolRepo.getCommandTypeByte("redo_command");
        } else {
            message[4] = ProtocolRepo.getCommandTypeByte("new_command");
            prevCommand = message[6];
        }
        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
        dataThreadForArduino.Send_Data(message);
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
            //скрытие окна о соединении
            progressOfConnectionDialog.hide();
            //вызов окна о неуспешном соединении
            connectionFailed();
        }
    };

    //результат работы Service при успешном соединении
    private final BroadcastReceiver mMessageReceiverSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //передача данных в поток
            clientSocket = DeviceHandler.getSocket();
            dataThreadForArduino.setSocket(clientSocket);
            //Устройство подключено, Service выполнился успешно
            showToast("Соединение успешно");
            //скрытие окна о соединении
            progressOfConnectionDialog.hide();
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
}
