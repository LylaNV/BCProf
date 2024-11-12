package com.github.lylanv.secdroid.inspections;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AdbUtils {
    private static String adbPath = "/Users/lylan/UiO/android/platform-tools/adb";

    public static boolean isAdbAvailable() {
        try {
            //String[] cmdArrayAdbAvailable = new String[1];
            //cmdArrayAdbAvailable[0] = "adb get-state";
            //Process process = Runtime.getRuntime().exec(cmdArrayAdbAvailable);


            ProcessBuilder pb = new ProcessBuilder(adbPath, "get-state");
            Process process = pb.start();


            //Process process = Runtime.getRuntime().exec("adb get-state");
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
//            String[] cmdArrayStartAdb = new String[1];
//            cmdArrayStartAdb[0] = "adb start-serve";
//            //Runtime.getRuntime().exec("adb start-server");
//            Runtime.getRuntime().exec(cmdArrayStartAdb);

            ProcessBuilder pb = new ProcessBuilder(adbPath, "start-serve");
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopAdb() {
        try {
//            String[] cmdArrayStopAdb = new String[1];
//            cmdArrayStopAdb[0] = "adb kill-server";
//            //Runtime.getRuntime().exec("adb kill-server");
//            Runtime.getRuntime().exec(cmdArrayStopAdb);

            ProcessBuilder pb = new ProcessBuilder(adbPath, "kill-server");
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stopEmulator() {
        try {
//            String[] cmdArrayStopEmulator = new String[1];
//            cmdArrayStopEmulator[0] = "adb emu kill";
//            //Runtime.getRuntime().exec("adb emu kill");
//            Runtime.getRuntime().exec(cmdArrayStopEmulator);

            ProcessBuilder pb = new ProcessBuilder(adbPath, "emu", "kill");
            pb.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void clearLogCatFile() {
        try {
//            String[] cmdArrayClearLogCatFile = new String[1];
//            cmdArrayClearLogCatFile[0] = "adb logcat -c";
//            //Runtime.getRuntime().exec("adb logcat -c");
//            Runtime.getRuntime().exec(cmdArrayClearLogCatFile);

            ProcessBuilder pb = new ProcessBuilder(adbPath, "logcat", "-c");
            pb.start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Process getLogCatFile() {
        try {
//            String[] cmdArrayGetLogCatFile = new String[1];
//            cmdArrayGetLogCatFile[0] = "adb logcat";
//            //return Runtime.getRuntime().exec("adb logcat");
//            return Runtime.getRuntime().exec(cmdArrayGetLogCatFile);

            //ProcessBuilder pb = new ProcessBuilder(adbPath, "adb logcat");
            ProcessBuilder pb = new ProcessBuilder(adbPath, "logcat");
            return pb.start();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static int getEmulatorBatteryLevel() {
        try {
//            String[] cmdArrayGetEmulatorBatteryLevel = new String[1];
//            cmdArrayGetEmulatorBatteryLevel[0] = "adb emu power display";
//            //return Runtime.getRuntime().exec("adb emu power display");
//            return Runtime.getRuntime().exec(cmdArrayGetEmulatorBatteryLevel);

            ProcessBuilder pb = new ProcessBuilder(adbPath, "emu", "power", "display");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));
                String lineBatteryPercentage;
                while ((lineBatteryPercentage = reader.readLine()) != null) {
                    if (lineBatteryPercentage.contains("capacity")) {
                        reader.close();
                        return Integer.parseInt(lineBatteryPercentage.split(" ")[1]);
                    }
                }

            } else {
                System.out.println("[AdbUtils -> getEmulatorBatteryLevel$ Failed to get emulator battery level");
                return -1;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    public static Boolean isEmulatorBooted() {
        try {
            /*
            * TODO: "emulator-5554" should be replaced with a string holding the emulator name
            * To achieve this, you should have a function in this class to get the name of the emulator
            * using "adb devices" command
            * */
            ProcessBuilder pb = new ProcessBuilder(adbPath, "-s", "emulator-5554", "shell", "getprop", "sys.boot_completed");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line = reader.readLine();
                if (line == null) {
                    return false;
                }else if(line.contains("1")){
                    return true;
                }else {
                    return false;
                }
            } else {
                System.out.println("[AdbUtils -> isEmulatorBooted$ Failed to get emulator status");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
