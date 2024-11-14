package com.github.lylanv.secdroid.toolWindows;

import javax.swing.table.DefaultTableModel;

public class CustomTableModel extends DefaultTableModel {
    public CustomTableModel(Object[] columnNames) {
        super(columnNames, 0);  // Initialize with column names and 0 rows
    }

    // Method to add or update a row based on composite key (first two columns)
    public void addOrUpdateRow(Object key1, Object key2, Object[] rowData) {
        int rowIndex = findRowByKeys(key1, key2);

        if (rowIndex == -1) {
            // Row with the key doesn't exist, so add a new row
            addRow(rowData);
        } else {
            // Row with the key exists, so update the row
            for (int i = 0; i < rowData.length; i++) {
                setValueAt(rowData[i], rowIndex, i);
            }
        }
    }

    // Helper method to find a row by composite key (first two columns)
    private int findRowByKeys(Object key1, Object key2) {
        for (int i = 0; i < getRowCount(); i++) {
            if (getValueAt(i, 0).equals(key1) && getValueAt(i, 1).equals(key2)) {
                return i;
            }
        }
        return -1;  // Row with the given keys not found
    }

    // Optional: Clear the table by removing all rows
    public void clear() {
        setRowCount(0);
    }
}
