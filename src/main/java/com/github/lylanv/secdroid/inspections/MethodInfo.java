package com.github.lylanv.secdroid.inspections;

import java.util.HashMap;
import java.util.Map;

public class MethodInfo {
    String methodName;
    String className;
    long startTime;
    //long endTime;
    long nestedTime; //Time spent in nested calls

    boolean cameraStatus_start;
    boolean gpsStatus_start;
    boolean screen_start;
    int screenBrightness_start;
    boolean bluetoothStatus_start;
    boolean networkStatus_start;
    boolean wifiStatus_start;
    boolean cellularDataStatus_start;
    Map<String, Integer[]> networkPackets_start = new HashMap<>();


    public MethodInfo(String methodName, String className, long startTime, boolean cameraStatus_start, boolean gpsStatus_start, boolean screen_start, int screenBrightness_start, boolean bluetoothStatus_start, Map<String, Integer[]> networkPackets_start) {
        this.methodName = methodName;
        this.className = className;
        this.startTime = startTime;
        this.nestedTime = 0;

        this.cameraStatus_start = cameraStatus_start;
        this.gpsStatus_start = gpsStatus_start;
        this.screen_start = screen_start;
        this.screenBrightness_start = screenBrightness_start;
        this.bluetoothStatus_start = bluetoothStatus_start;
        this.networkPackets_start = networkPackets_start;
    }


    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getNestedTime() {
        return nestedTime;
    }

    public void setNestedTime(long nestedTime) {
        this.nestedTime = nestedTime;
    }

    public boolean isCameraStatusStart() {
        return cameraStatus_start;
    }

    public boolean isGpsStatusStart() {
        return gpsStatus_start;
    }

    public boolean isScreenStart() {
        return screen_start;
    }

    public int getScreenBrightnessStart() {
        return screenBrightness_start;
    }

    public boolean isBluetoothStatusStart() {
        return bluetoothStatus_start;
    }

    public Map<String, Integer[]> getNetworkPacketsStart() {
        return networkPackets_start;
    }


}
