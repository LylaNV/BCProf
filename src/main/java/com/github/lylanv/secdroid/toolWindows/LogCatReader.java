package com.github.lylanv.secdroid.toolWindows;

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
import com.github.lylanv.secdroid.inspections.Singleton;
import com.google.common.eventbus.Subscribe;

public class LogCatReader implements Runnable {

    private final String TAG = "GreenMeter"; // Logging tag -> will be used to filter the logcat file
    private volatile boolean running = true; // Volatile to ensure visibility across threads
    Map<String, Integer> redAPIsCount = new HashMap<>(); // Holds the name of APIs and their counts

    public static int energyConsumption = 0;
    public static int batteryLevel = 0;

    private Timer timer;


    public LogCatReader() {
        this.timer = new Timer();
        updateLineGraph(); // I called it here to have it as an ongoing continuous graph
    }

    @Subscribe
    public void handleBuildSuccessEvent(BuildSuccessEvent event) {
        if (event.getBuildStatus()){
            System.out.println("[LogCatReader -> handleBuildSuccessEvent$ Build successful");
        }
    }

    @Subscribe
    public void handleApplicationStoppedEvent(ApplicationStoppedEvent event) throws IOException {
        if (event.getApplicationStopped()){
            System.out.println("[LogCatReader -> handleBuildSuccessEvent$ Application stopped");

            stop();
        }
    }

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
            }


            //Gets the logcat file to be able to process it
            Process logcatProcess = AdbUtils.getLogCatFile();

            //Starts analyzing the LogCat file if it is not null
            if (logcatProcess != null) {
                System.out.println("[GreenEdge -> LogCatReader$ Reading the LogCat file...");

                BufferedReader logcatReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));

                String line;
                while ((line = logcatReader.readLine()) != null && running) {
                    //Filters the lines of the LogCat file with our considered TAG which is GreenMeter
                    if (line.contains(TAG)) {
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

                System.out.println("[GreenEdge -> LogCatReader$ Reading the LogCat file has been finished.");
                System.out.println("[GreenEdge -> LogCatReader$ The LogCatReader is closed and destroyed!");

            }else {
                System.out.println("[LogCatReader -> LogCatReader$ Failed to get emulator logcat file.");
                logcatProcess.destroy();
            }

        }catch (Exception e) {
            System.out.println("[LogCatReader -> LogCatReader$ Failed to analyze the logcat file in Run() method.");
            e.printStackTrace();
        }
    }

    //Gets API/method call name from the logged line
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

    //Gets API/method call name from the logged line
    private String getInfoFromLine(String line) {
        // finds the index of the open parentheses signe ("(") in the logged line
        int openParentheses = line.indexOf('(');
        if (openParentheses < 0) {
            return null;
        }

        // finds the index of the close parentheses signe ("(") in the logged line
        int closeParentheses = line.indexOf(')');
        if (closeParentheses < 0) {
            return null;
        }

        // returns the first argument in the parentheses
        return line.substring(openParentheses + 1, closeParentheses).trim();

    }

    //Cancel timer and clear the variables, LogCat file, and all components in the tool window
    public void stop() throws IOException {
        running = false;
        AdbUtils.clearLogCatFile();

        redAPIsCount.clear();
        energyConsumption = 0;
        batteryLevel = 0;

        LogcatAnalyzerToolWindowFactory.clearAllComponents();
        timer.cancel();
    }

    //To update the line graph periodically even without new energy consumptions value
    public void updateLineGraph() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (batteryLevel > 0 ) {
                    LogcatAnalyzerToolWindowFactory.updateLineGraph(batteryLevel);
                }
            }
        }
        ,0,1000);  // Updated every 1 second
    }
}
