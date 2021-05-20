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

    public SendDataThread(@NonNull Context context){
        if (context instanceof Activity){
            c = context;
        }
        Log.d(TAG, "Поток запущен");
    }

    @Override
    public void run() {
        while(flag){
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            // Read from the InputStream
            try {
                StringBuilder incomingDataBuffer = new StringBuilder();

                bytes = mmInStream.read(buffer);

                String incomingMessage = new String(buffer, 0, bytes);
                incomingMessage = incomingMessage.replaceAll("\r", "");
                str.append(incomingMessage);
                int j = 0;
                boolean isComplete = false;
                while(!isComplete){
                    if(str.charAt(j)=='\n' && j+1<=str.length()-1) {
                        incomingDataBuffer.append(str.substring(0, j+1));
                        incomingData(incomingDataBuffer.toString());

                        incomingDataBuffer.setLength(0);
                        String bufferStr = str.substring(j + 1);
                        str.setLength(0);
                        str.append(bufferStr);
                        j = -1;

                    } else if(str.charAt(j)=='\n') {
                        incomingDataBuffer.append(str);
                        incomingData(incomingDataBuffer.toString());
                        j = -1;
                        str.setLength(0);
                    }
                    if(str.indexOf("\n") == -1){
                        isComplete = true;
                    }
                    j++;
                }

            } catch (IOException e) {
                Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                flag = false;
            }
        }
    }

    synchronized void incomingData(String incomingData){
        Log.d(TAG, "InputStream: " + incomingData);
        ((SendDataActivity) c).outputText.append(incomingData);
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
