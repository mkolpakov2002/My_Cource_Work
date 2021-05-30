package com.miem.mmkolpakov.coursework;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendDataThread extends Thread {

    //таг для логов
    private final String TAG = "SendDataThread";
    //
    Context c;
    BluetoothSocket clientSocket;
    OutputStream mmOutStream;
    InputStream mmInStream;
    boolean flag = true;
    StringBuilder str = new StringBuilder();

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
        while(flag && !((SendDataActivity) c).getIsActivityNeedsStopping("1")){
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            // Read from the InputStream
            try {
                bytes = mmInStream.read(buffer);
                flag = true;
            } catch (IOException e) {
                Log.e(TAG, "Ошибка чтения входящих данных в потоке " + e.getMessage());
                flag = false;
            }
            if(flag){
                //успешно считываем данные
                StringBuilder incomingDataBuffer = new StringBuilder();
                String incomingMessage = new String(buffer, 0, bytes);
                //incomingMessage - текущие входящие данные (в текущем заходе цикла while); содержат символы \r, которые не нужны
                incomingMessage = incomingMessage.replaceAll("\r", "");
                //str - переменная для формирования итоговой строки
                str.append(incomingMessage);
                int j = 0;
                boolean isComplete = false;
                while(!isComplete){
                    //обрабатываем str, выделяем строки с символом \n в конце
                    if(str.charAt(j)=='\n' && j+1<=str.length()-1) {
                        //substring копирует до второго параметра НЕ включительно, но включая с первого
                        incomingDataBuffer.append(str.substring(0, j+1));
                        incomingData(incomingDataBuffer.toString());
                        //incomingDataBuffer.toString() - подходящая строка
                        //записываем в str остаток старой строки (str без incomingDataBuffer.toString())
                        incomingDataBuffer.setLength(0);
                        String bufferStr = str.substring(j + 1);
                        str.setLength(0);
                        str.append(bufferStr);
                        j = -1;
                    } else if(str.charAt(j)=='\n') {
                        //нету элемента j+1, рассматриваемый символ \n последний в str
                        //просто копируем (без остатка, его нет)
                        incomingDataBuffer.append(str);
                        incomingData(incomingDataBuffer.toString());
                        j = -1;
                        str.setLength(0);
                    }
                    if(str.indexOf("\n") == -1){
                        //более символов \n не найдено, завершаем обработку строки
                        isComplete = true;
                    }
                    j++;
                }
            } else if(SendDataActivity.active && ((SendDataActivity) c).isNeedToRestartConnection("2")){
                //чтение входящей информации неуспешно при открытом приложении
                ((SendDataActivity) c).runOnUiThread(new Runnable() {
                    public void run() {

                        ((SendDataActivity) c).connectionFailed();
                    }
                });
            }
        }
    }

    synchronized void incomingData(String incomingData){
        if(!((SendDataActivity) c).getIsActivityNeedsStopping("1")){
            Log.d(TAG, "Входящие данные: " + incomingData);
            ((SendDataActivity) c).runOnUiThread(new Runnable() {
                public void run() {
                    ((SendDataActivity) c).printDataToTextView(incomingData.replaceAll("\n",""));
                }
            });
            SystemClock.sleep(100);
        }
    }

    public void sendData(byte[] message) {
        if(!((SendDataActivity) c).getIsActivityNeedsStopping("1")){
            Log.d(TAG, "Отправка данных в потоке");
            StringBuilder logMessage = new StringBuilder(((SendDataActivity) c).getResources().getString(R.string.sending_data_bytes));
            for (int i=0; i < 32; i++)
                logMessage.append(message[i]).append(" ");
            Log.d(TAG, logMessage + "]");
            try
            {
                mmOutStream.write(message);
            } catch (IOException e) {
                Log.e(TAG, "Ошибка отправки данных в потоке " + e.getMessage());
                flag = false;
                if(((SendDataActivity) c).isNeedToRestartConnection("2")){
                    //отправка сообщения об ошибке
                    ((SendDataActivity) c).runOnUiThread(new Runnable() {
                        public void run() {
                            ((SendDataActivity) c).printDataToTextView(((SendDataActivity) c).getResources().getString(R.string.sending_data_failed));
                            ((SendDataActivity) c).connectionFailed();
                        }
                    });
                }
            }
        }
    }

    public void Disconnect() {
        Log.d(TAG, "Пытаюсь отключиться от устройства в потоке");
        try {
            clientSocket.close();
        } catch (IOException e2) {
            Log.e(TAG, "Ошибка при попытке отключения от устройства " + e2.getMessage());
        }
    }
}
