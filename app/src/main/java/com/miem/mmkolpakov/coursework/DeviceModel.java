package com.miem.mmkolpakov.coursework;

import java.io.Serializable;

public class DeviceModel implements Serializable {

    private final String deviceName;

    public DeviceModel(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

}