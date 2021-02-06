package com.mmkolpakov.mycoursework;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class SendDataActivity extends AppCompatActivity {
    public static boolean stateOfMessage;
    String sending_data;
    String selectedDevice;
    ProgressBar progressBar;

    private static final String TAG = "SendDataActivity";
    public BluetoothAdapter btAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);


        // Получаем данные об устройстве из Main Activity
        Bundle arguments = getIntent().getExtras();
        selectedDevice = arguments.get("idOfDevice").toString();

        progressBar = findViewById(R.id.progressBarCheckSocketState);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        MainActivity.isItemSelected = false;
        MainActivity.stateOfToastAboutConnection = false;
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


    // Вызываем метод для отправки сообщения
    //TODO
    public void sendZeroButtonVoid(View view) {
        if (btIsEnabledFlagVoid()) {
            sending_data = "0";
            sendData(sending_data);
        } else refreshActivity();
    }

    // Вызываем метод для отправки сообщения
    public void sendOneButtonVoid(View view) {
        if (btIsEnabledFlagVoid()) {
            sending_data = "1";
            sendData(sending_data);
        } else refreshActivity();
    }


    // Метод для отправки сообщения
    void sendData(String outputData) {
        // Преобразуем отправляемые данные в байты
        byte[] msgBuffer = outputData.getBytes();
        try {
            Log.d(TAG, "...Посылаем данные: " + outputData + "...");
            // Пишем данные ввиде байтов в исходящий поток
            BluetoothConnectionService.clientSocket.getOutputStream().write(msgBuffer);
            BluetoothConnectionService.clientSocket.getOutputStream().flush();
            stateOfMessage = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("BLUETOOTH", e.getMessage());
            stateOfMessage = false;
        }
        if (!stateOfMessage) {
            if (!btIsEnabledFlagVoid()) {
                // Обновляем экран
                refreshActivity();
            } else {
                // Непредвиденная ошибка
                showToast(getResources().getString(R.string.error));
            }
        }
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
        if (!btIsEnabledFlagVoid()) {
            showToast(getResources().getString(R.string.suggestionEnableBluetooth));
            finishActivity();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, BluetoothConnectionService.class));
    }
}
