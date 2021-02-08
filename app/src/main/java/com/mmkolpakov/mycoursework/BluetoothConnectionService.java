package com.mmkolpakov.mycoursework;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class BluetoothConnectionService extends Service {
    public static BluetoothDevice device;
    String selectedDevice;
    private static final String TAG = "SendDataActivity";
    // SPP UUID сервиса
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothAdapter btAdapter;
    public boolean stateOfConnection = false;
    static BluetoothSocket clientSocket;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle arguments = intent.getExtras();
        selectedDevice = arguments.get("idOfDevice").toString();
        BluetoothConnectionServiceVoid();
        return Service.START_STICKY;
    }


    public void BluetoothConnectionServiceVoid() {
        new Thread(() -> {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (btIsEnabledFlagVoid()) {
                device = btAdapter.getRemoteDevice(selectedDevice);
                // Попытка подключиться к устройству
                // В новом потоке, чтобы Main Activity не зависал
                try {
                    clientSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocketToServiceRecord", UUID.class).invoke(device, MY_UUID);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                    stateOfConnection = false;
                }
                try {
                    clientSocket.connect();
                    // Отключаем поиск устройств для сохранения заряда батареи
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
                    stateOfConnection = true;
                } catch (IOException e) {
                    stateOfConnection = false;
                    try {
                        // В случае ошибки пытаемся закрыть соединение
                        clientSocket.close();
                    } catch (IOException closeException) {
                        Log.d("BLUETOOTH", e.getMessage());
                    }
                    Log.d("BLUETOOTH", e.getMessage());
                }

                if (stateOfConnection) {
                    // Передаём данные о устройстве в Main Activity
                    MainActivity.clientSocket = clientSocket;
                    MainActivity.device = device;
                    try {
                        // Решение ошибки, зависящей от версии Android - даём время на установку полного подключения
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    resultOfConnection();
                }

            } else {
                stateOfConnection = false;
            }
            if (!stateOfConnection) {
                resultOfConnection();
                stopSelf();
                onDestroy();
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid() {
        return btAdapter.isEnabled();
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            // В случае остановки сервиса завершаем соединение
            clientSocket.close();
        } catch (IOException e) {
            Log.d("BLUETOOTH", e.getMessage());
        }
    }

    // Передаём данные о статусе соединения в Main Activity
    public void resultOfConnection() {
        Intent resultOfConnectionIntent;
        if (!stateOfConnection) {
            resultOfConnectionIntent = new Intent("not_success");
        } else {
            resultOfConnectionIntent = new Intent("success");

        }
        sendBroadcast(resultOfConnectionIntent);
    }

}