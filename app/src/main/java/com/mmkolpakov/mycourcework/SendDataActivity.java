package com.mmkolpakov.mycourcework;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class SendDataActivity extends AppCompatActivity {
    String sending_data;
    byte[] msgBuffer;
    public Button sendZero;
    public Button sendOne;
    String selectedDevice;
    BluetoothSocket clientSocket = null;
    private static final String TAG = "SendDataActivity";
    // SPP UUID сервиса
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothAdapter btAdapter;

    OutputStream outStream = null;
    public boolean stateOfMessage = false;
    public boolean stateOfConnection = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data);
        Bundle arguments = getIntent().getExtras();
        selectedDevice = arguments.get("idOfDevice").toString();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (!btIsEnabledFlagVoid()) {
            showToast(getResources().getString(R.string.suggestionEnableBluetooth));
            finishActivity();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (!btIsEnabledFlagVoid()) {
            showToast(getResources().getString(R.string.suggestionEnableBluetooth));
            finishActivity();
        }
    }

        public void sendZeroButtonVoid(View view)
    {
        if(btIsEnabledFlagVoid()) {
            sending_data = "0";
            sendData(sending_data);
        } else refreshActivity();
    }
    public void sendOneButtonVoid(View view)
    {
        if(btIsEnabledFlagVoid()) {
            sending_data = "1";
            sendData(sending_data);
        } else refreshActivity();
    }


    void sendData(String outputData) {
        msgBuffer = outputData.getBytes();
        try {
            BluetoothConnectionService.clientSocket.getOutputStream().write(msgBuffer);
            stateOfMessage = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("BLUETOOTH", e.getMessage());
            stateOfMessage = false;
        }
        if (!stateOfMessage) {
            if (!btIsEnabledFlagVoid()) {
                showToast(getResources().getString(R.string.suggestionEnableBluetooth));
                finishActivity();
            } else {
                showToast(getResources().getString(R.string.error));
            }
        }

    }
    protected void finishActivity() {

        stopService(new Intent(SendDataActivity.this, BluetoothConnectionService.class));
        onBackPressed();
    }
    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid(){
        return btAdapter.isEnabled();
    }
    public void showToast(String outputInfoString) {
        Toast outputInfoToast = Toast.makeText(this, outputInfoString, Toast.LENGTH_LONG);
        outputInfoToast.show();
    }
    public void refreshActivity(){
        if(!btIsEnabledFlagVoid()){
            showToast(getResources().getString(R.string.suggestionEnableBluetooth));
            finishActivity();
        }
    }
}
