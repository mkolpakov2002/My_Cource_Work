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

    public ExtendedFloatingActionButton fab_en_bt;
    public ExtendedFloatingActionButton fab_add_device;
    public ListView pairedList;
    //инициализация swipe refresh
    public SwipeRefreshLayout mSwipeRefreshLayout;
    public BluetoothAdapter btAdapter;
    public boolean btIsEnabledFlag = false;
    public boolean stateOfFabEnBt = false;
    public boolean stateOfFabAddDevice = false;
    private static final String TAG = "MainActivity";
    public TextView pairedDevicesTitle;
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

        this.fab_add_device = findViewById(R.id.floating_action_button_Add_Device);
        this.fab_en_bt = findViewById(R.id.floating_action_button_En_Bt);
        this.pairedList = findViewById(R.id.pairedList);
        pairedDevicesTitle = findViewById(R.id.paired_devices_title);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        pairedList.setOnItemClickListener((parent, view, position, id) -> {

            Object listItem = pairedList.getItemAtPosition(position);
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
        });

        fab_add_device.setOnClickListener(view -> {
            Intent intent_add_device = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent_add_device);
        });
        fab_en_bt.setOnClickListener(view -> {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        });

        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(this::refreshApplication);
        fab_en_bt.hide();
        fab_add_device.hide();


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
        enableBt();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableBt();

    }

    //возвращает true, если bluetooth включён
    public boolean btIsEnabledFlagVoid(){
        return btAdapter.isEnabled();
    }

    public void refreshApplication(){
        Intent intent_reload_activity = getIntent();
        overridePendingTransition(0, 0);
        intent_reload_activity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent_reload_activity);
        mSwipeRefreshLayout.setRefreshing(false);
    }


    public void searchForDevice(){
        ListView pairedList = findViewById(R.id.pairedList);
        Set<BluetoothDevice> pairedDevices= btAdapter.getBondedDevices();
// создаем адаптер
        // Если список спаренных устройств не пуст
        if(pairedDevices.size()>0){
            pairedDevicesTitle.setText(R.string.paired_devices);
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
            pairedDevicesTitle.setText(R.string.no_devices_added);
            pairedList.setAdapter(null);
        }
    }

    //включаем bluetooth
    public void enableBt() {

        if (btAdapter != null) {
            btIsEnabledFlag = btIsEnabledFlagVoid();
            if (btIsEnabledFlag) {
                if(!stateOfFabAddDevice){
                    fab_add_device.show();
                    stateOfFabAddDevice = true;
                }
                if (stateOfFabEnBt) {
                    fab_en_bt.hide();
                    stateOfFabEnBt = false;
                }
                searchForDevice();
            } else {
                if(stateOfFabAddDevice){
                    fab_add_device.hide();
                    stateOfFabAddDevice = false;
                }
                // Bluetooth выключен. Предложим пользователю включить его.
                if (!stateOfFabEnBt) {
                    fab_en_bt.show();
                    stateOfFabEnBt = true;
                }
            }
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