package com.mmkolpakov.mycourcework;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.provider.Settings;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public ExtendedFloatingActionButton fabToEnBt;
    public ExtendedFloatingActionButton fabToAddDevice;
    public ListView pairedList;
    //инициализация swipe refresh
    public SwipeRefreshLayout swipeToRefreshLayout;
    public BluetoothAdapter btAdapter;
    public boolean stateOfBt = false;
    public boolean stateOfFabToEnBt = false;
    public boolean stateOfFabToAddDevice = false;
    private static final String TAG = "MainActivity";
    public TextView pairedDevicesTitleTextView;
    public TextView otherDevicesTextView;
    // SPP UUID сервиса
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothSocket clientSocket = null;
    OutputStream outStream = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.fabToAddDevice = findViewById(R.id.floating_action_button_Add_Device);
        this.fabToEnBt = findViewById(R.id.floating_action_button_En_Bt);
        this.pairedList = findViewById(R.id.paired_list);
        pairedDevicesTitleTextView = findViewById(R.id.paired_devices_title);
        otherDevicesTextView = findViewById(R.id.other_discoverable_devices_title);
        swipeToRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        fabToEnBt.hide();
        fabToAddDevice.hide();

        pairedList.setOnItemClickListener((parent, view, position, id) -> {
            int positionOfSelectedDevice = position;
            startSendingData(positionOfSelectedDevice);
        });
        fabToAddDevice.setOnClickListener(view -> {
            Intent intent_add_device = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent_add_device);
        });
        fabToEnBt.setOnClickListener(view -> {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        });

        swipeToRefreshLayout.setOnRefreshListener(this::refreshApplication);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent1 = new Intent(this, AboutActivity.class);
            startActivity(intent1);
            return true;
        }
        if (id == R.id.refresh_application) {
            refreshApplication();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForBtAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForBtAdapter();
    }

    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid(){
        return btAdapter.isEnabled();
    }

    public void startSendingData(int positionOfSelectedDevice){
        Object listItem = pairedList.getItemAtPosition(positionOfSelectedDevice);
        String selectedDevice = listItem.toString();
        //Получили информацию с выбранной позиции List View в String виде
        showToast(listItem.toString());
        int i = selectedDevice.indexOf(':');
        i = i - 2;
        //В текущем пункте List View находим первый символ ":", всё после него, а также два символа до него - адрес выбранного устройства
        selectedDevice = selectedDevice.substring(i);
        //Сокет, с помощью которого мы будем отправлять данные на выбранное устройство
        //соединение с устройством с выбранным адресом Bluetooth модуля.
        BluetoothDevice device = btAdapter.getRemoteDevice(selectedDevice);

        try {
            clientSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.d("BLUETOOTH", e.getMessage());
        }
        btAdapter.cancelDiscovery();
        try {

            clientSocket.connect();
            Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
        } catch (IOException e) {
            try {
                clientSocket.close();
            } catch (IOException e2) {
                Log.d("BLUETOOTH", e2.getMessage());
            }
        }
        try {
            outStream = clientSocket.getOutputStream();
        } catch (IOException e) {
            Log.d("BLUETOOTH", e.getMessage());
        }
        try{
            int sending_data;
            //изменяем данные для посылки
            sending_data = 60;
            //byte[] msgBuffer = sending_data.getBytes();
            //Пишем данные в выходной поток
            if (outStream != null) {
                outStream.write(sending_data);
                //Выводим сообщение об успешном подключении
                Toast.makeText(getApplicationContext(), "CONNECTED", Toast.LENGTH_LONG).show();
            }
        } catch (IOException | SecurityException | IllegalArgumentException e) {
            Log.d("BLUETOOTH", e.getMessage());
        }
    }

    public void refreshApplication(){
        stateOfBt = btIsEnabledFlagVoid();
        if (stateOfBt) {
            // Bluetooth включён. Предложим пользователю добавить устройства и начать передачу данных.
            if (stateOfFabToEnBt) {
                fabToEnBt.hide();
                stateOfFabToEnBt = false;
            }
            if(!stateOfFabToAddDevice){
                fabToAddDevice.show();
                stateOfFabToAddDevice = true;
                pairedDevicesTitleTextView.setVisibility(View.VISIBLE);
                otherDevicesTextView.setVisibility(View.VISIBLE);
                searchForDevice();
            }
        } else {
            if(stateOfFabToAddDevice){
                fabToAddDevice.hide();
                stateOfFabToAddDevice = false;
                pairedDevicesTitleTextView.setVisibility(View.GONE);
                otherDevicesTextView.setVisibility(View.GONE);
                pairedList.setAdapter(null);
            }
            // Bluetooth выключен. Предложим пользователю включить его.
            if (!stateOfFabToEnBt) {
                fabToEnBt.show();
                stateOfFabToEnBt = true;
            }
        }
        swipeToRefreshLayout.setRefreshing(false);
    }


    public void searchForDevice(){
        ListView pairedList = findViewById(R.id.paired_list);
        Set<BluetoothDevice> pairedDevices= btAdapter.getBondedDevices();
        // создаем адаптер
        // Если список спаренных устройств не пуст
        if(pairedDevices.size()>0){
            pairedDevicesTitleTextView.setText(R.string.paired_devices);
            String deviceHardwareAddress;
            ArrayList<String> devicesList = new ArrayList<String>();
            // проходимся в цикле по этому списку
            for(BluetoothDevice device: pairedDevices){
                deviceHardwareAddress = device.getName() + "\n" + device.getAddress(); // Name + MAC address
                devicesList.addAll( Arrays.asList(deviceHardwareAddress) );
                ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devicesList);
                pairedList.setAdapter( listAdapter );



            }
        } else {
            //no_devices_added
            pairedDevicesTitleTextView.setText(R.string.no_devices_added);
            pairedList.setAdapter(null);
        }
    }

    public void checkForBtAdapter() {
        if (btAdapter != null) {
            refreshApplication();
        } else {
            System.out.println("There is no bluetooth adapter on device!");
            //suggestionNoBtAdapter
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
            dialog.setTitle(getString(R.string.error));
            dialog.setMessage(getString(R.string.suggestionNoBtAdapter));
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    (dialog1, which) -> {
                        // Closes the dialog and terminates the activity.
                        dialog1.dismiss();
                        MainActivity.this.finish();
                    });
        }
    }

    public void showToast(String outputInfoString){
        Toast outputInfoToast = Toast.makeText(this, outputInfoString,Toast.LENGTH_LONG);
        outputInfoToast.show();
    }
}