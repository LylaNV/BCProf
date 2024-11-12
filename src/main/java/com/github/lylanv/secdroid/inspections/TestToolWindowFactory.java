package com.github.lylanv.secdroid.inspections;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class TestToolWindowFactory implements ToolWindowFactory {

    private static JTextArea textArea;
    private static JTable table;
    private static DefaultCategoryDataset barChartDataset;
//    private static XYSeries lineGraphSeries;
    private static TimeSeries lineGraphSeries;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        JTabbedPane tabbedPane = new JTabbedPane();


        // Adding components in separate tabs
        tabbedPane.addTab("Text", createTextComponent());
        tabbedPane.addTab("Table", createTableComponent());
        tabbedPane.addTab("Bar Chart", createBarChartComponent());
        tabbedPane.addTab("Line Graph", createLineGraphComponent());

        toolWindow.getContentManager().addContent(ContentFactory.getInstance().createContent(tabbedPane, "Example Tool Window", false));
    }

    private JComponent createTextComponent() {
        textArea = new JTextArea("This is an example text component inside the tool window.");
        textArea.setLineWrap(true);

        JScrollPane textScrollPane = new JBScrollPane(textArea);

        return textScrollPane;
    }

    private JComponent createTableComponent() {
        String[] columns = {"No.", "Method Name", "Energy Usage"};
        String[][] data = {
                {"Row1-Col1", "Row1-Col2", "Row1-Col3"},
                {"Row2-Col1", "Row2-Col2", "Row2-Col3"},
                {"Row3-Col1", "Row3-Col2", "Row3-Col3"},
        };

        table = new JTable(data, columns);

        JScrollPane tableScrollPane = new JBScrollPane(table);

        return tableScrollPane;
    }

    private JComponent createBarChartComponent() {
        // Assuming you have added JFreeChart as a dependency
        barChartDataset = new DefaultCategoryDataset();
//        barChartDataset.addValue(5, "Category 1", "Value A");
//        barChartDataset.addValue(3, "Category 1", "Value B");
//        barChartDataset.addValue(7, "Category 1", "Value C");

        JFreeChart chart = ChartFactory.createBarChart("Bar Chart Example", "Category", "Value", barChartDataset, PlotOrientation.VERTICAL, false, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        return chartPanel;
    }

    private JComponent createLineGraphComponent() {
//        // Assuming you have added JFreeChart as a dependency
//        lineGraphSeries = new XYSeries("Random Data");
//        lineGraphSeries.add(1, 5);
//        lineGraphSeries.add(2, 7);
//        lineGraphSeries.add(3, 6);
//        lineGraphSeries.add(4, 8);
//
//        XYSeriesCollection dataset = new XYSeriesCollection(lineGraphSeries);
//        JFreeChart chart = ChartFactory.createXYLineChart("Line Graph Example", "X", "Y", dataset, PlotOrientation.VERTICAL, false, true, false);
//        ChartPanel chartPanel = new ChartPanel(chart);
//        chartPanel.setPreferredSize(new Dimension(400, 300));
//        return chartPanel;

        // Create a time series for the line graph
        lineGraphSeries = new TimeSeries("Dynamic Data");

        // Use TimeSeriesCollection as the dataset for JFreeChart's time series chart
        TimeSeriesCollection dataset = new TimeSeriesCollection(lineGraphSeries);

        // Create the line chart with time on the X-axis and data on the Y-axis
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Time Series Line Graph", "Time", "Value", dataset, false, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));

        return chartPanel;

    }


    // Methods to update each component
    public static void updateText(String newText) {
        //textArea.setText(newText);
        textArea.append("\n");
        textArea.append(newText);
    }

    public static void updateTable(String[][] newData) {
        table.setModel(new javax.swing.table.DefaultTableModel(newData, new String[]{"Column 1", "Column 2", "Column 3"}));
    }

//    public static void updateBarChart(Map<String, Integer> logLevelCount) {
//        barChartDataset = new DefaultCategoryDataset();
//        for (Map.Entry<String, Integer> entry : logLevelCount.entrySet()) {
//            barChartDataset.addValue(entry.getValue(), entry.getKey(), "");
//        }
//    }

    public static void updateBarChart(Number value, String rowKey, String columnKey) {
        barChartDataset.setValue(value, rowKey, columnKey);
    }

//    public static void updateLineGraph(Number x, Number y) {
//        lineGraphSeries.add(x, y);
//    }

    // Method to update only the vertical axis value on the line graph
    public static void updateLineGraph(Number yValue) {
        lineGraphSeries.addOrUpdate(new Second(), yValue.doubleValue());  // X-axis is time; only Y value changes
    }

    public static void clearAllComponents(){
        //Zero the local variables

        textArea.setText("");
        //table.removeAll();
        // Clear table (set an empty model)
        //TODO: check the table clearance!
        table.setModel(new javax.swing.table.DefaultTableModel(new Object[][]{}, new String[]{"Column 1", "Column 2", "Column 3"}));
        barChartDataset.clear();
        lineGraphSeries.clear();
    }
}
