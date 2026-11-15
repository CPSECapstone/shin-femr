package femr.common.models;

import java.util.ArrayList;

public class CSVImportItem {
    private long numberOfRowsAdded;
    private ArrayList<CSVImportRowItem> csvImportRowItems;
    private ArrayList<String> csvFormatErrors;

    public CSVImportItem() {
        this.numberOfRowsAdded = 0;
        this.csvImportRowItems = new ArrayList<>();
        this.csvFormatErrors = new ArrayList<>();
    }

    public void setNumberOfRowsAdded(long numberOfRowsAdded) {
        this.numberOfRowsAdded = numberOfRowsAdded;
    }

    public long getNumberOfRowsAdded() {
        return this.numberOfRowsAdded;
    }

    public void setCSVImportRowItems(ArrayList<CSVImportRowItem> csvImportRowItems) {
        // Null-check guard
        if (csvImportRowItems == null) {
            return;
        }
        this.csvImportRowItems = csvImportRowItems;
    }

    public ArrayList<CSVImportRowItem> getCSVImportRowItems() {
        return this.csvImportRowItems;
    }

    public void setCSVFormatErrors(ArrayList<String> csvFormatErrors) {
        // Null-check guard
        if (csvFormatErrors == null) {
            return;
        }
        this.csvFormatErrors = csvFormatErrors;
    }

    public ArrayList<String> getCSVFormatErrors() {
        return this.csvFormatErrors;
    }
}
