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
    String selectedDeviceName, selectedDeviceId, devType;
    Context c;
    List<String> listTypes;
    AlertDialog.Builder builder;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            c = context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        selectedDeviceName = ((MainActivity) c).selectedDeviceName;
        selectedDeviceId = ((MainActivity) c).selectedDeviceId;
        listTypes = new ArrayList<>();
        listTypes = Arrays.asList("type_sphere", "type_anthropomorphic", "type_cubbi", "type_computer", "no_type");
        builder = new AlertDialog.Builder(c, R.style.AlertDialog);

        LinearLayout layout = new LinearLayout(c);
        layout.setOrientation(LinearLayout.VERTICAL);
        ArrayAdapter<String> adapterType = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, listTypes);
        adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType = new AppCompatSpinner(c);
        spinnerType.setAdapter(adapterType);
        spinnerType.getBackground().setColorFilter(c.getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);
        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK); /* if you want your item to be white */
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        spinnerType.setSelection(0);
        spinnerType.setPadding(5,20,5,20);

        if(spinnerType.getParent() != null) {
            ((ViewGroup)spinnerType.getParent()).removeView(spinnerType); // <- fix crash
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
                .setNegativeButton("Отмена", (dialog, whichButton) -> {
                    ((MainActivity) c).onRefresh();
                }
                )
                .setOnDismissListener(dialogInterface -> {
                    //action when dialog is dismissed goes here
                    ((MainActivity) c).onRefresh();
                })
                .create();
    }
}
