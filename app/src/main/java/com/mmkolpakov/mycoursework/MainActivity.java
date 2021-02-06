package com.mmkolpakov.mycoursework;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Set;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    public ExtendedFloatingActionButton fabToEnBt;
    public ExtendedFloatingActionButton fabToAddDevice;
    ListView pairedList;
    //инициализация swipe refresh
    SwipeRefreshLayout swipeToRefreshLayout;
    public BluetoothAdapter btAdapter;
    ProgressBar progressBar;
    public boolean stateOfBt = false;
    boolean stateOfAlertToSendData = false;
    boolean stateOfAlertToAddDevice = false;
    static boolean stateOfToastAboutConnection = false;
    public boolean stateOfFabToEnBt = false;
    public boolean stateOfFabToAddDevice = false;
    public static boolean isItemSelected;
    private static final String TAG = "MainActivity";

    public TextView pairedDevicesTitleTextView;
    AlertDialog.Builder alertDialogBuilder;
    String deviceHardwareAddress;
    ArrayAdapter<String> listAdapter;

    public static BluetoothDevice device;
    public static BluetoothSocket clientSocket;
    String selectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        registerReceiver(mMessageReceiverNotSuccess, new IntentFilter("not_success"));
        registerReceiver(mMessageReceiverSuccess, new IntentFilter("success"));
        this.fabToAddDevice = findViewById(R.id.floating_action_button_Add_Device);
        this.fabToEnBt = findViewById(R.id.floating_action_button_En_Bt);
        pairedList = findViewById(R.id.paired_list);
        pairedDevicesTitleTextView = findViewById(R.id.paired_devices_title);

        swipeToRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeToRefreshLayout.setColorSchemeResources(android.R.color.holo_green_light);
        progressBar = findViewById(R.id.progressBarStartSendingData);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        alertDialogBuilder = new AlertDialog.Builder(this);

        fabToEnBt.hide();
        fabToAddDevice.hide();

        pairedList.setOnItemClickListener((parent, view, position, id) -> checkDeviceAddress(position));

        fabToAddDevice.setOnClickListener(view -> {
            Intent intent_add_device = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent_add_device);
        });
        fabToEnBt.setOnClickListener(view -> {
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        });
        swipeToRefreshLayout.setOnRefreshListener(this);
        isItemSelected = false;
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
            Intent intent1 = new Intent(this, InstructionsActivity.class);
            startActivity(intent1);
            return true;
        }
        if (id == R.id.refresh_application) {
            onRefresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Результат работы Service
    private final BroadcastReceiver mMessageReceiverNotSuccess = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            showToast("Not success");
            progressBar.setVisibility(INVISIBLE);
            isItemSelected = false;
            stateOfToastAboutConnection = false;
        }
    };

    private final BroadcastReceiver mMessageReceiverSuccess = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Устройство подключено, Service выполнился успешно
            showToast("success");
            Intent startSendingData = new Intent(MainActivity.this, SendDataActivity.class);
            startSendingData.putExtra("idOfDevice", selectedDevice);
            startActivity(startSendingData);
            //SendDataActivity.device = device;
            progressBar.setVisibility(INVISIBLE);
        }
    };

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

    //True, если Bluetooth включён
    public boolean btIsEnabledFlagVoid(){
        return btAdapter.isEnabled();
    }

    //Получаем адрес устройства из List View
    public void checkDeviceAddress(int positionOfSelectedDevice) {
        if (!isItemSelected) {
            isItemSelected = true;
            Object listItem = pairedList.getItemAtPosition(positionOfSelectedDevice);
            selectedDevice = listItem.toString();
            //Get information from List View in String
            showToast(selectedDevice);
            int i = selectedDevice.indexOf(':');
            i = i - 2;
            //В текущем пункте List View находим первый символ ":", всё после него, а также два символа до него - адрес выбранного устройства
            selectedDevice = selectedDevice.substring(i);
            progressBar.setVisibility(VISIBLE);
            // запускаем длительную операцию подключения в Service
            Intent startBluetoothConnectionService = new Intent(this, BluetoothConnectionService.class);
            startBluetoothConnectionService.putExtra("idOfDevice", selectedDevice);
            startService(startBluetoothConnectionService);
        } else {
            if (!stateOfToastAboutConnection) {
                showToast(getResources().getString(R.string.deviceIsSelected));
                stateOfToastAboutConnection = true;
            }
        }
    }

    // Добавляем сопряжённые устройства в List View
    public void searchForDevice(){
        ListView pairedList = findViewById(R.id.paired_list);
        // Обновление List View - удаление старых данных
        pairedList.setAdapter(null);
        Set<BluetoothDevice> pairedDevices= btAdapter.getBondedDevices();
        // Если список спаренных устройств не пуст
        if(pairedDevices.size()>0) {
            if (!stateOfAlertToSendData) {
                createOneButtonAlertDialog(getResources().getString(R.string.instruction_alert), getResources().getString(R.string.other_discoverable_devices));
            }
            stateOfAlertToSendData = true;
            // устанавливаем связь между данными
            pairedList.setAdapter(listAdapter);
            ArrayList<String> devicesList = new ArrayList<String>();
            listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, devicesList);
            // проходимся в цикле по этому списку
            for (BluetoothDevice device : pairedDevices) {
                // Обновление List View - добавляем в него сопряжённые устройства
                deviceHardwareAddress = device.getName() + "\n" + device.getAddress(); // Name + MAC address в виде String переменной
                devicesList.add(deviceHardwareAddress);
                listAdapter.notifyDataSetChanged();
            }
            pairedDevicesTitleTextView.setText(R.string.paired_devices);
            //getListViewHeightBasedOnChildren(pairedList);
            //TODO
        } else {
            if (!stateOfAlertToAddDevice) {
                createOneButtonAlertDialog(getResources().getString(R.string.instruction_alert), getResources().getString(R.string.no_paired_devices));
            }
            stateOfAlertToAddDevice = true;
            //no_devices_added
            pairedDevicesTitleTextView.setText(R.string.no_devices_added);
            pairedList.setAdapter(null);
        }
    }

    // проверка на наличие Bluetooth адаптера; дальнейшее продолжение работы в случае наличия
    public void checkForBtAdapter() {
        if (btAdapter != null) {
            onRefresh();
        } else {
            System.out.println("There is no bluetooth adapter on device!");
            // объект Builder для создания диалогового окна
            //suggestionNoBtAdapter
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog).create();
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

    // Метод для вывода всплывающих данных на экран
    public void showToast(String outputInfoString) {
        Toast outputInfoToast = Toast.makeText(this, outputInfoString, Toast.LENGTH_SHORT);
        outputInfoToast.show();
    }

    // завершение работы приложения
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mMessageReceiverNotSuccess);
        unregisterReceiver(mMessageReceiverSuccess);
    }

    public static void getListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter != null) {
            int totalHeight = 0;
            int size = listAdapter.getCount();
            for (int i = 0; i < size; i++) {
                View listItem = listAdapter.getView(i, null, listView);
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }
            totalHeight = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        }
    }

    //Обновляем внешний вид приложения, скрываем и добавляем нужные элементы интерфейса
    @Override
    public void onRefresh() {
        stateOfBt = btIsEnabledFlagVoid();
        if (stateOfBt) {
            // Bluetooth включён. Предложим пользователю добавить устройства и начать передачу данных.
            if (stateOfFabToEnBt) {
                // Bluetooth включён, надо скрыть кнопку включения Bluetooth
                fabToEnBt.hide();
                stateOfFabToEnBt = false;
            }
            if (!stateOfFabToAddDevice) {
                // Bluetooth включён, надо показать кнопку добавления устройств и другую информацию
                fabToAddDevice.show();
                stateOfFabToAddDevice = true;
                pairedDevicesTitleTextView.setText(R.string.paired_devices);
                pairedDevicesTitleTextView.setVisibility(View.VISIBLE);
            }
            searchForDevice();
        } else {
            if (stateOfFabToAddDevice) {
                // Bluetooth выключён, надо скрыть кнопку добавления устройств и другую информацию
                fabToAddDevice.hide();
                stateOfFabToAddDevice = false;
                pairedList.setAdapter(null);
                pairedDevicesTitleTextView.setText(R.string.suggestionEnableBluetooth);
            }
            if (!stateOfFabToEnBt) {
                // Bluetooth выключён, надо показать кнопку включения Bluetooth
                fabToEnBt.show();
                stateOfFabToEnBt = true;
            }
        }
        // Приложение обновлено, завершаем анимацию обновления
        swipeToRefreshLayout.setRefreshing(false);
    }


    // создает диалоговое окно с 1й кнопкой
    private void createOneButtonAlertDialog(String title, String content) {
        // объект Builder для создания диалогового окна
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog);
        // добавляем различные компоненты в диалоговое окно
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setCancelable(true);
        // устанавливаем кнопку, которая отвечает за позитивный ответ
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            dialog.dismiss();
        });
        // объект Builder создал диалоговое окно и оно готово появиться на экране
        // вызываем этот метод, чтобы показать AlertDialog на экране пользователя
        builder.show();
    }
}