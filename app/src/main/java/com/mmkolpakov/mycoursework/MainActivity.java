package com.mmkolpakov.mycoursework;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements DevicesAdapter.SelectedDevice, SwipeRefreshLayout.OnRefreshListener {

    ExtendedFloatingActionButton fabToEnBt;
    ExtendedFloatingActionButton fabToAddDevice;
    RecyclerView pairedList;
    SearchView searchView;
    //инициализация swipe refresh
    SwipeRefreshLayout swipeToRefreshLayout;
    public BluetoothAdapter btAdapter;
    ProgressBar progressBar;
    boolean stateOfBt = false;
    boolean stateOfAlertToSendData = false;
    boolean stateOfAlertToAddDevice = false;
    static boolean stateOfToastAboutConnection = false;
    static boolean isItemSelected;

    TextView pairedDevicesTitleTextView;
    AlertDialog.Builder alertDialogBuilder;
    String deviceHardwareAddress;
    ArrayAdapter<String> listAdapter;
    public static BluetoothDevice device;
    public static BluetoothSocket clientSocket;
    String selectedDeviceId;
    String selectedDeviceName;
    DevicesAdapter devicesAdapter;
    int isFirstLaunch;
    SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sPref = getPreferences(MODE_PRIVATE);
        isFirstLaunch = sPref.getInt("isFirstLaunch", 1);

        registerReceiver(mMessageReceiverNotSuccess, new IntentFilter("not_success"));
        registerReceiver(mMessageReceiverSuccess, new IntentFilter("success"));
        this.fabToAddDevice = findViewById(R.id.floating_action_button_Add_Device);
        this.fabToEnBt = findViewById(R.id.floating_action_button_En_Bt);

        pairedList = findViewById(R.id.paired_list);
        pairedList.setLayoutManager(new LinearLayoutManager(this));
        pairedList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));


        pairedDevicesTitleTextView = findViewById(R.id.paired_devices_title);

        alertDialogBuilder = new AlertDialog.Builder(this);

        swipeToRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeToRefreshLayout.setColorSchemeResources(android.R.color.holo_green_light);
        progressBar = findViewById(R.id.progressBarStartSendingData);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        fabToEnBt.hide();
        fabToAddDevice.hide();

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
        MenuItem menuItem = menu.findItem(R.id.search_device);
        searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        ImageView icon = searchView.findViewById(R.id.search_button);
        icon.setColorFilter(getResources().getColor(R.color.white));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //запускается, когда будет нажата кнопка поиска
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //вызывается после ввода пользователем каждого символа в текстовом поле
                devicesAdapter.getFilter().filter(newText);
                return true;
            }
        });
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
        if (id == R.id.search_device) {

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
            startSendingData.putExtra("idOfDevice", selectedDeviceId);
            startSendingData.putExtra("nameOfDevice", selectedDeviceName);
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
    public void checkDeviceAddress(DeviceModel deviceModel) {
        if (!isItemSelected) {

            isItemSelected = true;

            selectedDeviceId = deviceModel.getDeviceName();
            selectedDeviceName = selectedDeviceId;
            //Get information from List View in String
            showToast(selectedDeviceId);
            int i = selectedDeviceId.indexOf(':');
            i = i - 2;
            //В текущем пункте List View находим первый символ ":", всё после него, а также два символа до него - адрес выбранного устройства
            selectedDeviceId = selectedDeviceId.substring(i);
            selectedDeviceName = selectedDeviceName.substring(0,i-1);
            progressBar.setVisibility(VISIBLE);
            // запускаем длительную операцию подключения в Service
            Intent startBluetoothConnectionService = new Intent(this, BluetoothConnectionService.class);
            startBluetoothConnectionService.putExtra("idOfDevice", selectedDeviceId);
            startService(startBluetoothConnectionService);
        } else {
            if (!stateOfToastAboutConnection) {
                showToast(getResources().getString(R.string.deviceIsSelected));
                stateOfToastAboutConnection = true;
            }
        }
    }


    // Добавляем сопряжённые устройства в Recycle View
    public void searchForDevice(){
        // Обновление List View - удаление старых данных
        pairedList.setAdapter(null);
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // Если список спаренных устройств не пуст
        if(pairedDevices.size()>0) {
            stateOfAlertToSendData = true;
            List<DeviceModel> devicesList = new ArrayList<>();
            List<String> a = new ArrayList<String>();


            // устанавливаем связь между данными
            // проходимся в цикле по этому списку
            for (BluetoothDevice device : pairedDevices) {
                // Обновление List View - добавляем в него сопряжённые устройства
                deviceHardwareAddress = device.getName() + "\n" + device.getAddress(); // Name + MAC address в виде String переменной
                a.add(deviceHardwareAddress);
            }
            String[] array = a.toArray(new String[0]);

            for (String s : array) {
                DeviceModel userModel = new DeviceModel(s);

                devicesList.add(userModel);
            }
            devicesAdapter = new DevicesAdapter(devicesList, this);

            pairedList.setAdapter(devicesAdapter);

            pairedDevicesTitleTextView.setText(R.string.paired_devices);
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
            if (isFirstLaunch == 1){
                sPref = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                ed.putInt("isFirstLaunch", 0);
                ed.apply();
                isFirstLaunch = 0;
                if (btIsEnabledFlagVoid()){
                    createOneButtonAlertDialog(getResources().getString(R.string.instruction_alert),
                            getResources().getString(R.string.other_discoverable_devices));
                }
            }
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

    //Обновляем внешний вид приложения, скрываем и добавляем нужные элементы интерфейса
    @Override
    public void onRefresh() {
        stateOfBt = btIsEnabledFlagVoid();
        if (stateOfBt) {
            // Bluetooth включён, надо скрыть кнопку включения Bluetooth
            fabToEnBt.hide();
            // Bluetooth включён, надо показать кнопку добавления устройств и другую информацию
            pairedList.setVisibility(View.VISIBLE);
            fabToAddDevice.show();
            pairedDevicesTitleTextView.setText(R.string.paired_devices);
            pairedDevicesTitleTextView.setVisibility(View.VISIBLE);
            searchForDevice();
        } else {
            // Bluetooth выключён, надо скрыть кнопку добавления устройств и другую информацию
            fabToAddDevice.hide();
            pairedList.setVisibility(View.INVISIBLE);
            pairedDevicesTitleTextView.setText(R.string.suggestionEnableBluetooth);
            // Bluetooth выключён, надо показать кнопку включения Bluetooth
            fabToEnBt.show();
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
        // Create the alert dialog and change Buttons colour
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface arg0) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.colorAccent));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.colorAccent));
                //dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(R.color.colorAccent));
            }
        });
        dialog.show();
    }



    private static long back_pressed = 0;

    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                showToast("Press again to exit");
            }
            back_pressed = System.currentTimeMillis();
        }

    }

    @Override
    public void selectedDevice(DeviceModel deviceModel) {
        checkDeviceAddress(deviceModel);
    }
}