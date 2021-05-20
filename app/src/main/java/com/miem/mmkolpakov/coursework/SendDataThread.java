package com.miem.mmkolpakov.coursework;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.UUID;

public class SendDataThread extends Thread {

    //таг для логов
    private final String TAG = "SendDataThread";
    private Handler mHandler;
    String MAC;
    Context c;
    BluetoothSocket clientSocket;
    OutputStream mmOutStream;
    InputStream mmInStream;
    ArrayList<String> stringIncomingMessage = new ArrayList<>();
    ArrayList<Integer> intIncomingMessage = new ArrayList<>();
    boolean flag = true;
    StringBuilder str = new StringBuilder();
    Integer u = null;

    public void setSelectedDevice(String selectedDevice) {
        this.MAC = selectedDevice;
    }

    public void setSocket(BluetoothSocket clientSocket){
        this.clientSocket = clientSocket;
        OutputStream tmpOut = null;
        InputStream tmpIn = null;
        try {
            tmpOut = clientSocket.getOutputStream();
            tmpIn = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        mmOutStream = tmpOut;
        mmInStream = tmpIn;
    }

    public void setHandler(Handler handler){
        this.mHandler = handler;
    }

    public SendDataThread(@NonNull Context context){
        if (context instanceof Activity){
            c = context;
        }
        Log.d(TAG, "Поток запущен");
    }

    @Override
    public void run() {


    }

    void printData(){
        if(flag){

        }
    }
    public void sendData(byte[] message)
    {
        Log.d(TAG, "Отправка данных в потоке");
        StringBuilder logMessage = new StringBuilder("***Отправляем данные: ");
        for (int i=0; i < 32; i++)
            logMessage.append(message[i]).append(" ");
        Log.d(TAG, logMessage + "***");
        try
        {
            mmOutStream.write(message);
        } catch (IOException e)
        {
            Log.d(TAG, "Ошибка отправки данных в потоке");
        }
    }

    public void Disconnect() // при ручном управлении передачей пакетов
    {
        Log.d(TAG, "...In onPause()...");
        try
        {
            clientSocket.close();

        } catch (IOException e2)
        {
            //MyError("Fatal Error", "В onPause() Не могу закрыть сокет" + e2.getMessage() + ".", "Не могу закрыть сокет.");
        }
    }

}
