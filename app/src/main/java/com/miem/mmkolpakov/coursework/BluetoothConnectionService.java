package com.miem.mmkolpakov.coursework;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class BluetoothConnectionService extends Service {
    //устройство к которому будет происходить соедиение
    BluetoothDevice device;
    //таг для логов
    private final String TAG = "ConnectionService";
    // SPP UUID сервиса согласно документации Android:
    /*
    Hint: If you are connecting to a Bluetooth serial board,
     then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB
     https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html
     */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter btAdapter;
    //boolean переменная для хранения результата соединения
    boolean stateOfConnection = false;
    //сокет для передачи данных
    BluetoothSocket clientSocket;
    //данные из MainActivity
    Bundle arguments;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //получение данных из MainActivity
        arguments = intent.getExtras();
        //вызов метода подключения
        BluetoothConnectionServiceVoid();
        return Service.START_NOT_STICKY;
    }


    public void BluetoothConnectionServiceVoid() {
        //запуск нового потока для исключения зависания приложения
         new Thread(() -> {
            //получаем локальный Bluetooth адаптер устройства
            btAdapter = BluetoothAdapter.getDefaultAdapter();
            //ещё одна проверка на состояние Bluetooth
            if (btIsEnabledFlagVoid()) {
                //устройство с выбранным MAC как объект
                device = btAdapter.getRemoteDevice(arguments.get("idOfDevice").toString());
                // Попытка подключиться к устройству
                try {
                    clientSocket = (BluetoothSocket) device.getClass()
                            .getMethod("createRfcommSocketToServiceRecord", UUID.class)
                            .invoke(device, MY_UUID);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    Log.d(TAG, e.getMessage());
                    //подключение неуспешно
                    stateOfConnection = false;
                }
                try {
                    if (clientSocket != null) {
                        clientSocket.connect();
                        // Отключаем поиск устройств для сохранения заряда батареи
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                        Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
                        //соединение успешно
                        stateOfConnection = true;
                        SystemClock.sleep(2000);
                    } else {
                        //подключение неуспешно
                        stateOfConnection = false;
                    }
                } catch (IOException e) {
                    //подключение неуспешно
                    stateOfConnection = false;
                    try {
                        // В случае ошибки пытаемся закрыть соединение
                        clientSocket.close();
                    } catch (IOException closeException) {
                        //запись логов ошибки
                        Log.d(TAG, e.getMessage());
                    }
                    //запись логов ошибки
                    Log.d(TAG, e.getMessage());
                }
            } else {
                //подключение неуспешно, т.к. Bluetooth выключен
                stateOfConnection = false;
            }
            //передаём результат соединения в соот. функции
            resultOfConnection();
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
        //остановка сервиса, вывод логов об этом
        Log.d(TAG, "...Сервис остановлен...");
    }

    // Передаём данные о статусе соединения в Main Activity
    public void resultOfConnection() {
        Intent resultOfConnectionIntent;
        //коды запуска Service - 1 из MainActivity и 2 из SendDataActivity при переподключении
        if (!stateOfConnection) {
            //неуспешно
            if(arguments.getInt("startCode") == 1){
                resultOfConnectionIntent = new Intent("not_success_code_1");
            } else {
                resultOfConnectionIntent = new Intent("not_success_code_2");
            }
        } else {
            //успешно
            if(arguments.getInt("startCode") == 1){
                resultOfConnectionIntent = new Intent("success_code_1");
            } else {
                resultOfConnectionIntent = new Intent("success_code_2");
            }
            // Сохраняем данные о устройстве через класс посредник
            DeviceHandler.setSocket(clientSocket);
            DeviceHandler.setDeviceId(arguments.get("idOfDevice").toString());
            DeviceHandler.setDeviceName(arguments.get("nameOfDevice").toString());
            DeviceHandler.setDeviceClass(arguments.get("classOfDevice").toString());
        }
        //отправка сообщения о результате
        sendBroadcast(resultOfConnectionIntent);
        //остановка сервиса
        stopSelf();
    }

}