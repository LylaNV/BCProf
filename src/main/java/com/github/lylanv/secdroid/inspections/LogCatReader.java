package com.github.lylanv.secdroid.inspections;

import com.github.lylanv.secdroid.events.ApplicationStoppedEvent;
import com.github.lylanv.secdroid.events.BuildSuccessEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.android.ddmlib.logcat.LogCatListener;
import com.android.ddmlib.logcat.LogCatMessage;
import com.google.common.eventbus.Subscribe;
import org.jfree.data.category.DefaultCategoryDataset;

public class LogCatReader implements Runnable {
    private LogcatAnalyzerToolWindow toolWindow;
    private final String TAG;
    private volatile boolean running = true; // Volatile to ensure visibility across threads
    Map<String, Integer> redAPIsCount = new HashMap<>(); // Holds the name of APIs and their counts
    private DefaultCategoryDataset dataset;

    public static int energyConsumption = 0;
    public static int batteryLevel = 0;

    private boolean buildSucceeded = false;

    //NEW
    private final Timer timer;


    public LogCatReader(LogcatAnalyzerToolWindow toolWindow, String TAG) {
        this.toolWindow = toolWindow;
        this.TAG = TAG;

        //NEW
        this.timer = new Timer();
        updateLineGraph(); // I called it here to have it as an ongoing continuous graph
        AdbUtils.clearLogCatFile(); // I added to clear the previous tags.
    }

    @Subscribe
    public void handleBuildSuccessEvent(BuildSuccessEvent event) {
        if (event.getBuildStatus()){
            buildSucceeded = true;
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
            //toolWindow.drawLineGraph();
            //Process batteryPercentageProcess = Runtime.getRuntime().exec("adb emu power display");
            Boolean adbServerIsRunning = AdbUtils.isAdbAvailable();
            if (!adbServerIsRunning){
                AdbUtils.startAdb();
            }

//            Process batteryPercentageProcess = AdbUtils.getEmulatorBatteryLevel();
//            if (batteryPercentageProcess != null) {
//                BufferedReader reader = new BufferedReader(new InputStreamReader(batteryPercentageProcess.getInputStream()));
//                String lineBatteryPercentage;
//                while ((lineBatteryPercentage = reader.readLine()) != null) {
//                    if (lineBatteryPercentage.contains("capacity")) {
//                        //toolWindow.appendLog(lineBatteryPercentage);
//                        batteryLevel = Integer.parseInt(lineBatteryPercentage.split(" ")[1]);
//                        toolWindow.appendLog("Battery capacity is: " + batteryLevel);
//                    }
//                }
//                reader.close();
//                batteryPercentageProcess.destroy();
//            } else {
//                System.out.println("[LogCatReader -> run$ Failed to get emulator battery level");
//                batteryPercentageProcess.destroy();
//            }


            //Process logcatCleaningProcess = Runtime.getRuntime().exec("adb logcat -c");
            AdbUtils.clearLogCatFile();
            //Process logcatProcess = Runtime.getRuntime().exec("adb logcat");
            Process logcatProcess = AdbUtils.getLogCatFile();
            if (logcatProcess != null) {
                System.out.println("[GreenEdge -> LogCatReader$ Logcat is not null. run LogCatReader");
                BufferedReader logcatReader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));

                String line;
                while ((line = logcatReader.readLine()) != null && running) {
                    if (line.contains(TAG)) {
                        //System.out.println("[GreenEdge -> LogCatReader$ Logcat contains our tag! Filtering the line with the tag and append to toolwindow.");
                        toolWindow.appendLog(line);

                        String extractedAPICallName = getAPICallName(line);
                        redAPIsCount.put(extractedAPICallName, redAPIsCount.getOrDefault(extractedAPICallName, 0) + 1);
                        energyConsumption ++;
                        updateGraph(redAPIsCount);

                        //NEW
                        TestToolWindowFactory.updateText(line);
                        TestToolWindowFactory.updateLineGraph(energyConsumption);
                        TestToolWindowFactory.updateBarChart(redAPIsCount.getOrDefault(extractedAPICallName,1),extractedAPICallName,extractedAPICallName);
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
                //stop();

                System.out.println("[GreenEdge -> LogCatReader$ LogCatReader is closed and destroyed!");

            }else {
                System.out.println("[LogCatReader -> run$ Failed to get emulator logcat file");
                logcatProcess.destroy();
            }


        }catch (Exception e) {
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

    public void stop() throws IOException {
        running = false;
//        Runtime runtime = Runtime.getRuntime();
//        runtime.exec("adb logcat -c");
        AdbUtils.clearLogCatFile();

        if (dataset !=null){
            dataset.clear();
        }
        redAPIsCount.clear();
        energyConsumption = 0;

        //NEW
        TestToolWindowFactory.clearAllComponents();
        timer.cancel();
        //These four should be here even if the others are deleted
//        running = false;
//        AdbUtils.clearLogCatFile();
//        redAPIsCount.clear();
//        energyConsumption = 0;

    }

    // Updates the bar graph
    private void updateGraph(Map<String, Integer> logLevelCount) {
        dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, Integer> entry : logLevelCount.entrySet()) {
            dataset.addValue(entry.getValue(), entry.getKey(), "");
        }

        toolWindow.updateGraph(dataset);
    }


    //NEW
    //To update the line graph even without new energy consumptions
    public void updateLineGraph() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                TestToolWindowFactory.updateLineGraph(energyConsumption);
                }
            }
        ,0,1000);  // Updated every 1 second
    }
}
