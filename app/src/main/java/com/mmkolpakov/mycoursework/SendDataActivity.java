package com.mmkolpakov.mycoursework;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class SendDataActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static boolean stateOfMessage;
    String sending_data;
    String selectedDeviceId;
    String selectedDeviceName;
    private SendDataThread dataThreadForArduino;
    private boolean is_hold_command;
    private Timer arduino_timer;            // таймер для arduino
    private String[] pre_str_sens_data;             // форматирование вывода данных с сенсоров
    private int[] sens_data;                        // непосредственно данные с сенсоров
    private final byte[] message= new byte[32];      // комманда посылаемая на arduino
    private byte prevCommand = 0;
    static TextView outputText;

    private static final String TAG = "SendDataActivity";
    public BluetoothAdapter btAdapter;
    BluetoothSocket clientSocket;
    private int[] my_data;
    private TextView text_sens_data;
    HashMap<String, Byte> getDevicesID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        // Получаем данные об устройстве из Main Activity
        Bundle arguments = getIntent().getExtras();
        selectedDeviceId = arguments.get("idOfDevice").toString();
        selectedDeviceName = arguments.get("nameOfDevice").toString();

        TextView deviceInfo = findViewById(R.id.textViewNameManual);
        deviceInfo.setText("Устройство: " + selectedDeviceName + "\n MAC: " + selectedDeviceId);

        outputText = findViewById(R.id.incoming_data);
        outputText.setMovementMethod(new ScrollingMovementMethod());

        getDevicesID = new ProtocolRepo("main_protocol");
        dataThreadForArduino = new SendDataThread();
        clientSocket = SocketHandler.getSocket();
        dataThreadForArduino.setSocket(clientSocket);
        dataThreadForArduino.setSelectedDevice(selectedDeviceId);
        dataThreadForArduino.start();

        MainActivity.isItemSelected = false;
        MainActivity.stateOfToastAboutConnection = false;

        pre_str_sens_data = new String[5];
        pre_str_sens_data[0] = "     0º \t\t-\t\t ";
        pre_str_sens_data[1] = " -45º \t\t-\t\t ";
        pre_str_sens_data[2] = "  45º \t\t-\t\t ";
        pre_str_sens_data[3] = " -90º \t\t-\t\t ";
        pre_str_sens_data[4] = "  90º \t\t-\t\t ";

        sens_data = new int[5];
        my_data = new int[12];

        is_hold_command = false;

        arduino_timer = new Timer();
        // функция выполняющаяся при тике таймера для arduino
        TimerTask arduino_timer_task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Data_request();
                    }
                });
            }
        };



        //refreshActivity.start();

        Button button_left_45 = findViewById(R.id.button_left_45);
        button_left_45.setVisibility(View.INVISIBLE);
        Button button_right_45 = findViewById(R.id.button_right_45);
        button_right_45.setVisibility(View.INVISIBLE);
        Button button_left_90 = findViewById(R.id.button_left_90);
        button_left_90.setVisibility(View.INVISIBLE);
        Button button_right_90 = findViewById(R.id.button_right_90);
        button_right_90.setVisibility(View.INVISIBLE);


        findViewById(R.id.button_stop).setOnClickListener(this);
        findViewById(R.id.button_left_45).setOnClickListener(this);
        findViewById(R.id.button_right_45).setOnClickListener(this);
        findViewById(R.id.button_left_90).setOnClickListener(this);
        findViewById(R.id.button_right_90).setOnClickListener(this);

        findViewById(R.id.button_up).setOnTouchListener(touchListener);
        findViewById(R.id.button_down).setOnTouchListener(touchListener);
        findViewById(R.id.button_left).setOnTouchListener(touchListener);
        findViewById(R.id.button_right).setOnTouchListener(touchListener);

        is_hold_command = false;
        findViewById(R.id.button_stop).setVisibility(INVISIBLE);

        Switch hold_command = findViewById(R.id.switch_hold_command_mm);
        hold_command.setOnCheckedChangeListener(this);

        Arrays.fill(message, (byte) 0);

    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshActivity();
    }


    protected void finishActivity() {
        // Останавливаем сервис
        stopService(new Intent(SendDataActivity.this, BluetoothConnectionService.class));
        // Выходим из Activity
        onBackPressed();
    }

    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid() {
        return btAdapter.isEnabled();
    }

    // Метод для вывода всплывающих данных на экран
    public void showToast(String outputInfoString) {
        Toast outputInfoToast = Toast.makeText(this, outputInfoString, Toast.LENGTH_LONG);
        outputInfoToast.show();
    }

    //Обновляем внешний вид приложения, если Bluetooth выключен завершаем Activity и Сервис
    public void refreshActivity() {
        if (!btAdapter.isEnabled()) {
            showToast(getResources().getString(R.string.suggestionEnableBluetooth));
            finishActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothConnectionService.class));
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        message[4] = (byte) 0x0a;
        message[5] = (byte) 0xa1;
        message[6] = (byte) 0x7f;
        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
        dataThreadForArduino.Send_Data(message);

        if(arduino_timer != null)
        {
            arduino_timer.cancel();
            arduino_timer = null;
        }

        try
        {
            dataThreadForArduino.Disconnect();                 // отсоединяемся от bluetooth
            //arduino.Shut_down_bt();               // и выключаем  bluetooth на cubietruck
        }
        catch (Exception e)
        {}
    }

    @Override
    public void onClick(View v)
    {
        refreshActivity();
        message[0] = (byte) 0x30;
        message[1] = (byte) 0x65; // класс и тип устройства отправки
        message[2] = (byte) 0x7e; // класс и тип устройства приема
        switch (v.getId())
        {
            case R.id.button_stop:
                //Toast.makeText(getApplicationContext(), "Стоп всех комманд", Toast.LENGTH_SHORT).show();
                message[4] = (prevCommand == (byte) 0x7f)? getDevicesID.get("redo_command"): getDevicesID.get("new_command");
                message[5] = (byte) 0xa1;
                message[6] = prevCommand = (byte) 0x7f;
                dataThreadForArduino.Send_Data(message);
                outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                break;
        }
    }

    View.OnTouchListener touchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            refreshActivity();
            message[0] = (byte) 0x7e;
            message[1] = (byte) 0x65; // класс и тип устройства отправки
            message[2] = (byte) 0x7e; // класс и тип устройства приема
            message[5] = (byte) 0xa1;
            if(event.getAction() == MotionEvent.ACTION_DOWN)                        // если нажали на кнопку и не важно есть удержание команд или нет
            {
                switch (v.getId())
                {
                    case R.id.button_up:
                        Log.d("Вперед поехали", "********************************************");
                        message[4] = (prevCommand == (byte) 0x01)? (byte) 0x15: (byte) 0x0A;
                        message[6] = prevCommand = (byte) 0x01;
                        dataThreadForArduino.Send_Data(message);
                        outputText.append("\n"+ "Движение вперед");
                        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                        break;
                    case R.id.button_down:
                        Log.d("Назад поехали", "********************************************");
                        //Toast.makeText(getApplicationContext(), "Назад поехали", Toast.LENGTH_SHORT).show();
                        message[4] = (prevCommand == (byte) 0x02)? (byte) 0x15: (byte) 0x0A;
                        message[6] = prevCommand = (byte) 0x02;
                        outputText.append("\n"+ "Движение назад");
                        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                        dataThreadForArduino.Send_Data(message);

                        break;
                    case R.id.button_left:
                        //Toast.makeText(getApplicationContext(), "Влево поехали", Toast.LENGTH_SHORT).show();
                        Log.d("Влево поехали", "********************************************");
                        message[4] = (prevCommand == (byte) 0x03)? (byte) 0x15: (byte) 0x0A;
                        message[6] = prevCommand = (byte) 0x03;
                        outputText.append("\n"+ "Движение влево");
                        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                        dataThreadForArduino.Send_Data(message);
                        break;
                    case R.id.button_right:
                        //Toast.makeText(getApplicationContext(), "Вправо поехали", Toast.LENGTH_SHORT).show();
                        Log.d("Вправо поехали", "********************************************");
                        message[4] = (prevCommand == (byte) 0x0c)? (byte) 0x15: (byte) 0x0A;
                        message[6] = prevCommand = (byte) 0x0c;
                        outputText.append("\n"+ "Движение вправо");
                        outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                        dataThreadForArduino.Send_Data(message);
                        break;
                }
            }
            else if(event.getAction() == MotionEvent.ACTION_UP)             // если отпустили кнопку
            {
                if(!is_hold_command)    // и нет удержания команд то все кнопки отправляют команду стоп
                {
                    outputText.append("\n"+ "Кнопка отпущена, отправляем Stop");
                    switch (v.getId())
                    {
                        case R.id.button_up:
                            message[4] = (prevCommand == (byte) 0x41)? (byte) 0x15: (byte) 0x0A;
                            message[6] = prevCommand = (byte) 0x41;
                            outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                            dataThreadForArduino.Send_Data(message);
                            break;
                        case R.id.button_down:
                            message[4] = (prevCommand == (byte) 0x42)? (byte) 0x15: (byte) 0x0A;
                            message[6] = prevCommand = (byte) 0x42;
                            outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                            dataThreadForArduino.Send_Data(message);
                            break;
                        case R.id.button_left:
                            message[4] = (prevCommand == (byte) 0x43)? (byte) 0x15: (byte) 0x0A;
                            message[6] = prevCommand = (byte) 0x43;
                            outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                            dataThreadForArduino.Send_Data(message);
                            break;
                        case R.id.button_right:
                            message[4] = (prevCommand == (byte) 0x4c)? (byte) 0x15: (byte) 0x0A;
                            message[6] = prevCommand = (byte) 0x4c;
                            outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
                            dataThreadForArduino.Send_Data(message);
                            break;
                    }
                }
            }
            return false;
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        refreshActivity();
        switch (buttonView.getId())
        {
            case R.id.switch_hold_command_mm:
                is_hold_command = isChecked;
                if(is_hold_command)
                {
                    Toast.makeText(this, "Удерживание комманды включено: ", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.button_stop).setVisibility(VISIBLE);
                }
                else
                {
                    Toast.makeText(this, "Удерживание комманды отключено: ", Toast.LENGTH_SHORT).show();
                    findViewById(R.id.button_stop).setVisibility(INVISIBLE);
                }
                break;
        }
    }

    private void Data_request()
    {
        refreshActivity();
        if (dataThreadForArduino.isReady_to_request()) // если готовы принимать данные, таймер действует
        {
            sens_data = dataThreadForArduino.getMy_data();
            outputText.append("\n"+ "Данные с устройства");
            outputText.append("\n"+ pre_str_sens_data[0] + sens_data[0] + "\n" +
                    pre_str_sens_data[1] + sens_data[1] + "\n" +
                    pre_str_sens_data[2] + sens_data[2] + "\n" +
                    pre_str_sens_data[3] + sens_data[3] + "\n" +
                    pre_str_sens_data[4] + sens_data[4]);

            dataThreadForArduino.Send_Data(message);
            outputText.append("\n"+ "Отправляем данные:" + "\n"+ Arrays.toString(message));
            dataThreadForArduino.setReady_to_request(false); // как только отправили запрос, то так сказать приостанавливаем таймер
        } else // если не готовы получать данные то просто ничего не делаем
        {
            Log.d("qwerty", "******************************************** ошибка");
        }
    }
}
