package com.github.lylanv.secdroid.toolWindows;

import com.android.tools.idea.gradle.project.model.GradleAndroidModel;
import com.github.lylanv.secdroid.inspections.EventBusManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;


import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;


public class LogcatAnalyzerToolWindowFactory implements ToolWindowFactory {

    private static Project project;

    private static Thread logcatAnalyzerThread;
    private static LogCatReader logcatReader;

    private static JTextArea textArea;
    private static JTable table;
    private static CustomTableModel tableModel;
    private static DefaultCategoryDataset barChartDataset;
    private static TimeSeries lineGraphSeries;

    private static String resultPathBase;

    private static String packageName;


    public static volatile boolean flagLogcatReaderRunning = false;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        this.project = project;

        resultPathBase = project.getBasePath();

        JTabbedPane tabbedPane = new JTabbedPane();

        // Adding components in separate tabs
        tabbedPane.addTab("Text", createTextComponent());
        tabbedPane.addTab("Table", createTableComponent());
        tabbedPane.addTab("Bar Chart", createBarChartComponent());
        tabbedPane.addTab("Line Graph", createLineGraphComponent());

        toolWindow.getContentManager().addContent(ContentFactory.getInstance().createContent(tabbedPane, "Energy Consumption Result", false));

//        // Create new thread to analyze the logcat file
//        logcatReader = new LogCatReader();
//        EventBusManager.register(logcatReader);
//        logcatAnalyzerThread = new Thread(logcatReader);
//        logcatAnalyzerThread.start();

//        MessageBusConnection connection = project.getMessageBus().connect(project);
//        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
//            @Override
//            public void toolWindowShown(ToolWindow window) {
//                if (window.getId().equals(toolWindow.getId())) {
//                    if (logcatAnalyzerThread == null || !logcatAnalyzerThread.isAlive()) {
//                        logcatReader = new LogCatReader();
//                        EventBusManager.register(logcatReader);
//                        logcatAnalyzerThread = new Thread(logcatReader);
//                        logcatAnalyzerThread.start();
//                    }
//                }
//            }
//        });
    }

//    private static String extractPackageNameFromManifest(String manifestContent) {
//        // Use regex or XML parsing to extract the package name
//        String regex = "package\\s*=\\s*\"([^\"]+)\"";
//        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(manifestContent);
//        if (matcher.find()) {
//            return matcher.group(1); // Return the captured package name
//        }
//        return null;
//    }
//
//    public static String getPackageNameFromMergedManifest(Module module) {
//        try {
//            // Fetch the package name from the merged manifest
//            return MergedManifestManager.getMergedManifest(module).get().getPackage();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null; // Handle cases where the manifest or module is invalid
//        }
//    }

    private JComponent createTextComponent() {
        if (packageName == null) {
            //textArea = new JTextArea("This is an example text component inside the tool window.");
            textArea = new JTextArea("");
        }else{
            textArea = new JTextArea("Package is: " + packageName);
        }

        textArea.setLineWrap(true);

        JScrollPane textScrollPane = new JBScrollPane(textArea);

        return textScrollPane;
    }

    private JComponent createTableComponent() {
//        String[] columns = {"Class Name", "Method Name", "Energy Consumption"};
//        String[][] data = {
//                {"Row1-Col1", "Row1-Col2", "Row1-Col3"},
//                {"Row2-Col1", "Row2-Col2", "Row2-Col3"},
//                {"Row3-Col1", "Row3-Col2", "Row3-Col3"},
//        };
//
//        table = new JTable(data, columns);
//
//        JScrollPane tableScrollPane = new JBScrollPane(table);
//
//        return tableScrollPane;

        // Define column names
        Object[] columnNames = {"Class Name", "Method Name", "API Energy Consumption", "Hardware Energy Consumption"};

        // Initialize the custom table model with column names
        tableModel = new CustomTableModel(columnNames);

        // Initialize the JTable with the custom model
        table = new JTable(tableModel);

//        // Return the table component, wrapped in a scroll pane if needed
//        return new JScrollPane(table);

        JScrollPane tableScrollPane = new JBScrollPane(table);

        return tableScrollPane;
    }

    private JComponent createBarChartComponent() {
        barChartDataset = new DefaultCategoryDataset();

        JFreeChart chart = ChartFactory.createBarChart(
                "API Calls Count",
                "Category",
                "Count",
                barChartDataset, PlotOrientation.VERTICAL,
                true, true, false);
                //false, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }

    private JComponent createLineGraphComponent() {

        // Create a time series for the line graph
        lineGraphSeries = new TimeSeries("Battery Consumption");

        // Use TimeSeriesCollection as the dataset for JFreeChart's time series chart
        TimeSeriesCollection dataset = new TimeSeriesCollection(lineGraphSeries);

        // Create the line chart with time on the X-axis and data on the Y-axis
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Application Battery Consumption",
                "Time",
                "Battery Percentage",
                dataset,
                true, true, false);
                //false, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));

        return chartPanel;

    }



    // Methods to update each component
    public static void updateText(String newText) {
        textArea.append("\n");
        textArea.append(newText);
    }

//    public static void updateTable(String[][] newData) {
//        table.setModel(new javax.swing.table.DefaultTableModel(newData, new String[]{"Column 1", "Column 2", "Column 3"}));
//    }

    // Method to add or update a row in the table
    public static void addOrUpdateTableRow(Object key1, Object key2, Object[] rowData) {
        tableModel.addOrUpdateRow(key1, key2, rowData);
    }

    public static void updateBarChart(Number value, String rowKey, String columnKey) {
        barChartDataset.setValue(value, rowKey, columnKey);
    }

    // Method to update only the vertical axis value on the line graph
    public static void updateLineGraph(Number yValue) {
        //lineGraphSeries.addOrUpdate(new Second(), yValue.doubleValue());  // X-axis is time; only Y value changes
        lineGraphSeries.addOrUpdate(new Second(new java.util.Date(System.currentTimeMillis())), yValue.doubleValue());  // X-axis is time; only Y value changes
    }

    public static void saveResultsToFile(){

        if (resultPathBase != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd.HH.mm.ss");
            LocalDateTime now = LocalDateTime.now();

            String resultPath = resultPathBase + "/SECSDroid_Result" + dtf.format(now) + ".json";

            // Create a map to store all data
            HashMap<String, Object> data = new HashMap<>();

            // Save table data
            ArrayList<Object> tableData = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object[] row = new Object[tableModel.getColumnCount()];
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    row[j] = tableModel.getValueAt(i, j);
                }
                tableData.add(Arrays.toString(row));
            }
            data.put("table", tableData);

            // Save bar chart data
            ArrayList<Object> barChartData = new ArrayList<>();
            for (int i = 0; i < barChartDataset.getRowCount(); i++) {
                for (int j = 0; j < barChartDataset.getColumnCount(); j++) {
                    HashMap<String, Object> entry = new HashMap<>();
                    if (barChartDataset.getRowKey(i) != null && barChartDataset.getColumnKey(j) != null && barChartDataset.getValue(i,j) != null) {
                        entry.put("rowKey", barChartDataset.getRowKey(i).toString());
                        entry.put("columnKey", barChartDataset.getColumnKey(j).toString());
                        entry.put("value", barChartDataset.getValue(i, j));
                        barChartData.add(entry);
                    }

//                    entry.put("rowKey", barChartDataset.getRowKey(i).toString());
//                    entry.put("columnKey", barChartDataset.getColumnKey(j).toString());
//                    entry.put("value", barChartDataset.getValue(i, j));
//                    barChartData.add(entry);
                }
            }
            data.put("barChart", barChartData);

            // Save text area content
            data.put("text", textArea.getText());

            // Save line graph data
            ArrayList<Object> lineGraphData = new ArrayList<>();
            lineGraphSeries.getItems().forEach(item -> {
                HashMap<String, Object> point = new HashMap<>();
                if (item instanceof TimeSeriesDataItem) {
                    TimeSeriesDataItem dataItem = (TimeSeriesDataItem) item;
                    point.put("time",dataItem.getPeriod().getStart());
                    point.put("value",dataItem.getValue());
                    lineGraphData.add(point);
                }else {
                    System.out.println("[GreenMeter -> LogcatAnalyzerToolWindowFactory -> saveResultsToFile$ Can't save the table point to the result file! Unexpected item type: " + item.getClass().getName());
                    //point.put("value",item.toString());
                }
            });
            data.put("lineGraph", lineGraphData);

            // Write data to JSON file
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(resultPath)) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                System.out.println("[GreenMeter -> LogcatAnalyzerToolWindowFactory -> saveResultsToFile$ FATAL ERROR: Can't save results to the file. IOException occurred: !]" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[GreenMeter -> LogcatAnalyzerToolWindowFactory -> saveResultsToFile$ FATAL ERROR: Can't save results to the file. The file path is null!]");
        }

    }

    public static void clearAllComponents(){

        textArea.setText("");

        //table.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"Column 1", "Column 2", "Column 3"}));
        tableModel.clear();

        barChartDataset.clear();
        lineGraphSeries.clear();
    }


    public static String getPackageName(){

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            GradleAndroidModel androidModel = GradleAndroidModel.get(module);
            if (androidModel != null) {
                String applicationId = androidModel.getApplicationId();
                System.out.println("[GreenMeter -> LogcatAnalyzerToolWindowFactory -> getPackageName$ Application ID: " + applicationId);
                packageName = applicationId;
                return packageName;
            }
        }
        return null;
    }

//    /* We have this function to refresh the toolwindow without needing to go to another
//    * toolwindow and then come back to this one if we run and stop application multiple times
//    * */
    public static void refreshToolWindow() {

//        if (logcatAnalyzerThread !=null) {
//            EventBusManager.unregister(logcatReader);
//            logcatAnalyzerThread. interrupt();
//            logcatAnalyzerThread = null;
//            logcatReader = null;
//
//
//            logcatReader.stop();
//        }
//

//        if (!flagLogcatReaderRunning){
//            if (logcatReader != null) {
//                logcatReader = new LogCatReader();
//                EventBusManager.register(logcatReader);
//                logcatAnalyzerThread = new Thread(logcatReader);
//                logcatAnalyzerThread.start();
//
//                flagLogcatReaderRunning = true;
//            }else {
//                EventBusManager.unregister(logcatReader);
//                logcatAnalyzerThread.join();
//
//                logcatReader = new LogCatReader();
//                EventBusManager.register(logcatReader);
//                logcatAnalyzerThread = new Thread(logcatReader);
//                logcatAnalyzerThread.start();
//
//                flagLogcatReaderRunning = true;
//
//
//            }
//        }


        if (logcatAnalyzerThread == null || !logcatAnalyzerThread.isAlive()) {
            logcatReader = new LogCatReader();
            EventBusManager.register(logcatReader);
            logcatAnalyzerThread = new Thread(logcatReader);
            logcatAnalyzerThread.start();
        }

    }


//    //For testing
//    public static void typeChecker(){
//            lineGraphSeries.getItems().forEach(item -> {
//                System.out.println("**********************************************************************************************************************");
//                System.out.println("Unexpected item type: " + item.getClass().getName());
//            });
//    }
}
