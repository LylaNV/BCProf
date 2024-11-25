package com.github.lylanv.secdroid.inspections;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BatteryHealthAndCapacityDialog extends DialogWrapper {
    private JTextField batteryCapacityField;
    private JTextField batteryHealthField;

    public BatteryHealthAndCapacityDialog() {
        super(true); // Use current project as parent, true for modal dialog
        init();
        setTitle("Battery Capacity and Health Status");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2));

        JLabel batteryCapacityLabel = new JLabel("Battery Capacity in mAh:");
        batteryCapacityField = new JTextField();
        JLabel batteryHealthLabel = new JLabel("Battery Health in percent:");
        batteryHealthField = new JTextField();

        panel.add(batteryCapacityLabel);
        panel.add(batteryCapacityField);
        panel.add(batteryHealthLabel);
        panel.add(batteryHealthField);

        return panel;
    }

    public String getBatteryCapacity() {
        return batteryCapacityField.getText();
    }

    public String getBatteryHealth() {
        return batteryHealthField.getText();
    }
}
