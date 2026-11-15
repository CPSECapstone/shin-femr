package femr.common.models;

public class CSVImportItem {
    private int numberOfRowsAdded;
    private CSVImportRowItem[] csvImportRowItems;
    private String[] csvFormatErrors;

    public CSVImportItem() {
        this.numberOfRowsAdded = 0;
        this.csvImportRowItems = null;
        this.csvFormatErrors = null;
    }

    public void setNumberOfRowsAdded(int numberOfRowsAdded) {
        this.numberOfRowsAdded = numberOfRowsAdded;
    }

    public int getNumberOfRowsAdded() {
        return this.numberOfRowsAdded;
    }

    public void setCSVImportRowItems(CSVImportRowItem[] csvImportRowItems) {
        this.csvImportRowItems = csvImportRowItems;
    }

    public CSVImportRowItem[] getCSVImportRowItems() {
        return this.csvImportRowItems;
    }

    public void setCSVFormatErrors(String[] csvFormatErrors) {
        this.csvFormatErrors = csvFormatErrors;
    }
}
