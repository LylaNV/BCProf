package com.github.lylanv.secdroid.inspections;

public class PowerXML {

    // https://source.android.com/docs/core/power/values
    //All the numbers are from the above link except bluetoothActive
    public static double wifiActive = 31, radioActive = 200, gpsOn = 50, gpsSignalQualityBasedWeak = 10, gpsSignalQualityBasedStrong = 30, screenOn = 200, screenFull = 200, cameraAvg = 600, bluetoothActive = 20;

    //https://www.biologic.net/topics/battery-states-state-of-charge-soc-state-of-health-soh/#:~:text=The%20state%2Dof%2Dhealth%20(,charge%20to%20its%20rated%20capacity.
    //https://www.biologic.net/topics/battery-states-state-of-charge-soc-state-of-health-soh/
    public static double stateOfHealth = 100; //100%
    public static double batteryCapacity = 5000; //mAh

    public static void setWifiActive(double inWifiActive){
        wifiActive = inWifiActive;
    }
    public static double getWifiActive(){
        return wifiActive;
    }

    public static void setRadioActive(double inRadioActive){
        radioActive = inRadioActive;
    }
    public static double getRadioActive(){
        return radioActive;
    }

    public static void setGpsOn(double inGpsOn){
        gpsOn = inGpsOn;
    }
    public static double getGpsOn(){
        return gpsOn;
    }

    public static void setGpsSignalQualityBasedWeak(double inGpsSignalQualityBasedWeak){
        gpsSignalQualityBasedWeak = inGpsSignalQualityBasedWeak;
    }
    public static double getGpsSignalQualityBasedWeak(){
        return gpsSignalQualityBasedWeak;
    }

    public static void setGpsSignalQualityBasedStrong(double inGpsSignalQualityBasedStrong){
        gpsSignalQualityBasedStrong = inGpsSignalQualityBasedStrong;
    }
    public static double getGpsSignalQualityBasedStrong(){
        return gpsSignalQualityBasedStrong;
    }

    public static void setScreenOn(double inScreenOn){
        screenOn = inScreenOn;
    }
    public static double getScreenOn(){
        return screenOn;
    }

    public static void setScreenFull(double inScreenFull){
        screenFull = inScreenFull;
    }
    public static double getScreenFull(){
        return screenFull;
    }

    public static void setCameraAvg(double inCameraAvg){
        cameraAvg = inCameraAvg;
    }
    public static double getCameraAvg(){
        return cameraAvg;
    }


    public static void setStateOfHealth(double inStateOfHealth){
        stateOfHealth = inStateOfHealth;
    }
    public static double getStateOfHealth(){
        return stateOfHealth;
    }

    public static void setBatteryCapacity(double inBatteryCapacity){
        batteryCapacity = inBatteryCapacity;
    }
    public static double getBatteryCapacity(){
        return batteryCapacity;
    }

    public static void setBluetoothActive(double inBluetoothActive){
        bluetoothActive = inBluetoothActive;
    }
    public static double getBluetoothActive(){
        return bluetoothActive;
    }


}
