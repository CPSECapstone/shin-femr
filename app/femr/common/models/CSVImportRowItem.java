package femr.common.models;

public class CSVImportRowItem {
    private long recordNumber;
    private CSVImportCellItem medicationCellItem;
    private CSVImportCellItem quantityCellItem;

    public CSVImportRowItem() {
        this.recordNumber = -1;
        this.medicationCellItem = null;
        this.quantityCellItem = null;
    }

    public void setRecordNumber(long recordNumber) {
        this.recordNumber = recordNumber;
    }

    public long getRecordNumber() {
        return this.recordNumber;
    }

    public void setMedicationCellItem(CSVImportCellItem medicationCellItem) {
        this.medicationCellItem = medicationCellItem;
    }

    public CSVImportCellItem getMedicationCellItem() {
        return this.medicationCellItem;
    }

    public void setQuantityCellItem(CSVImportCellItem quantityCellItem) {
        this.quantityCellItem = quantityCellItem;
    }

    public CSVImportCellItem getQuantityCellItem() {
        return this.quantityCellItem;
    }
}