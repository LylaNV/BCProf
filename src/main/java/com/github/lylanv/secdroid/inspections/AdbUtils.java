package com.github.lylanv.secdroid.inspections;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AdbUtils {
    public static boolean isAdbAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("adb get-state");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            process.waitFor();
            return line != null && !line.isEmpty();
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void startAdb() {
        try {
            Runtime.getRuntime().exec("adb start-server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopAdb() {
        try {
            Runtime.getRuntime().exec("adb kill-server");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopEmulator() {
        try {
            Runtime.getRuntime().exec("adb emu kill");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void clearLogCatFile() {
        try {
            Runtime.getRuntime().exec("adb logcat -c");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Process getLogCatFile() {
        try {
            return Runtime.getRuntime().exec("adb logcat");
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Process getEmulatorBatteryLevel() {
        try {
            return Runtime.getRuntime().exec("adb emu power display");
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
