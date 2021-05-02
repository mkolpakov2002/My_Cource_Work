package com.miem.mmkolpakov.coursework;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity implements DevicesAdapter.SelectedDevice, SwipeRefreshLayout.OnRefreshListener {

    //Кнопки fab интерфейса
    //Включение Bluetooth
    ExtendedFloatingActionButton fabToEnBt;
    //Добавление устройств
    ExtendedFloatingActionButton fabToAddDevice;
    //Список устройств на главном экране
    RecyclerView pairedList;
    //Поиск среди устройств на главном экране
    SearchView searchView;
    //инициализация swipe refresh для обновления
    SwipeRefreshLayout swipeToRefreshLayout;
    /*
    boolean для отслеживания того,
     была ли единожды показана подсказка по добавлению новых устройств
    */
    boolean stateOfAlertToAddDevice = false;
    //Заголовок главного экрана
    TextView pairedDevicesTitleTextView;
    //MAC выбранного устройства
    String selectedDeviceId;
    //Имя выбранного устройства
    String selectedDeviceName;
    //Адаптер для работы со списком на главном экране
    DevicesAdapter devicesAdapter;
    //Переменная, хранящая информацию о том, первый ли это запуск
    int isFirstLaunch;
    SharedPreferences sPref;
    //Диалог при соединении с устройством
    ProgressDialog progressOfConnectionDialog;
    //Переменная для хранения времени между нажатиями кнопки назад
    private static long back_pressed = 0;
    //таг для логов
    private final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //инициализация toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //получение значения для проверки на первый запуск
        sPref = getPreferences(MODE_PRIVATE);
        isFirstLaunch = sPref.getInt("isFirstLaunch", 1);
        //получение сообщений из сервиса по подключению
        registerReceiver(mMessageReceiverNotSuccess, new IntentFilter("not_success_code_1"));
        registerReceiver(mMessageReceiverSuccess, new IntentFilter("success_code_1"));
        //инициализация кнопок fab на главном экране
        //Добавление устройств
        fabToAddDevice = findViewById(R.id.floating_action_button_Add_Device);
        //Включение Bluetooth
        fabToEnBt = findViewById(R.id.floating_action_button_En_Bt);
        //инициализация списка устройств
        pairedList = findViewById(R.id.paired_list);
        pairedList.setLayoutManager(new LinearLayoutManager(this));
        pairedList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        //инициализация заголовка главного экрана
        pairedDevicesTitleTextView = findViewById(R.id.paired_devices_title);
        //инициализация swipe to refresh
        swipeToRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        //по умолчанию все кнопки fab скрыты
        fabToEnBt.hide();
        fabToAddDevice.hide();
        //методы, выполняемые при нажатии кнопок fab
        fabToAddDevice.setOnClickListener(view -> {
            //открытие списка сопряжённных устройств в меню настроек устройства
            Intent intent_add_device = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent_add_device);
        });
        fabToEnBt.setOnClickListener(view -> {
            //открытие системного диалога с предложением включить Bluetooth
            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);
        });
        //метод, выполняемый при жесте обновления экрана
        //по-умолчанию onRefresh
        swipeToRefreshLayout.setOnRefreshListener(this);
        registerReceiver(BluetoothStateChanged, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Диалог включения Bluetooth закрыт");
        onRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // инициализация элементов меню главного экрана
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // параметры для поискового меню
        MenuItem menuItem = menu.findItem(R.id.search_device);
        searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        // изменение цвета иконки поиска на белый
        ImageView icon = searchView.findViewById(R.id.search_button);
        icon.setColorFilter(getResources().getColor(R.color.white));
        // методы при взаимодействии со строкой поиска
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

    // методы вызываемые при нажатии на элементы меню главного экрана
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // запуск Activity с краткой информацией о приложении
            Log.d(TAG, "запуск Activity с краткой информацией о приложении");
            startActivity(new Intent(this, InstructionsActivity.class));
            return true;
        }
        if (id == R.id.refresh_application) {
            // обновление экрана
            onRefresh();
            return true;
        }
        if (id == R.id.search_device) {
            // открытие строки поиска
            Log.d(TAG, "открытие строки поиска");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Результат работы Service при неуспешном соединении
    private final BroadcastReceiver mMessageReceiverNotSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showToast("Соединение не успешно");
            progressOfConnectionDialog.hide();
        }
    };

    //результат работы Service при успешном соединении
    private final BroadcastReceiver mMessageReceiverSuccess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Устройство подключено, Service выполнился успешно
            showToast("Соединение успешно");
            Intent startSendingData = new Intent(MainActivity.this, SendDataActivity.class);
            startSendingData.putExtra("idOfDevice", selectedDeviceId);
            startSendingData.putExtra("nameOfDevice", selectedDeviceName);
            startActivity(startSendingData);
            progressOfConnectionDialog.hide();
        }
    };

    //выполняемый код при изменении состояния bluetooth
    private final BroadcastReceiver BluetoothStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onRefresh();
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // проверка на присутствие Bluetooth адаптера у смартфона
        checkForBtAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        // проверка на присутствие Bluetooth адаптера у смартфона
        checkForBtAdapter();
    }

    //True, если Bluetooth на устройстве включён
    public boolean btIsEnabledFlagVoid(){
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    //Получаем адрес устройства из Recycle View
    public void startBluetoothConnectionService(DeviceModel deviceModel) {
        //вид получаемой информации - "Name\n00:00:00:00:00:00"
        //"\n" - это один символ
        /*
        В текущем пункте Recycle View находим первый символ ":",
        всё после него, а также два символа до него - адрес выбранного устройства
        */
        selectedDeviceId = deviceModel.getDeviceName()
                .substring(deviceModel.getDeviceName().indexOf(':') - 2);
        /*
        В текущем пункте Recycle View находим первый символ ":",
        всё кроме последних 3-ёх символов до него - имя выбранного устройства
        */
        selectedDeviceName = deviceModel.getDeviceName()
                .substring(0, deviceModel.getDeviceName().indexOf(':') - 3);
        showToast(selectedDeviceName + "\n" + selectedDeviceId);
        DialogChooseRobot dialog = new DialogChooseRobot();
        dialog.show(this.getSupportFragmentManager(), "dialog");
    }

    // Добавляем сопряжённые устройства в Recycle View
    public void searchForDevice(){
        // Обновление Recycle View - удаление старых данных
        pairedList.setAdapter(null);
        Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter()
                .getBondedDevices();
        // Если список спаренных устройств не пуст
        if(pairedDevices.size()>0) {
            List<DeviceModel> devicesList = new ArrayList<>();
            // устанавливаем связь между данными
            // проходимся в цикле по этому списку
            for (BluetoothDevice device : pairedDevices) {
                // Обновление Recycle View - добавляем в него сопряжённые устройства
                devicesList.add(new DeviceModel(device.getName() + "\n" +
                        device.getAddress()));
            }
            devicesAdapter = new DevicesAdapter(devicesList, this);
            // связь контейнера на экране с реальными данными
            pairedList.setAdapter(devicesAdapter);
            pairedDevicesTitleTextView.setText(R.string.paired_devices);
        } else {
            // список устройств пуст, предложим добавить их
            if (!stateOfAlertToAddDevice) {
                createOneButtonAlertDialog(getResources().getString(R.string.instruction_alert), getResources().getString(R.string.no_paired_devices));
                stateOfAlertToAddDevice = true;
            }
            //изменение заголовка главного экрана
            pairedDevicesTitleTextView.setText(R.string.no_devices_added);
            // нет данных для отображения
            pairedList.setAdapter(null);
        }
    }

    // проверка на наличие Bluetooth адаптера; дальнейшее продолжение работы в случае наличия
    public void checkForBtAdapter() {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            if (isFirstLaunch == 1 && btIsEnabledFlagVoid()){
                // выполнение кода при первом запуске приложения, когда включён Bluetooth
                sPref = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor ed = sPref.edit();
                // сохранение переменной int в памяти приложения
                // 0 - приложение уже было когда-либо запущено
                ed.putInt("isFirstLaunch", 0);
                ed.apply();
                isFirstLaunch = 0;
                // показ подсказки на экране
                createOneButtonAlertDialog(getResources().getString(R.string.instruction_alert),
                        getResources().getString(R.string.other_discoverable_devices));
            }
            // обновление главного экрана
            onRefresh();
        } else {
            // отсутствует Bluetooth адаптер, работа приложения невозможна
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialog).create();
            dialog.setTitle(getString(R.string.error));
            dialog.setMessage(getString(R.string.suggestionNoBtAdapter));
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    (dialog1, which) -> {
                        // скрывает диалог и завершает работу приложения
                        dialog1.dismiss();
                        MainActivity.this.finish();
                    });
            // нельзя закрыть этот диалог
            dialog.setCancelable(false);
            dialog.show();
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
        if (btIsEnabledFlagVoid()) {
            // Bluetooth включён, надо скрыть кнопку включения Bluetooth
            fabToEnBt.hide();
            // Bluetooth включён, надо показать кнопку добавления устройств и другую информацию
            pairedList.setVisibility(View.VISIBLE);
            fabToAddDevice.show();
            pairedDevicesTitleTextView.setText(R.string.paired_devices);
            pairedDevicesTitleTextView.setVisibility(View.VISIBLE);
            // поиск сопряжённых устройств
            searchForDevice();
        } else {
            // Bluetooth выключён, надо скрыть кнопку добавления устройств и другую информацию
            fabToAddDevice.hide();
            pairedList.setVisibility(View.INVISIBLE);
            pairedDevicesTitleTextView.setText(R.string.suggestionEnableBluetooth);
            // Bluetooth выключён, надо показать кнопку включения Bluetooth
            fabToEnBt.show();
        }
        // Приложение обновлено, завершаем анимацию обновления (при её наличии)
        swipeToRefreshLayout.setRefreshing(false);
    }


    // создает диалоговое окно с 1й кнопкой
    private void createOneButtonAlertDialog(String title, String content) {
        // объект Builder для создания диалогового окна
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog);
        // добавляем различные компоненты в диалоговое окно
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setCancelable(true);
        // устанавливаем кнопку, которая отвечает за позитивный ответ
        builder.setPositiveButton(getResources().getString(R.string.ok), (dialog, which) -> {
            dialog.dismiss();
        });
        // объект Builder создал диалоговое окно и оно готово появиться на экране
        // Создание alert dialog и изменение цвета его кнопок
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(arg0 -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.colorAccent));
        });
        // вызываем этот метод, чтобы показать AlertDialog на экране пользователя
        dialog.show();
    }

    //метод, вызываемый при нажатии кнопки назад на главном экране
    @Override
    public void onBackPressed() {
        if (!searchView.isIconified()) {
            //если открыта строка поиска, сворачиваем её
            searchView.setIconified(true);
        } else {
            //иначе даём возможность выйти из приложения, но по двойному нажатию кнопки назад
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                //показ сообщения, о необходимости второго нажатия кнопки назад при выходе
                showToast(getResources().getString(R.string.press_again));
            }
            back_pressed = System.currentTimeMillis();
        }

    }
    //получение информации о нажатом устройстве, вызов метода для начала соединения
    @Override
    public void selectedDevice(DeviceModel deviceModel) {
        startBluetoothConnectionService(deviceModel);
    }

}