package com.miem.mmkolpakov.coursework;

import android.bluetooth.BluetoothSocket;

public class DeviceHandler {
    private static BluetoothSocket socket;
    private static String selectedDeviceId;
    private static String selectedDeviceName;
    private static String selectedDeviceClass;

    public static synchronized BluetoothSocket getSocket(){
        return socket;
    }

    public static synchronized void setSocket(BluetoothSocket socket){
        DeviceHandler.socket = socket;
    }

    public static synchronized String getDeviceId(){
        return selectedDeviceId;
    }

    public static synchronized void setDeviceId(String selectedDeviceId){
        DeviceHandler.selectedDeviceId = selectedDeviceId;
    }

    public static synchronized String getDeviceName(){
        return selectedDeviceName;
    }

    public static synchronized void setDeviceName(String selectedDeviceName){
        DeviceHandler.selectedDeviceName = selectedDeviceName;
    }

    public static synchronized String getDeviceClass(){
        return selectedDeviceClass;
    }

    public static synchronized void setDeviceClass(String selectedDeviceClass){
        DeviceHandler.selectedDeviceClass = selectedDeviceClass;
    }
}
