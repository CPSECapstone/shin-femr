package femr.common.models;

public class CSVSubmitEditItem {
    String medication;
    String quantity;

    public CSVSubmitEditItem() {
        this.medication = null;
        this.quantity = null;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getMedication() {
        return this.medication;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getQuantity() {
        return this.quantity;
    }
}
