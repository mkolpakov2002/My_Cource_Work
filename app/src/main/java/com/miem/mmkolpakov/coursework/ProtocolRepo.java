package com.miem.mmkolpakov.coursework;

import java.util.HashMap;

public class ProtocolRepo extends HashMap<String, Byte> {

    public static HashMap<String, Byte> getDevicesID = new HashMap<>();
    public static HashMap<String, Byte> commandCodes = new HashMap<>();
    public static HashMap<String, Byte> commandType = new HashMap<>();

    public static Byte getDeviceCodeByte(String device) {
        return getDevicesID.get(device);
    }
    public static Byte getMoveCommandByte(String code) {
        return commandCodes.get(code);
    }

    public static Byte getCommandTypeByte(String code) {
        return commandType.get(code);
    }

    public ProtocolRepo(String code) {
        commandCodes.put("FORWARD", (byte) 0x01);
        commandCodes.put("FORWARD_STOP", (byte) 0x41);
        commandCodes.put("BACK", (byte) 0x02);
        commandCodes.put("BACK_STOP", (byte) 0x42);
        commandCodes.put("LEFT", (byte) 0x03);
        commandCodes.put("LEFT_STOP", (byte) 0x43);
        commandCodes.put("RIGHT", (byte) 0x0C);
        commandCodes.put("RIGHT_STOP", (byte) 0x4C);
        commandCodes.put("STOP", (byte) 0x7F);
        getDevicesID.put("class_android", (byte) 0x30);
        getDevicesID.put("class_computer", (byte) 0x65);
        getDevicesID.put("class_arduino", (byte) 0x7E);
        getDevicesID.put("type_sphere", (byte) 0x1);
        getDevicesID.put("type_anthropomorphic", (byte) 0x9);
        getDevicesID.put("type_cubbi", (byte) 0x41);
        getDevicesID.put("type_computer", (byte) 0x9D);
        commandType.put("redo_command", (byte) 0x15);
        commandType.put("new_command", (byte) 0x0A);
        commandType.put("type_move", (byte) 0xA1);
        commandType.put("type_tele", (byte) 0xB4);
        //TODO
        //Тип телеметрия + нет калибровки
        //this.put("type_tele", (byte) 0xB4);
    }

}