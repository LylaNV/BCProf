package com.github.lylanv.secdroid.toolWindows;

import com.github.lylanv.secdroid.events.ApplicationStartedEvent;
import com.github.lylanv.secdroid.events.ApplicationStoppedEvent;
import com.github.lylanv.secdroid.events.BuildSuccessEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.github.lylanv.secdroid.inspections.*;
import com.github.lylanv.secdroid.utils.TwoStringKey;
import com.google.common.eventbus.Subscribe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.Messages;

public class LogCatReader implements Runnable {

    private final String TAG = "GreenMeter"; // Logging tag -> will be used to filter the logcat file
    private final String MethodStartTAG = "METHOD_START";
    private final String MethodEndTAG = "METHOD_END";

    private volatile boolean running = true; // Volatile to ensure visibility across threads

    private volatile boolean updatingFlag = false; //to avoid getting error when exporting the output if client/user forgot to first click the SECDroid, also helping to control the multiple application runs and updating the tool window

    Map<String, Integer> redAPIsCount = new HashMap<>(); // Holds the name of APIs and their counts

    Map<TwoStringKey, Double> currentMethodEnergyUsageMap = new HashMap<>();

    public static double energyConsumption = 0;
    public static double batteryLevel = 0;

    private Timer timer;

    //HW components variables
    private Map<String,Integer[]> networkInitialUsageMap = new HashMap<>();
    private Map<String,Integer[]> networkCurrentUsageMap = new HashMap<>();

    private boolean gpsStatus_initial = false;
    private boolean gpsStatus_current = false;

    private boolean displayStatus_initial = false;
    private boolean displayStatus_current = false;

    private boolean cameraStatus_initial = false;
    private boolean cameraStatus_current = false;

    // TEST
    private double batteryStamp = 0;
    private boolean batteryChangedFlag = false;


    String applicationPackageName;

    private static int packagePid = -1;

    private boolean isOutputReady = false;

    // These variables are added to be able to calculate the energy consumption of HW components for each method
    private Stack<MethodInfo> stack = new Stack<>();
    private Map<TwoStringKey, Long> methodTimes = new HashMap<>();
    private Map<TwoStringKey, Double> hwBatteryConsumption = new HashMap<>();


    public LogCatReader() {
        //ERROR
        while (applicationPackageName == null) {
            applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();
        }



        this.timer = new Timer();
        updateLineGraph();


        //TEST : Commented
        //this.timer = new Timer();
        //updateLineGraph(); // I called it here to have it as an ongoing continuous graph
    }


    /*
    * ********************************************************************************************************
    *                                       EVENT HANDLERS
    * ********************************************************************************************************
    * */

    @Subscribe
    public void handleBuildSuccessEvent(BuildSuccessEvent event) throws IOException {
        if (event.getBuildStatus()){
            System.out.println("[LogCatReader -> handleBuildSuccessEvent$ Build successful");
            while (applicationPackageName == null){
                applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();
            }

            System.out.println("[LogCatReader -> handleBuildSuccessEvent$ Build successful - application package name: " + applicationPackageName);
        }
    }

    @Subscribe
    public void handleApplicationStoppedEvent(ApplicationStoppedEvent event) throws IOException {
        if (event.getApplicationStopped()){
            System.out.println("[LogCatReader -> handleBuildSuccessEvent$ Application stopped");

            stop();
        }
    }

    @Subscribe
    public void handleApplicationStartEvent(ApplicationStartedEvent event) throws IOException {
        if (event.getApplicationStarted()){
//            while (applicationPackageName == null){
//                applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();
//            }
        }
    }


    /*
     * ********************************************************************************************************
     *                                       RUN() and STOP()
     * ********************************************************************************************************
     * */

    @Override
    public void run() {
        try {
            System.out.println("[GreenEdge -> LogCatReader$ In the run method. Start reading and analyzing LogCat file ....");

            Boolean adbServerIsRunning = AdbUtils.isAdbAvailable();
            if (!adbServerIsRunning){
                AdbUtils.startAdb();
            }
            System.out.println("[GreenEdge -> LogCatReader$ The adb tool is available and started.");

            //Clears logcat file
            AdbUtils.clearLogCatFile();
            System.out.println("[GreenEdge -> LogCatReader$ The LogCat file has been cleared.");

            while (!AdbUtils.isEmulatorBooted()) {
                //Wait: Stay in the loop until emulator is booted!
            }

            if (AdbUtils.isEmulatorBooted()) {

                System.out.println("[GreenEdge -> LogCatReader$ Emulator Booted]");
                //Get battery level
                batteryLevel = AdbUtils.getEmulatorBatteryLevel();
                if(batteryLevel == -1){
                    System.out.println("[GreenEdge -> LogCatReader$ FATAL ERROR: Battery level is negative.");
                }

                if (applicationPackageName == null){
                    System.out.println("[GreenEdge -> LogCatReader$ FATAL ERROR: Cannot get initial status of hardware components! Application package name is null.");
                }else {

                    //monitorInitialStatusOfHW(applicationPackageName);
                    Map<String,Integer[]> tempMAP = monitoringStatusOfNetwork(applicationPackageName);
                    if (tempMAP != null){
                        networkInitialUsageMap = tempMAP;
                    }


                }

            }

            //Gets the logcat file to be able to process it
            Process logcatProcess = AdbUtils.getLogCatFile();

            //Starts analyzing the LogCat file if it is not null
            if (logcatProcess != null) {

                //Check if log statement are added
                if (Singleton.redAPICalls == null){
                    showSystemUsageDialog("Please first click the SECDroid button then run the application!");

                    //isOutputReady = false;
                    updatingFlag = false;

                    //AdbUtils.stopEmulator();
                    //stopWithoutSaving();

                }else if (Singleton.redAPICalls.isEmpty()){
                    showSystemUsageDialog("Please first click the SECDroid button then run the application!");

                    //isOutputReady = false;
                    updatingFlag = false;

                    //AdbUtils.stopEmulator();
                    //stopWithoutSaving();
                }else {
                    //isOutputReady = true;
                    updatingFlag = true;
                }


                System.out.println("[GreenEdge -> LogCatReader$ Reading the LogCat file...");

                Map<TwoStringKey, MethodInfo> methodMap = new HashMap<>();

                BufferedReader logcatReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));

                String line;
                while ((line = logcatReader.readLine()) != null && running && updatingFlag) {
                    //Filters the lines of the LogCat file with our considered TAG which is GreenMeter
                    if (line.contains(TAG)) {

                        // ******************************************  ANDROID API CALLS ENERGY *********************************************
                        //Log statement is for API calls
                        if (!getThirdElementOfLogStatement(line).contains(MethodStartTAG) && !getThirdElementOfLogStatement(line).contains(MethodEndTAG)){
                            //Extracts info and API call name form the line
                            String extractedAPICallName = getAPICallName(line);
                            String extractedInfo = getInfoFromLine(line);

                            //Updates the number of API calls count
                            redAPIsCount.put(extractedAPICallName, redAPIsCount.getOrDefault(extractedAPICallName, 0) + 1);

                            /*
                             * TODO: The idea of having a singleton class in plugins is a bad one.
                             *  The next line will generate null pointer error if we do not first
                             *  click the action button add log lines!!!
                             *  Consider to correct this!
                             * */

                            //TEST = Commented
                            energyConsumption = batteryLevel - Singleton.redAPICalls.get(extractedAPICallName);
//                            batteryLevel = energyConsumption;

                            //TEST
                            if (energyConsumption >= 0){
                            //if (batteryLevel >= 0){
                                LogcatAnalyzerToolWindowFactory.updateText(extractedInfo); //Update text area

                                //TEST
                                //LogcatAnalyzerToolWindowFactory.updateLineGraph(energyConsumption); //Update the line graph
                                batteryChangedFlag = true;
                                batteryStamp = batteryStamp + Singleton.redAPICalls.get(extractedAPICallName);

                                LogcatAnalyzerToolWindowFactory.updateBarChart(redAPIsCount.getOrDefault(extractedAPICallName,1),extractedAPICallName,extractedAPICallName); //Update the bar chart
                            } else {
                                AdbUtils.stopEmulator();
                            }

                        }else{ //Log statement is for Start or End of a method.
                            // ******************************************  METHOD/FUNCTION START *********************************************

                            if(getThirdElementOfLogStatement(line).contains(MethodStartTAG)){
                                //Extract elements
                                String firstElement = getFirstElementOfLogStatement(line);
                                String secondElement = getSecondElementOfLogStatement(line);

                                //To calculate the energy consumption of hardware components for methods
//                                MethodInfo methodInfo = new MethodInfo(firstElement,secondElement,System.currentTimeMillis(), AdbUtils.isCameraOn(),AdbUtils.isUsingGPS(applicationPackageName),
//                                        AdbUtils.isScreenOn(),AdbUtils.getScreenBrightnessLevel(),AdbUtils.isBluetoothConnected(),checkNetwork(applicationPackageName));

                                long startTimeTemp = System.currentTimeMillis();
                                MethodInfo methodInfo = new MethodInfo(firstElement,secondElement,startTimeTemp, batteryLevel);
                                Map<String,Integer[]> tempNetworkStatusMap = monitoringStatusOfNetwork(applicationPackageName);
                                if (tempNetworkStatusMap != null){
                                    methodInfo.setNetworkPacketsStart(tempNetworkStatusMap);
                                }

                                stack.push(methodInfo);

                            } else if (getThirdElementOfLogStatement(line).contains(MethodEndTAG)) {
                                // ******************************************  METHOD/FUNCTION START *********************************************

                                MethodInfo methodInfo = stack.pop();
                                long totalTime = System.currentTimeMillis() - methodInfo.getStartTime();
                                long selfTime = totalTime - methodInfo.getNestedTime();
                                double selfTimeSeconds = ((double) selfTime)/ 1000;
                                //long selfTimeSeconds = selfTime / 1000;

                                double batteryChargeStamp = methodInfo.getBatteryCharge();

                                if (!stack.isEmpty()) {
                                    Long updatedNestedTime = (stack.peek().getNestedTime()) + totalTime;
                                    stack.peek().setNestedTime(updatedNestedTime);
                                }

                                //Extract elements
                                String firstElement = getFirstElementOfLogStatement(line);
                                String secondElement = getSecondElementOfLogStatement(line);

                                Object key1 = secondElement;
                                Object key2 = firstElement;

                                TwoStringKey key = new TwoStringKey(secondElement, firstElement);

                                Double hwBatteryConsumptionValue = 0.0;
                                if (hwBatteryConsumption != null && hwBatteryConsumption.containsKey(key)) {
                                    hwBatteryConsumptionValue = hwBatteryConsumption.get(key);
                                }

                                double batteryChargeHelper = 0;

                                // Camera
                                if (AdbUtils.isCameraOn()){
                                    batteryChargeHelper = batteryPercentage(PowerXML.getCameraAvg(),selfTimeSeconds,batteryChargeStamp);
                                    hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                    batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                }

                                //GPS
                                if (AdbUtils.isUsingGPS(applicationPackageName)){
                                    batteryChargeHelper = batteryPercentage(PowerXML.getGpsOn(),selfTimeSeconds,batteryChargeStamp);
                                    hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                    batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                }

                                //Screen
                                if (AdbUtils.isScreenOn()){
                                    double brightnessLevelOfScreen = AdbUtils.getScreenBrightnessLevel();
                                    if (brightnessLevelOfScreen != -1){
                                        batteryChargeHelper = (batteryPercentage(PowerXML.getScreenOn(),selfTimeSeconds,batteryChargeStamp) + batteryPercentage((PowerXML.getScreenFull() * (brightnessLevelOfScreen) / 255), selfTimeSeconds, batteryChargeStamp));

                                        hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                        batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;

                                    }else {

                                        batteryChargeHelper = batteryPercentage(PowerXML.getScreenOn(),selfTimeSeconds,batteryChargeStamp);

                                        hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                        batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                    }
                                }

                                //Bluetooth
                                if (AdbUtils.isBluetoothConnected()){
                                    batteryChargeHelper = batteryPercentage(PowerXML.getBluetoothActive(),selfTimeSeconds,batteryChargeStamp);
                                    hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                    batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                }

                                //Network
                                if (methodInfo.getNetworkPacketsStart() != null){
                                    if (!methodInfo.getNetworkPacketsStart().isEmpty()){
                                        //Network (Cellular data and Wi-Fi) battery consumption
                                        Map<String,Integer[]> networkPacketsAtStart = methodInfo.getNetworkPacketsStart();
                                        Map<String,Integer[]> networkCurrentPackets = monitoringStatusOfNetwork(applicationPackageName);
                                        if (networkCurrentPackets != null && networkCurrentPackets != null) {
                                            if (networkCurrentPackets.size() > 0 && networkPacketsAtStart.size() > 0){
                                                for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentPackets.entrySet()) {
                                                    for (Map.Entry<String,Integer[]> entryInitial: networkPacketsAtStart.entrySet()){
                                                        if (entryCurrent.getKey().contains(entryInitial.getKey())) {
                                                            if (entryCurrent.getValue()[0] - entryInitial.getValue()[0] > 0 || entryCurrent.getValue()[1] - entryInitial.getValue()[1] > 0) {
                                                                //Calculation of battery consumption
                                                                //Application is using network calculate based on the power
                                                                //wlan0 -> Wi-Fi
                                                                //eth0 -> cellular data
                                                                if (entryCurrent.getKey().contains("wlan0")){ // Wi-Fi
                                                                    batteryChargeHelper = batteryPercentage(PowerXML.getWifiActive(),selfTimeSeconds,batteryChargeStamp);
                                                                    hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                                                    batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;

                                                                }else if (entryCurrent.getKey().contains("eth0")){// Cellular data
                                                                    batteryChargeHelper = batteryPercentage(PowerXML.getRadioActive(),selfTimeSeconds,batteryChargeStamp);
                                                                    hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                                                    batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }else if (networkPacketsAtStart.size() == 0 && networkCurrentPackets.size() != 0){
                                                for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentPackets.entrySet()){
                                                    if (entryCurrent.getKey().contains("wlan0")){ // Wi-Fi
                                                        batteryChargeHelper = batteryPercentage(PowerXML.getWifiActive(),selfTimeSeconds,batteryChargeStamp);
                                                        hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;

                                                        batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;

                                                    }else if (entryCurrent.getKey().contains("eth0")){// Cellular data
                                                        batteryChargeHelper = batteryPercentage(PowerXML.getRadioActive(),selfTimeSeconds,batteryChargeStamp);
                                                        hwBatteryConsumptionValue = hwBatteryConsumptionValue + batteryChargeHelper;


                                                        batteryChargeStamp = batteryChargeStamp - batteryChargeHelper;
                                                    }
                                                }
                                            }else if (networkPacketsAtStart.size() == 0 && networkCurrentPackets.size() == 0){
                                                System.out.println("[GreenEdge -> LogCatReader -> run$ Fatal Error: Initial and Current network status is not available!");
                                            }
                                        }

                                    }
                                }

                                /*
                                * [LogCatReader -> LogCatReader$ FATAL ERROR: I/O Exception. Failed to analyze the logcat file in Run() method.
                                * java.lang.NullPointerException: Cannot invoke "java.util.Map.put(Object, Object)" because "this.hwBatteryConsumption" is null
	                            * at com.github.lylanv.secdroid.toolWindows.LogCatReader.run(LogCatReader.java:452)
	                            * at java.base/java.lang.Thread.run(Unknown Source)*/
                                if (hwBatteryConsumption == null){
                                    hwBatteryConsumption = new HashMap<>();
                                    hwBatteryConsumption.put(key,hwBatteryConsumptionValue);
                                } else {
                                    if (hwBatteryConsumption.containsKey(key)){
                                        hwBatteryConsumption.replace(key, hwBatteryConsumptionValue);
                                    }else {
                                        hwBatteryConsumption.put(key, hwBatteryConsumptionValue);
                                    }

                                }

                                //COMMENTED BECAUSE OF HW ONLY
                                //API calls
                                Double energy = Singleton.methodsAPICallsTotalEnergyCostMap.get(key);
                                if (energy == null){
                                    //System.out.println("[GreenEdge -> LogCatReader$ Energy is null for " + key.getPart1() + ", " + key.getPart2() + "]");
                                    energy = 0.0;
                                }
//                                //ADDED BECAUSE OF HW ONLY
//                                Double energy = 0.0;

                                //TODO - IMP -DONE
                                //java.lang.NullPointerException: Cannot invoke "java.util.Map.isEmpty()" because "this.currentMethodEnergyUsageMap" is null
                                //	at com.github.lylanv.secdroid.toolWindows.LogCatReader.run(LogCatReader.java:514)
                                //	at java.base/java.lang.Thread.run(Unknown Source)
                                if (currentMethodEnergyUsageMap != null){
                                    if (currentMethodEnergyUsageMap.isEmpty()) {
                                        currentMethodEnergyUsageMap.put(key, energy);
                                        Object[] rowData = {key1, key2, energy, hwBatteryConsumptionValue};
                                        LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);

                                    } else if (currentMethodEnergyUsageMap.containsKey(key)) {
                                        Double oldEnergy = currentMethodEnergyUsageMap.get(key);
                                        if (oldEnergy == null) {
                                            //System.out.println("[GreenEdge -> LogCatReader$ oldEnergy is null for " + key.getPart1() + ", " + key.getPart2() + "]");
                                            oldEnergy = 0.0;
                                        }
                                        Double newEnergy = oldEnergy + energy;
                                        //System.out.println("[GreenEdge -> LogCatReader$ Old Energy: " + oldEnergy + "]");
                                        currentMethodEnergyUsageMap.put(key, newEnergy);
                                        Object[] rowData = {key1, key2, newEnergy, hwBatteryConsumptionValue};
                                        LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);
                                    }else {
                                        currentMethodEnergyUsageMap.put(key, energy);
                                        Object[] rowData = {key1, key2, energy, hwBatteryConsumptionValue};
                                        LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);
                                    }
                                } else {
                                    currentMethodEnergyUsageMap = new HashMap<>();
                                    currentMethodEnergyUsageMap.put(key, energy);
                                    Object[] rowData = {key1, key2, energy, hwBatteryConsumptionValue};
                                    LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);
                                }



                            }

                        }

                    } else {
                        // TODO: I got null pointer- Check it
                        if (DroidEC.projectName != null){
                            if (line.contains(DroidEC.projectName)) {
                                System.out.println(line);
                            }
                        }
                    }
                }
                logcatReader.close();
                logcatProcess.destroy();

//                fillTheTable(numberOfRunningEachMethodMap);

                System.out.println("[GreenEdge -> LogCatReader$ Reading the LogCat file has been finished.");
                System.out.println("[GreenEdge -> LogCatReader$ The LogCatReader is closed and destroyed!");

            }else {
                System.out.println("[LogCatReader -> LogCatReader$ FATAL ERROR: Failed to get emulator logcat file process!.");
                logcatProcess.destroy();
            }

        }catch (Exception e) {
            System.out.println("[LogCatReader -> LogCatReader$ FATAL ERROR: I/O Exception. Failed to analyze the logcat file in Run() method.");
            e.printStackTrace();
        }
    }


    //Cancel timer and clear the variables, LogCat file, and all components in the tool window
    public void stop() {

        running = false;

        if (updatingFlag) {
            LogcatAnalyzerToolWindowFactory.saveResultsToFile();
            LogcatAnalyzerToolWindowFactory.clearAllComponents();
        }

        AdbUtils.clearLogCatFile();

        if (redAPIsCount != null) {
            redAPIsCount.clear();
        }

        //NOT TESTED
        if (networkInitialUsageMap != null){
            networkInitialUsageMap.clear();
        }
        if (networkCurrentUsageMap != null){
            networkCurrentUsageMap.clear();
        }
        //NOT TESTED
        gpsStatus_initial = false;
        gpsStatus_current = false;

        displayStatus_initial = false;
        displayStatus_current = false;

        cameraStatus_initial = false;
        cameraStatus_current = false;


        // These variables are added to be able to calculate the energy consumption of HW components for each method
        if (stack!=null){stack = null;}
        if (methodTimes!=null){methodTimes = new HashMap<>();}
        if (hwBatteryConsumption!=null){hwBatteryConsumption = null;}

        if (currentMethodEnergyUsageMap!=null){currentMethodEnergyUsageMap = null;}

        if (redAPIsCount != null){redAPIsCount = null;}

        energyConsumption = 0;
        batteryLevel = 0;

        //TEST
        batteryStamp = 0;
        batteryChangedFlag = false;

        timer.cancel();

        packagePid = -1;
//        AdbUtils.gpsUsagePreviousCount = 0;
//        AdbUtils.gpsUsageCount = 0;
//        AdbUtils.gpsActiveInPreviousSlot = false;

        EventBusManager.unregister(this);

    }


    /*
     * ********************************************************************************************************
     *                                       HW COMPONENTS
     * ********************************************************************************************************
     * */

    private Map<String,Integer[]> monitoringStatusOfNetwork(String appPkgName) {

        packagePid = AdbUtils.getPackagePid(appPkgName);
        return AdbUtils.numberOfPackagePackets(packagePid);

//        //check network
//        networkInitialUsageMap = checkNetwork(applicationPackageName);

    }

//    private void monitorInitialStatusOfHW(String applicationPackageName) {
//        networkInitialUsageMap = checkNetwork(applicationPackageName);
//        gpsStatus_initial = AdbUtils.isUsingGPS(applicationPackageName);
//        displayStatus_initial = AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName);
//        cameraStatus_initial = AdbUtils.isCameraOn();
//
//        //TEST
//        System.out.println("[GreenEdge -> LogCatReader$ gpsStatus_initial " + gpsStatus_initial);
//        System.out.println("[GreenEdge -> LogCatReader$ displayStatus_initial " + displayStatus_initial);
//        System.out.println("[GreenEdge -> LogCatReader$ cameraStatus_initial" + cameraStatus_initial);
//    }
//
//    private void monitorCurrentStatusOfHW(String applicationPackageName) {
//        networkCurrentUsageMap = checkNetwork(applicationPackageName);
//        gpsStatus_current = AdbUtils.isUsingGPS(applicationPackageName);
//        displayStatus_current = AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName);
//        cameraStatus_current = AdbUtils.isCameraOn();
//
//        //TEST
//        System.out.println("[GreenEdge -> LogCatReader$ gpsStatus_current " + gpsStatus_current);
//        System.out.println("[GreenEdge -> LogCatReader$ displayStatus_current " + displayStatus_current);
//        System.out.println("[GreenEdge -> LogCatReader$ cameraStatus_current" + cameraStatus_current);
//    }

//    private Map<String, Integer[]> checkNetwork(String applicationPackageName) {
//
//        String connectedInterface =  AdbUtils.isNetworkConnected();
//        if (connectedInterface != null) {
//            System.out.println("[LogCatReader -> checkNetwork$ NETWORK is CONNECTED");
//            if (!connectedInterface.contains("Idle timers")) {
//                boolean networkConnectionEstablished = AdbUtils.isNetworkConnectionEstablished();
//
//                if (networkConnectionEstablished) {
//                    System.out.println("[LogCatReader -> checkNetwork$ NETWORK CONNECTION ESTABLISHED!");
//                    //System.out.println("[LogCatReader -> checkNetwork$ There is an active/established network connection.");
//                    if (AdbUtils.isPackageUsingNetwork(applicationPackageName)) {
//                        System.out.println("[LogCatReader -> checkNetwork$ PACKAGE USING NETWORK!");
//                        //System.out.println("[LogCatReader -> checkNetwork$ There is an active/established network connection for this application.");
//                        if (AdbUtils.numberOfPackets() != null) {
//                            //System.out.println("[ALogCatReader -> checkNetwork$ Returning number of packets.");
//                            System.out.println("[LogCatReader -> checkNetwork$ NUMBER OF PACKETS " + AdbUtils.numberOfPackets());
//                            return AdbUtils.numberOfPackets();
//                        }else {
//                            System.out.println("[LogCatReader -> checkNetwork$ Problem in extracting number of packets for this application.");
//                        }
//                    }
//
//                }else {
//                    System.out.println("[LogCatReader -> checkNetwork$ NETWORK CONNECTION IS NOT ESTABLISHED!");
//                }
//
//            }else {
//                System.out.println("[LogCatReader -> checkNetwork$ NETWORK is IDLE");
//            }
//
//        }else {
//            System.out.println("[LogCatReader -> checkNetwork$ NETWORK is NOT CONNECTED! CHECK ADB!");
//        }
//
//        return null;
//    }


    /*
     * ********************************************************************************************************
     *                                       TOOL WINDOW UPDATE
     * ********************************************************************************************************
     * */

    //To update the line graph periodically even without new energy consumptions value
    public void updateLineGraph() {
        System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (running && updatingFlag) {

                    //If application package name is still null try to get it
                    if(applicationPackageName == null){
                        System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$  Package is null, trying to get it.");
                        applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();
                        System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$  Package is " + applicationPackageName);
                    }

                    if (batteryLevel > 0 ) {

                        // ******************************************  ANDROID API CALLS ENERGY *********************************************
                        //COMMENTED BECAUSE OF HW ONLY
                        //Reflects the battery consumption of the Android API calls
                        if (batteryChangedFlag){
                            batteryChangedFlag = false;
                            batteryLevel = batteryLevel - batteryStamp;
                            batteryStamp = 0;
                        }

                        // ******************************************  HW COMPONENTS ENERGY *********************************************
                        //Camera battery consumption
                        if (AdbUtils.isCameraOn()){

                            batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getCameraAvg());

                            if(AdbUtils.isScreenOn()){
                                double brightnessLevelOfScreen = AdbUtils.getScreenBrightnessLevel();
                                if (brightnessLevelOfScreen != -1){

                                    batteryLevel = batteryLevel - (batteryPercentageInOneSecond(PowerXML.getScreenOn()) + batteryPercentageInOneSecond((PowerXML.getScreenFull() * (brightnessLevelOfScreen)/255)));
                                }else {
                                    batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getScreenOn());
                                }
                            }
                        }

                        //Screen battery consumption
                        if (AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName)){
                            double brightnessLevelOfScreen = AdbUtils.getScreenBrightnessLevel();
                            if (brightnessLevelOfScreen != -1){
                                batteryLevel = batteryLevel - (batteryPercentageInOneSecond(PowerXML.getScreenOn()) + batteryPercentageInOneSecond((PowerXML.getScreenFull() * (brightnessLevelOfScreen)/255)));
                            }else {
                                batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getScreenOn());
                            }

                        }

                        //GPS battery consumption
                        if (AdbUtils.isUsingGPS(applicationPackageName)){
                            batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getGpsOn());
                        }

                        //Network (Cellular data and Wi-Fi) battery consumption
                        packagePid = AdbUtils.getPackagePid(applicationPackageName);
                        if (packagePid != -1){
                            networkCurrentUsageMap = AdbUtils.numberOfPackagePackets(packagePid);
                            if (networkCurrentUsageMap != null && networkInitialUsageMap != null){
                                if (networkInitialUsageMap.size() != 0 && networkCurrentUsageMap.size() != 0){
                                    for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentUsageMap.entrySet()) {
                                        for (Map.Entry<String,Integer[]> entryInitial: networkInitialUsageMap.entrySet()){
                                            if (entryCurrent.getKey().contains(entryInitial.getKey())) {
                                                if (entryCurrent.getValue()[0] - entryInitial.getValue()[0] > 0 || entryCurrent.getValue()[1] - entryInitial.getValue()[1] > 0) {
                                                    //Application is using network calculate based on the power
                                                    //wlan0 -> Wi-Fi
                                                    //eth0 -> cellular data
                                                    if (entryCurrent.getKey().contains("wlan0")){ // Wi-Fi

                                                        batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getWifiActive());

                                                    }else if (entryCurrent.getKey().contains("eth0")){ // Cellular data

                                                        batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getRadioActive());

                                                    }

                                                    //Updating network status
                                                    Integer[] newValues = entryCurrent.getValue();
                                                    Integer[] oldValues = entryInitial.getValue();

                                                    /* Replacing map value
                                                    public V replace(K key, V newValue)
                                                    public boolean replace(K key, V oldValue, V newValue)*/
                                                    networkInitialUsageMap.replace(entryCurrent.getKey(), newValues);
                                                }
                                            }
                                        }
                                    }
                                }else if (networkInitialUsageMap.size() == 0 && networkCurrentUsageMap.size() != 0){
                                    for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentUsageMap.entrySet()){
                                        if (entryCurrent.getKey().contains("wlan0")){ // Wi-Fi

                                            batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getWifiActive());

                                        }else if (entryCurrent.getKey().contains("eth0")){ // Cellular data

                                            batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getRadioActive());
                                        }
                                    }
                                    networkInitialUsageMap.putAll(networkCurrentUsageMap);
                                }else if (networkInitialUsageMap.size() == 0 && networkCurrentUsageMap.size() == 0){
                                    System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ Fatal Error: Initial and Current network status is not available!");
                                }

                            }else if (networkCurrentUsageMap != null && networkInitialUsageMap == null) {
                                System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ Calculating NET - networkInitialUsageMap NULL!");
                                //current info available and initial is null -> network is connected and established recently -> update the initial map -> no need to update energy
                                for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentUsageMap.entrySet()) {
                                    networkInitialUsageMap.put(entryCurrent.getKey(), entryCurrent.getValue());
                                }
                            } else {
                                System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ Fatal Error: Current network status is not available!");
                            }

                        }
//                        else {
//                            System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ Could not get package pid!");
//                        }


                        //Bluetooth battery consumption
                        if (AdbUtils.isBluetoothConnected()){
                            batteryLevel = batteryLevel - batteryPercentageInOneSecond(PowerXML.getBluetoothActive());
                        }

                        LogcatAnalyzerToolWindowFactory.updateLineGraph(batteryLevel);

                    }

                }

            }
        }
        ,0,1000);  // Updated every 1 second
    }


    /*
     * ********************************************************************************************************
     *                                       HELPER METHODS
     * ********************************************************************************************************
     * */

    // Shows a dialog window in the android studio with the input message
    public static void showSystemUsageDialog(String message) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showMessageDialog(message, "SECDroid Usage", Messages.getInformationIcon());
        });

    }

    //Gets info from the logged line (log statements)
    private String getInfoFromLine(String line) {
        // finds the index of the open parentheses signe ("(") in the logged line
        int openParentheses = line.indexOf('(');
        if (openParentheses < 0) {
            return null;
        }

        // finds the index of the close parentheses signe (")") in the logged line
        int closeParentheses = line.indexOf(')');
        if (closeParentheses < 0) {
            return null;
        }

        // returns the string in the parentheses
        return line.substring(openParentheses + 1, closeParentheses).trim();

    }

    //Gets API/method call name from the logged line log statements)
    private String getAPICallName(String line) {
        // finds the index of the open parentheses signe ("(") in the logged line
        int index = line.indexOf('(');
        if (index < 0) {
            return null;
        }

        // finds the index of the first comma after open parentheses signe ("(") in the logged line
        int firstComma = line.indexOf(',', index + 1);
        if (firstComma < 0) {
            return null;
        }

        // returns the first argument in the parentheses
        return line.substring(index + 1, firstComma).trim();
    }

    //Gets the first element from the logged line (log statements)
    private String getFirstElementOfLogStatement(String line) {
        String[] parts = line.split("[(),]");
        return parts[1];
    }

    //Gets the second element from the logged line (log statements)
    private String getSecondElementOfLogStatement(String line) {
        String[] parts = line.split("[(),]");
        return parts[2];
    }

    ////Gets the third element from the logged line (log statements)
    private String getThirdElementOfLogStatement(String line) {
        String[] parts = line.split("[(),]");
        return parts[3];
    }

    private double batteryPercentageInOneSecond(double inMilliAmpSecond) {

//        //Battery capacity = 6000 mAh = 21600000 mAs -> 100% battery charge and 100% battery health
//        return  (100*inMilliAmpSecond)/((PowerXML.getStateOfHealth()/100) * (batteryLevel/100) * (PowerXML.getBatteryCapacity()*3600));

        //TEST COMMENTED
        //Battery capacity = 6000 mAh = 21600000 mAs -> 100% battery charge and 100% battery health
        //return  (100 * inMilliAmpSecond)/((PowerXML.getStateOfHealth()/100) * (batteryLevel/100) * (PowerXML.getBatteryCapacity()*3600));
        return  (100 * inMilliAmpSecond)/((PowerXML.getStateOfHealth()/100) * (PowerXML.getBatteryCapacity()*3600));

    }

    private double batteryPercentage(double inMilliAmpSecond, double duration, double batteryChargeStamp) {

//        //Battery capacity = 6000 mAh = 21600000 mAs -> 100% battery charge and 100% battery health
//        return  (100*inMilliAmpSecond*duration)/((PowerXML.getStateOfHealth()/100) * (batteryChargeStamp/100) * (PowerXML.getBatteryCapacity()*3600));

        //TEST COMMENTED
        //Battery capacity = 6000 mAh = 21600000 mAs -> 100% battery charge and 100% battery health
        //return  (100*inMilliAmpSecond*duration)/((PowerXML.getStateOfHealth()/100) * (batteryChargeStamp/100) * (PowerXML.getBatteryCapacity()*3600));

        return  (100*inMilliAmpSecond*duration)/((PowerXML.getStateOfHealth()/100) * (PowerXML.getBatteryCapacity()*3600));

    }

//    private void fillTheTable(Map<TwoStringKey,Integer> inputNumberOfRunningEachMethodMap) {
//        // Iterate over each entry in the map and print the key and value
//        for (Map.Entry<TwoStringKey, Integer> entry : inputNumberOfRunningEachMethodMap.entrySet()) {
//            TwoStringKey key = entry.getKey();
//            Integer value = entry.getValue();
//            System.out.println("Key: (" + key.getPart1() + ", " + key.getPart2() + "), " + "Value: " + value);
//        }
//
//    }
}
