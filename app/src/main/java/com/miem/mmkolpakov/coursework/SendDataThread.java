package com.miem.mmkolpakov.coursework;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.UUID;

public class SendDataThread extends Thread {

    public void setSelectedDevice(String selectedDevice) {
        this.MAC = selectedDevice;
    }
    public void setSocket(BluetoothSocket clientSocket){
        this.clientSocket = clientSocket;
    }
    public SendDataThread(@NonNull Context context){
        if (context instanceof Activity){
            c = context;
        }
    }
    String MAC;
    Context c;
    BluetoothSocket clientSocket;
    private byte[] inputPacket;
    private final String TAG = "Thread";
    OutputStream OutStrem;
    InputStream InStrem;
    @Override
    public void run() {
        Log.d("thread is running", "********************************************");
        OutputStream tmpOut = null;
        InputStream tmpIn = null;
        try {
            tmpOut = clientSocket.getOutputStream();
            tmpIn = clientSocket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("BLUETOOTH", e.getMessage());
        }

        OutStrem = tmpOut;
        InStrem = tmpIn;
        inputPacket = new byte[12];
        byte[] buffer = new byte[12]; // 12 должно быть: 2 - префикс 7 - данные 3 - контр сумма
        int bufNum;
        int pacNum = 0;

        while (true) // ну а дальше тоже 12
        {
            try {
                bufNum = InStrem.read(buffer);
                if (pacNum + bufNum < 13) // нормально считываем данные
                {
                    System.arraycopy(buffer, 0, inputPacket, pacNum, bufNum);
                    pacNum = pacNum + bufNum;
                } else // если неправильые данные, то переслать
                {
                    pacNum = 0;
                    byte[] message = new byte[]{0x30, 0x15, 0x0f, 0x37, 0x37, 0x37};
                    sendData(message);
                }
                if (pacNum == 12) // все нормально
                {
                    pacNum = 0;//здесь проверяем пакет и прочее

                    Log.d(TAG, "***Получаем данные: " +
                            buffer[0] + " " +
                            buffer[1] + " " +
                            buffer[2] + " " +
                            buffer[3] + " " +
                            buffer[4] + " " +
                            buffer[5] + " " +
                            buffer[6] + " " +
                            buffer[7] + " " +
                            buffer[8] + " " +
                            buffer[9] + " " +
                            buffer[10] + " " +
                            buffer[11] + " ");
                    ((SendDataActivity) c).outputText.append("\n" +
                            "Получаем данные:" +
                            "\n" +
                            buffer[0] + " " +
                            buffer[1] + " " +
                            buffer[2] + " " +
                            buffer[3] + " " +
                            buffer[4] + " " +
                            buffer[5] + " " +
                            buffer[6] + " " +
                            buffer[7] + " " +
                            buffer[8] + " " +
                            buffer[9] + " " +
                            buffer[10] + " " +
                            buffer[11] + " ");
                }
            } catch (IOException e) {
                break;
            }
        }
        Log.d("Конец true", "********************************************");
    }

    public void sendData(byte[] message)
    {
        Log.d("Send_Data", "********************************************");
        String logMessage = "***Отправляем данные: ";
        for (int i=0; i < 32; i++)
            logMessage += message[i] + " ";
        Log.d(TAG, logMessage + "***");
        try
        {
            OutStrem.write(message);
        } catch (IOException e)
        {
        }
    }

    public void Send_Data(byte[] message) {
        Log.d("Send_Data", "********************************************");
        sendData(message);
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
