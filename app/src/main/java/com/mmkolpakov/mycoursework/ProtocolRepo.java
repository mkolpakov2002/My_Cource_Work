package com.mmkolpakov.mycoursework;

import java.util.HashMap;

public class ProtocolRepo extends HashMap<String, Byte> {

    static HashMap<String, Byte> getDevicesID;
    private HashMap<String, Byte> commandType = new HashMap<>(), moveCodes = new HashMap<>();

    public static Byte getDeviceCode(String device) {
        getDevicesID = new HashMap<>();
        getDevicesID.put("class_android", (byte) 0x30);
        getDevicesID.put("class_computer", (byte) 0x65);
        getDevicesID.put("class_arduino", (byte) 0x7e);
        getDevicesID.put("type_sphere", (byte) 0x1);
        getDevicesID.put("type_anthropomorphic", (byte) 0x9);
        getDevicesID.put("type_cubbi", (byte) 0x41);
        getDevicesID.put("type_computer", (byte) 0x9d);
        return getDevicesID.get(device);
    }

    public ProtocolRepo(String code) {
        switch (code) {
            case "main_protocol":
                this.put("redo_command", (byte) 0x15);
                this.put("new_command", (byte) 0x0a);

                this.put("type_move", (byte) 0xa1);
                this.put("type_tele", (byte) 0xb4);

                this.put("FORWARD", (byte) 0x01);
                this.put("FORWARD_STOP", (byte) 0x41);
                this.put("BACK", (byte) 0x02);
                this.put("BACK_STOP", (byte) 0x42);
                this.put("LEFT", (byte) 0x03);
                this.put("LEFT_STOP", (byte) 0x43);
                this.put("RIGHT", (byte) 0x0c);
                this.put("RIGHT_STOP", (byte) 0x4c);
                this.put("STOP", (byte) 0x7f);
                break;
            case "wheel_platform":
                break;
        }
    }

}