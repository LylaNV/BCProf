package com.github.lylanv.secdroid.toolWindows;

import com.github.lylanv.secdroid.inspections.EventBusManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.ContentFactory;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;

public class LogcatAnalyzerToolWindowFactory implements ToolWindowFactory {

    private Thread logcatAnalyzerThread;
    private LogCatReader logcatReader;


    private static JTextArea textArea;
    private static JTable table;
    private static CustomTableModel tableModel;
    private static DefaultCategoryDataset barChartDataset;
    private static TimeSeries lineGraphSeries;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JTabbedPane tabbedPane = new JTabbedPane();


        // Adding components in separate tabs
        tabbedPane.addTab("Text", createTextComponent());
        tabbedPane.addTab("Table", createTableComponent());
        tabbedPane.addTab("Bar Chart", createBarChartComponent());
        tabbedPane.addTab("Line Graph", createLineGraphComponent());

        toolWindow.getContentManager().addContent(ContentFactory.getInstance().createContent(tabbedPane, "Energy Consumption Result", false));

        // Create new thread to analyze the logcat file
        logcatReader = new LogCatReader();
        EventBusManager.register(logcatReader);
        logcatAnalyzerThread = new Thread(logcatReader);
        logcatAnalyzerThread.start();

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(ToolWindow window) {
                if (window.getId().equals(toolWindow.getId())) {
                    if (logcatAnalyzerThread == null || !logcatAnalyzerThread.isAlive()) {
                        logcatReader = new LogCatReader();
                        EventBusManager.register(logcatReader);
                        logcatAnalyzerThread = new Thread(logcatReader);
                        logcatAnalyzerThread.start();
                    }
                }
            }
        });
    }

    private JComponent createTextComponent() {
        textArea = new JTextArea("This is an example text component inside the tool window.");
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
        Object[] columnNames = {"Class Name", "Method Name", "Energy Consumption"};

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
                "Red API Calls Count",
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
        lineGraphSeries = new TimeSeries("Dynamic Data");

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
        lineGraphSeries.addOrUpdate(new Second(), yValue.doubleValue());  // X-axis is time; only Y value changes
    }

    public static void clearAllComponents(){

        textArea.setText("");

        //TODO: check the table clearance!
        //table.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"Column 1", "Column 2", "Column 3"}));
        tableModel.clear();

        barChartDataset.clear();
        lineGraphSeries.clear();
    }
}
