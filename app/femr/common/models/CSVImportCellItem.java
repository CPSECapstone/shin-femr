package femr.common.models;

public class CSVImportCellItem {
    private String value;
    private String cellError;

    public CSVImportCellItem() {
        this.value = null;
        this.cellError = null;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setCellError(String cellError) {
        this.cellError = cellError;
    }

    public String getCellError() {
        return this.cellError;
    }
}
