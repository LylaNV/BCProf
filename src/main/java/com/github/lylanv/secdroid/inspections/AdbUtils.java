package com.github.lylanv.secdroid.inspections;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

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


    public static Boolean isApplicationRunning(String appPackageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "ps", "|", "grep", appPackageName );
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(appPackageName)) {
                        //FOR TEST
                        System.out.println("[AdbUtils -> isApplicationRunning$ Application is running!");
                        return true;
                    }
                }

                //FOR TEST
                System.out.println("[AdbUtils -> isApplicationRunning$ Application is not running yet!");
                reader.close();
                return false;
            } else {
                System.out.println("[AdbUtils -> isApplicationRunning$ FATAL ERROR: Failed to get application runtime status process!");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isApplicationRunning$ FATAL ERROR: I/O Exception -> Failed to get application runtime status!");
            return false;
        }
    }



    /* ****************************************************************************************************************
     * ************************************* HW. COMPONENTS COMMANDS **************************************************
     **************************************************************************************************************** */


    /* *****************************************************************
     * For checking Network component, both WiFi and Cellular data
     * numberOfPackets, isNetworkConnected, isNetworkConnectedShort
     * isNetworkConnectionEstablished, isPackageUsingNetwork
     ****************************************************************** */

    //Returns Number of Received and sent packets over WiFi and Cellular data
    public static Map<String,Integer[]> numberOfPackets() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "cat", "/proc/net/dev");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                Integer numberOfPacketsReceived = 0;
                Integer numberOfPacketsSent = 0;

                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                Map<String, Integer[]> map = new HashMap<String, Integer[]>();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("wlan0:") || line.contains("eth0:")) {
                        //wlan0 -> WiFi
                        //eth0 -> cellular data
                        String[] parts = line.split("\\s+");

                        if (parts.length < 17) {
                            System.out.println("[AdbUtils -> numberOfPackets$ FATAL ERROR. Unknown line format");
                            return null;
                        }

                        String interfaceName = parts[1].replace(":", "");
                        numberOfPacketsReceived = Integer.valueOf(parts[3]); // 3rd column is received packets
                        numberOfPacketsSent = Integer.valueOf(parts[11]); // 11th column is transmitted packets
                        System.out.println("[AdbUtils -> numberOfPackets$ Interface: " + interfaceName + ", Number of received packets: " + numberOfPacketsReceived + ", Number of sent packets: " + numberOfPacketsSent);

                        map.put(interfaceName, new Integer[]{numberOfPacketsReceived, numberOfPacketsSent});
                    }
                }

                reader.close();

                if (map.isEmpty()){
                    System.out.println("[AdbUtils -> numberOfPackets$ FATAL ERROR. Info for the connected interface is not available.");
                    return null;
                } else {
                    return map;
                }
            } else {
                System.out.println("[AdbUtils -> numberOfPackets$ FATAL ERROR. Failed to get number of packets process.");
                return null;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> numberOfPackets$ FATAL ERROR: IO Error. Failed to get number of packets.");
            return null;
        }
    }


    // returns one of the connected interfaces if network is available, otherwise returns null
    public static String isNetworkConnected() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "connectivity");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                boolean networkFound = false;
                while ((line = reader.readLine()) != null) {
                    if(networkFound){
                        if (line.contains("eth0")) {//Cellular data
                            //System.out.println("[AdbUtils -> isNetworkAvailable$ eth0");
                            networkFound = false;
                            return "eth0";
                        }else if (line.contains("wlan0")) { //WiFi
                            //System.out.println("[AdbUtils -> isNetworkAvailable$ wlan0");
                            networkFound = false;
                            return "wlan0";
                        }else{
                            networkFound = false;
                            //System.out.println("[AdbUtils -> isNetworkAvailable$ unknown");
                            return line; //Unknown
                        }
                    }
                    if (line.contains("Idle timers")) {
                        networkFound = true;
                    }
                }

                reader.close();
                return networkFound ? "idle timers" : null; //if networkFound is true the output will be idle timer, and it means that there is no connectivity, otherwise there is problem and the output will be null
            } else {
                System.out.println("[AdbUtils -> isNetworkAvailable$ FATAL ERROR. Failed to get network status");
                return null;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isNetworkAvailable$ FATAL ERROR: IO Error. Failed to get network status");
            return null;
        }
    }

    // returns true if network is available, otherwise returns false
    public static boolean isNetworkConnectedShort() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "connectivity", "|", "grep", "\"NetworkAgentInfo\"");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                boolean networkFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("eth0") || line.contains("wlan0")) {//Cellular data
                        return true;
                    }
                }

                reader.close();
                return false;
            } else {
                System.out.println("[AdbUtils -> isNetworkConnectedShort$ FATAL ERROR. Failed to get network status process");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isNetworkConnectedShort$ FATAL ERROR: IO Error. Failed to get network status");
            return false;
        }
    }

    // returns true if there is at least one established network connection, otherwise it returns false
    // established means there is an active network connection
    public static boolean isNetworkConnectionEstablished() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "netstat");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                boolean networkFound = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ESTABLISHED")) {
                        return true;
                    }
                }

                reader.close();
                return false;
            } else {
                System.out.println("[AdbUtils -> isNetworkConnectionEstablished$ FATAL ERROR. Failed to get network connections status process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isNetworkConnectionEstablished$ FATAL ERROR: IO Error. Failed to get network status");
            return false;
        }
    }

    // returns true if package name is in the output of the command
    // We cannot conclude that for sure that package is using the internet
    // but we did here. Consider to find better/ more exact solution.
    //TODO: find more exact solution
    public static boolean isPackageUsingNetwork(String appPackageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "connectivity");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(appPackageName)) {
                        return true;
                    }
                }

            }else {
                System.out.println("[AdbUtils -> isPackageUsingNetwork$ FATAL ERROR: Cannot read the output steam of the command!");
            }

            return false;
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isPackageUsingNetwork$ FATAL ERROR: IO Error. Failed to get network status");
            return false;
        }
    }

    /* *****************************************************************
     * For checking GPS component
     * isUsingGPS
     ****************************************************************** */

    //returns true if the application is using the GPS, otherwise it returns false
    public static boolean isUsingGPS(String appPackageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "location", "|", "grep", "\"gps", "provider", "request\"");
            Process pbProcess = pb.start();

            boolean packageVisited = false;
            boolean gpsOffVisited = false;

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

//                String line;
//                boolean gpsFound = false;
//                while ((line = reader.readLine()) != null) {
//                    if(gpsFound){
//                        //Test
//                        if (line.contains(appPackageName)) {//Cellular data
//                            //System.out.println("[AdbUtils -> isUsingGPS$ GPS is used");
//                            gpsFound = false;
//                            return true;
//                        }
//                    }
//                    if (line.contains("gps provider:")) {
//                        gpsFound = true;
//                    }
//                }



                String line;
                int counterAPK = 0;
                int counterGPS = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(appPackageName)) {
                        packageVisited = true;
                        counterAPK++;
                    }

                    if (line.contains("OFF")) {
                        gpsOffVisited = true;
                        counterGPS++;
                    }

                    //If both are visisted we can stop the loop because the result is achieved and false should be returned
                    if (counterGPS >= 1 && counterAPK >= 1) {
                        break;
                    }
                }

                reader.close();
                /*The expression evaluates to true only if:
                packageVisited is true, and
                gpsOffVisited is false.*/
                return packageVisited && !gpsOffVisited;

            } else {
                System.out.println("[AdbUtils -> isUsingGPS$ FATAL ERROR. Failed to get location status process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isUsingGPS$ FATAL ERROR: IO Error. Failed to get location status");
            return false;
        }
    }

    /* *****************************************************************
    * For checking display/screen component
    * isScreenOn, isAppCurrentFocusOFScreen, isAppInListOfFocusedApps
    ****************************************************************** */

    //returns true if the screen/display is on, otherwise it returns false
    public static boolean isScreenOn(){
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "power", "|", "grep", "\"userActivitySummary\"");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("0x1")) {//Screen is on
                        return true;
                    }else if (line.contains("0x0")) { //Screen is off
                        return false;
                    }
                }

                reader.close();
                return false;
            } else {
                System.out.println("[AdbUtils -> isScreenOn$ FATAL ERROR. Failed to get screen status process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isScreenOn$ FATAL ERROR: IO Error. Failed to get screen status");
            return false;
        }
    }

    //Returns ture if app is current focus -> Also we can conclude that the screen is on because if screen is this will return false
    // The output of this command is null when the screen is off
    public static boolean isAppCurrentFocusOFScreen(String appPackageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "window", "|", "grep", "-E", "\"mCurrentFocus\"");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(appPackageName)) {//app is the current focus
                        return true;
                    }
                }

                reader.close();
                return false; //app isn't the current focus
            } else {
                System.out.println("[AdbUtils -> isAppCurrentFocusOFScreen$ FATAL ERROR. Failed to get focus app on the screen process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isAppCurrentFocusOFScreen$ FATAL ERROR: IO Error. Failed to get focus app on the screen.");
            return false;
        }

    }

    //Returns ture if app is in the focused list -> We can't conclude that the screen is on because whether screen is on or off the output of the command will have a value
    public static boolean isAppInListOfFocusedApps(String appPackageName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys", "window", "|", "grep", "-E", "\"mFocusedApp\"");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(appPackageName)) {//app is in the list of focused apps
                        return true;
                    }
                }

                reader.close();
                return false; ///app isn't in the list of focused apps -> It was not in the focus
            } else {
                System.out.println("[AdbUtils -> isAppInListOfFocusedApps$ FATAL ERROR. Failed to get focused app list process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isAppInListOfFocusedApps$ FATAL ERROR: IO Error. Failed to get focused app list.");
            return false;
        }
    }

    /* *****************************************************************
     * For checking camera component
     *
     ****************************************************************** */
    //Returns ture if the camera is in used -> My base assumption is that the only app running on the phone is our app
    //so, if camera is open and in use, it means that app is using it
    public static boolean isCameraOn() {
        try {
            ProcessBuilder pb = new ProcessBuilder(adbPath, "shell", "dumpsys",  "media.camera", "|", "awk", "\"/==", "Service", "global", "info:", "==/,/Allowed", "user", "IDs:/\"");
            Process pbProcess = pb.start();

            if (pbProcess != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(pbProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Camera ID:")) {
                        return true;
                    }
                }
                reader.close();
                return false;
            } else {
                System.out.println("[AdbUtils -> isCameraOn$ FATAL ERROR. Failed to get camera status process.");
                return false;
            }
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("[AdbUtils -> isCameraOn$ FATAL ERROR: IO Error. Failed to get camera status.");
            return false;
        }
    }
}
