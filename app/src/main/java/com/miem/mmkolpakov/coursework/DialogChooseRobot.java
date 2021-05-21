package com.miem.mmkolpakov.coursework;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DialogChooseRobot extends DialogFragment  {
    Spinner spinnerType;
    String selectedDeviceName, selectedDeviceId;
    Context c;
    List<String> listTypes;
    AlertDialog.Builder builder;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            //передаём контекст из Activity
            c = context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //получаем данные из MainActivity
        //Имя устройства
        selectedDeviceName = ((MainActivity) c).selectedDeviceName;
        //MAC адрес устройства
        selectedDeviceId = ((MainActivity) c).selectedDeviceId;
        //создание диалога как объекта класса AlertDialog()
        builder = new AlertDialog.Builder(c, R.style.AlertDialog);
        //контейнер для View элементов диалога
        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        //создаём список протоколов в виде листа
        listTypes = new ArrayList<>();
        listTypes = Arrays.asList("type_sphere", "type_anthropomorphic", "type_cubbi", "type_computer");
        ArrayAdapter<String> adapterType = new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, listTypes);
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //создание выпадающего списка протоколов
        spinnerType = new AppCompatSpinner(c);
        //связь с данными из листа протоколов
        spinnerType.setAdapter(adapterType);
        //решение проблем с цветами на разных устройствах
        spinnerType.getBackground().setColorFilter(c.getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //выбор по умолчанию - type_sphere
        spinnerType.setSelection(0);
        //параметры отображения выпадающего списка
        spinnerType.setPadding(5,20,5,20);

        if(spinnerType.getParent() != null) {
            ((ViewGroup)spinnerType.getParent()).removeView(spinnerType); // <- фикс ошибки
        }
        layout.addView(spinnerType);
        return builder.setTitle(("Настройка подключения"))
                .setMessage("Выберите тип подключаемого устройства:")
                .setView(layout)
                .setPositiveButton("ок", (dialog, whichButton) -> {
                    // начало показа диалога о соединении
                    ((MainActivity) c).progressOfConnectionDialog = new ProgressDialog(c);
                    ((MainActivity) c).progressOfConnectionDialog.setMessage("Соединение...");
                    ((MainActivity) c).progressOfConnectionDialog.setCancelable(false);
                    ((MainActivity) c).progressOfConnectionDialog.setInverseBackgroundForced(false);
                    ((MainActivity) c).progressOfConnectionDialog.show();
                    String classDevice = spinnerType.getSelectedItem().toString();
                    // запускаем длительную операцию подключения в Service
                    Intent startBluetoothConnectionService = new Intent(c, BluetoothConnectionService.class);
                    //передача данных о адресе и имени устройства
                    startBluetoothConnectionService.putExtra("idOfDevice", selectedDeviceId);
                    startBluetoothConnectionService.putExtra("nameOfDevice", selectedDeviceName);
                    startBluetoothConnectionService.putExtra("classOfDevice", classDevice);
                    startBluetoothConnectionService.putExtra("startCode", 1);
                    ((MainActivity) c).startService(startBluetoothConnectionService);
                })
                //если диалог закрыли - обновить MainActivity
                .setNegativeButton("Отмена", (dialog, whichButton) -> ((MainActivity) c).onRefresh()
                )
                .setOnDismissListener(dialogInterface -> {
                    //если диалог свернули - обновить MainActivity
                    ((MainActivity) c).onRefresh();
                })
                .create();
    }
}
