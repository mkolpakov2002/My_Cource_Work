package com.mmkolpakov.mycourcework;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class BluetoothConnectionService extends Service {
    String sending_data;
    byte[] msgBuffer;
    String selectedDevice;
    private static final String TAG = "SendDataActivity";
    // SPP UUID сервиса
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothAdapter btAdapter;
    OutputStream outStream = null;
    public boolean stateOfConnection = false;
    static BluetoothSocket clientSocket;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::BluetoothConnectionServiceVoid).start();
        Bundle arguments = intent.getExtras();
        selectedDevice = arguments.get("idOfDevice").toString();
        return Service.START_STICKY;
    }
    public void BluetoothConnectionServiceVoid() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btIsEnabledFlagVoid()) {


            BluetoothDevice device = btAdapter.getRemoteDevice(selectedDevice);
            // Attempt to connect to the device
            // Start the thread to connect with the given device

            try {
                clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.d("BLUETOOTH", e.getMessage());
            }
            btAdapter.cancelDiscovery();
            try {
                clientSocket.connect();
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
                stateOfConnection = true;
                Intent intent1 = new Intent("success");
                sendBroadcast(intent1);
            } catch (IOException e) {
                try {
                    clientSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocketToServiceRecord", UUID.class).invoke(device, MY_UUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    clientSocket.connect();
                    stateOfConnection = true;
                    Intent intent1 = new Intent("success");
                    sendBroadcast(intent1);
                } catch (IOException | NoSuchMethodException e2) {
                    Log.d("BLUETOOTH", e2.getMessage());
                    stateOfConnection = false;
                } catch (IllegalAccessException | InvocationTargetException illegalAccessException) {
                    illegalAccessException.printStackTrace();
                    stateOfConnection = false;
                }
            }
            if (!stateOfConnection) {
                Intent intent1 = new Intent("not_success");
                sendBroadcast(intent1);
            }

        } else {
            Intent intent1 = new Intent("not_success");
            sendBroadcast(intent1);
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid(){
        return btAdapter.isEnabled();
    }

    public void onDestroy(){
        super.onDestroy();
        try{
            clientSocket.close();
        } catch(IOException e){
            Log.d("BLUETOOTH", e.getMessage());
        }
    }
}