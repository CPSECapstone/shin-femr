package femr.common.models;

public class CSVImportRowItem {
    private long recordNumber;
    private CSVImportCellItem medicationCellItem;
    private CSVImportCellItem quantityCellItem;
    private String rowSubmitError;

    public CSVImportRowItem() {
        this.recordNumber = -1;
        this.medicationCellItem = new CSVImportCellItem();
        this.quantityCellItem = new CSVImportCellItem();
        this.rowSubmitError = null;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public long getRecordNumber() {
        return this.recordNumber;
    }

    public void setMedicationCellItem(CSVImportCellItem medicationCellItem) {
        // Null-check guard
        if (medicationCellItem == null) {
            return;
        }
        this.medicationCellItem = medicationCellItem;
    }

    public CSVImportCellItem getMedicationCellItem() {
        return this.medicationCellItem;
    }

    public void setQuantityCellItem(CSVImportCellItem quantityCellItem) {
        // Null-check guard
        if (quantityCellItem == null) {
            return;
        }
        this.quantityCellItem = quantityCellItem;
    }

    public CSVImportCellItem getQuantityCellItem() {
        return this.quantityCellItem;
    }

    public void setRowSubmitError(String rowSubmitError) {
        this.rowSubmitError = rowSubmitError;
    }

    public String getRowSubmitError() {
        return this.rowSubmitError;
    }
}