package com.github.lylanv.secdroid.toolWindows;

import com.android.tools.r8.L;
import com.android.tools.r8.M;
import com.android.tools.r8.internal.Sy;
import com.github.lylanv.secdroid.events.ApplicationStartedEvent;
import com.github.lylanv.secdroid.events.ApplicationStoppedEvent;
import com.github.lylanv.secdroid.events.BuildSuccessEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.github.lylanv.secdroid.inspections.AdbUtils;
import com.github.lylanv.secdroid.inspections.DroidEC;
import com.github.lylanv.secdroid.inspections.EventBusManager;
import com.github.lylanv.secdroid.inspections.Singleton;
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

    public static int energyConsumption = 0;
    public static int batteryLevel = 0;

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


    String applicationPackageName;

    private boolean isOutputReady = false;


    public LogCatReader() {
        this.timer = new Timer();
        applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();

        updateLineGraph(); // I called it here to have it as an ongoing continuous graph
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

//    private void analyzeLogCatForMethodStatistics()  {
//        Process finalLogcatProcess = AdbUtils.getLogCatFile();
//        if (finalLogcatProcess != null) {
//            System.out.println("[LogCatReader -> analyzeLogCatForMethodStatistics$ Start reading the logcat file...");
//
//            try {
//                Map<TwoStringKey, Integer> numberOfRunningEachMethodMap = new HashMap<>();
//
//                BufferedReader logcatReader = new BufferedReader(new InputStreamReader(finalLogcatProcess.getInputStream()));
//
//                String line;
//                while ((line = logcatReader.readLine()) != null) {
//                    if (line.contains(TAG)) {
//                        //Extract elements
//                        String firstElement = getFirstElementOfLogStatement(line);
//                        String secondElement = getSecondElementOfLogStatement(line);
//                        String thirdElement = getThirdElementOfLogStatement(line);
//
//                        TwoStringKey key = new TwoStringKey(secondElement, firstElement);
//
//                        if (thirdElement != null) {
//                            if (thirdElement.contains(MethodStartTAG)) {
//                                if (numberOfRunningEachMethodMap.isEmpty()) {
//                                    numberOfRunningEachMethodMap.put(key, 1);
//                                } else if (numberOfRunningEachMethodMap.containsKey(key)) {
//                                    int oldCount = numberOfRunningEachMethodMap.get(key);
//                                    numberOfRunningEachMethodMap.put(key, oldCount + 1);
//                                }else {
//                                    numberOfRunningEachMethodMap.put(key,1);
//                                }
//                            }
//
//                        }else {
//                            System.out.println("[LogCatReader -> analyzeLogCatForMethodStatistics$ FATAL ERROR: Splitting the log statement does not work correctly!");
//                        }
//
//                    } else {
//                        // TODO: I got null pointer- Check it
//                        if (DroidEC.projectName != null){
//                            if (line.contains(DroidEC.projectName)) {
//                                System.out.println(line);
//                            }
//                        }
//                    }
//
//                }
//
//                fillTheTable(numberOfRunningEachMethodMap);
//
//            }catch (IOException exception){
//                System.out.println("[LogCatReader -> analyzeLogCatForMethodStatistics$ Failed to read the logcat file.");
//                exception.printStackTrace();
//            }
//
//        }else{
//            System.out.println("[LogCatReader -> analyzeLogCatForMethodStatistics$ FATAL ERROR: The logcat file not found. Cannot update the table!");
//        }
//    }


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
                //Wait: Stay in the loop
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
                    monitoringInitialStatusOfNetwork();

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

                Map<TwoStringKey, Integer> numberOfRunningEachMethodMap = new HashMap<>();

                BufferedReader logcatReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));

                String line;
                while ((line = logcatReader.readLine()) != null && running && updatingFlag) {
                    //Filters the lines of the LogCat file with our considered TAG which is GreenMeter
                    if (line.contains(TAG)) {

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
                            energyConsumption = batteryLevel - Singleton.redAPICalls.get(extractedAPICallName);
                            batteryLevel = energyConsumption;

                            if (batteryLevel >= 0){
                                LogcatAnalyzerToolWindowFactory.updateText(extractedInfo); //Update text area
                                LogcatAnalyzerToolWindowFactory.updateLineGraph(energyConsumption); //Update the line graph
                                LogcatAnalyzerToolWindowFactory.updateBarChart(redAPIsCount.getOrDefault(extractedAPICallName,1),extractedAPICallName,extractedAPICallName); //Update the bar chart
                            } else {
                                AdbUtils.stopEmulator();
                            }

                        }else{
                            //Log statement is for Start or End of a method
                            if(getThirdElementOfLogStatement(line).contains(MethodStartTAG)){
                                //Extract elements
                                String firstElement = getFirstElementOfLogStatement(line);
                                String secondElement = getSecondElementOfLogStatement(line);


                                TwoStringKey key = new TwoStringKey(secondElement, firstElement);
                                Double energy = Singleton.methodsAPICallsTotalEnergyCostMap.get(key);

                                Object key1 = secondElement;
                                Object key2 = firstElement;

                                if (currentMethodEnergyUsageMap.isEmpty()) {
                                    currentMethodEnergyUsageMap.put(key, energy);
                                    Object[] rowData = {key1, key2, energy};
                                    LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);

                                } else if (currentMethodEnergyUsageMap.containsKey(key)) {
                                    Double oldEnergy = currentMethodEnergyUsageMap.get(key);
                                    Double newEnergy = oldEnergy + energy;
                                    currentMethodEnergyUsageMap.put(key, newEnergy);
                                    Object[] rowData = {key1, key2, newEnergy};
                                    LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);
                                }else {
                                    currentMethodEnergyUsageMap.put(key, energy);
                                    Object[] rowData = {key1, key2, energy};
                                    LogcatAnalyzerToolWindowFactory.addOrUpdateTableRow(key1,key2,rowData);
                                }

//                                if (numberOfRunningEachMethodMap.isEmpty()) {
//                                    numberOfRunningEachMethodMap.put(key, 1);
//                                } else if (numberOfRunningEachMethodMap.containsKey(key)) {
//                                    int oldCount = numberOfRunningEachMethodMap.get(key);
//                                    numberOfRunningEachMethodMap.put(key, oldCount + 1);
//                                }else {
//                                    numberOfRunningEachMethodMap.put(key,1);
//                                }
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

        energyConsumption = 0;
        batteryLevel = 0;

        timer.cancel();

        EventBusManager.unregister(this);

    }


    /*
     * ********************************************************************************************************
     *                                       HW COMPONENTS
     * ********************************************************************************************************
     * */

    private void monitoringInitialStatusOfNetwork() {

        //check network
        networkInitialUsageMap = checkNetwork(applicationPackageName);

    }

    private void monitorInitialStatusOfHW(String applicationPackageName) {
        networkInitialUsageMap = checkNetwork(applicationPackageName);
        gpsStatus_initial = AdbUtils.isUsingGPS(applicationPackageName);
        displayStatus_initial = AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName);
        cameraStatus_initial = AdbUtils.isCameraOn();

        //TEST
        System.out.println("[GreenEdge -> LogCatReader$ gpsStatus_initial " + gpsStatus_initial);
        System.out.println("[GreenEdge -> LogCatReader$ displayStatus_initial " + displayStatus_initial);
        System.out.println("[GreenEdge -> LogCatReader$ cameraStatus_initial" + cameraStatus_initial);
    }

    private void monitorCurrentStatusOfHW(String applicationPackageName) {
        networkCurrentUsageMap = checkNetwork(applicationPackageName);
        gpsStatus_current = AdbUtils.isUsingGPS(applicationPackageName);
        displayStatus_current = AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName);
        cameraStatus_current = AdbUtils.isCameraOn();

        //TEST
        System.out.println("[GreenEdge -> LogCatReader$ gpsStatus_current " + gpsStatus_current);
        System.out.println("[GreenEdge -> LogCatReader$ displayStatus_current " + displayStatus_current);
        System.out.println("[GreenEdge -> LogCatReader$ cameraStatus_current" + cameraStatus_current);
    }



    private Map<String, Integer[]> checkNetwork(String applicationPackageName) {

        String connectedInterface =  AdbUtils.isNetworkConnected();
        if (connectedInterface != null) {
            if (!connectedInterface.contains("Idle timers")) {
                boolean networkConnectionEstablished = AdbUtils.isNetworkConnectionEstablished();

                if (networkConnectionEstablished) {
                    //System.out.println("[LogCatReader -> checkNetwork$ There is an active/established network connection.");
                    if (AdbUtils.isPackageUsingNetwork(applicationPackageName)) {
                        //System.out.println("[LogCatReader -> checkNetwork$ There is an active/established network connection for this application.");
                        if (AdbUtils.numberOfPackets() != null) {
                            //System.out.println("[ALogCatReader -> checkNetwork$ Returning number of packets.");
                            return AdbUtils.numberOfPackets();
                        }else {
                            System.out.println("[LogCatReader -> checkNetwork$ Problem in extracting number of packets for this application.");
                        }
                    }

                }

            }

        }

        return null;
    }


    /*
     * ********************************************************************************************************
     *                                       TOOL WINDOW UPDATE
     * ********************************************************************************************************
     * */

    //To update the line graph periodically even without new energy consumptions value
    public void updateLineGraph() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (running && updatingFlag) {

                    //If application package name is still null try to get it
                    if(applicationPackageName == null){
                        applicationPackageName = LogcatAnalyzerToolWindowFactory.getPackageName();
                    }


                    if (batteryLevel > 0 ) {

                        if (AdbUtils.isCameraOn()){
                            //TODO calculate the battery consumption
                            batteryLevel--;
                            System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ CAMERA -> batteryLevel " + batteryLevel);
                        }


                        if (AdbUtils.isAppCurrentFocusOFScreen(applicationPackageName)){
                            batteryLevel--;
                            System.out.println("[GreenEdge -> LogCatReader -> updateLineGraph$ Screen -> batteryLevel " + batteryLevel);
                        }

                        if (AdbUtils.isUsingGPS(applicationPackageName)){
                            batteryLevel--;
                        }


                        //HW -Network
                        networkCurrentUsageMap = checkNetwork(applicationPackageName);
                        //current and initial network info available -> check if updating energy is needed
                        if (networkCurrentUsageMap !=null && networkInitialUsageMap != null) {
                            for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentUsageMap.entrySet()) {
                                for (Map.Entry<String,Integer[]> entryInitial: networkInitialUsageMap.entrySet()){
                                    if (entryCurrent.getKey() == entryInitial.getKey()) {
                                        if (entryCurrent.getValue()[0] - entryInitial.getValue()[0] > 0 && entryCurrent.getValue()[1] - entryInitial.getValue()[1] > 0) {
                                            //TODO: calculation of battery consumption
                                            //Application is using network calculate based on the power
                                            //wlan0 -> WiFi
                                            //eth0 -> cellular data
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
                        } else if (networkCurrentUsageMap !=null && networkInitialUsageMap == null) {
                            //current info available and initial is null -> network is connected and established recently -> update the initial map -> no need to update energy
                            for (Map.Entry<String,Integer[]> entryCurrent: networkCurrentUsageMap.entrySet()) {
                                networkInitialUsageMap.put(entryCurrent.getKey(), entryCurrent.getValue());
                            }
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
