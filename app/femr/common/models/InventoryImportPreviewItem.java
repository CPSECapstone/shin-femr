package femr.common.models;

import java.util.LinkedHashMap;
import java.util.Map;

/** Editable, non-persistent representation of one inventory import row. */
public class InventoryImportPreviewItem {
    // Retaining the parsed MedicationItem makes the preview, rather than the CSV,
    // the source of truth for the eventual upload.
    private final MedicationItem medicationItem;
    private final int rowNumber;
    private final Integer uploadedDelta;
    private final boolean replacesQuantity;
    private final Map<String, String> errors = new LinkedHashMap<>();
    private String medicationName;
    private boolean isNew;
    private Integer currentQuantityBefore;
    private int currentQuantityAfter;
    private Integer initialQuantityBefore;
    private Integer initialQuantityAfter;
    private boolean removed;

    public InventoryImportPreviewItem(String medicationName, boolean isNew,
                                      Integer currentQuantityBefore, int currentQuantityAfter,
                                      Integer initialQuantityBefore, Integer initialQuantityAfter) {
        this(new MedicationItem(), 0, null, false, medicationName, isNew, currentQuantityBefore,
                currentQuantityAfter, initialQuantityBefore, initialQuantityAfter);
        medicationItem.setFullName(medicationName);
    }

    public InventoryImportPreviewItem(MedicationItem medicationItem, int rowNumber, Integer uploadedDelta,
                                      boolean replacesQuantity, String medicationName, boolean isNew,
                                      Integer currentQuantityBefore, int currentQuantityAfter,
                                      Integer initialQuantityBefore, Integer initialQuantityAfter) {
        this.medicationItem = medicationItem;
        this.rowNumber = rowNumber;
        this.uploadedDelta = uploadedDelta;
        this.replacesQuantity = replacesQuantity;
        this.medicationName = medicationName;
        this.isNew = isNew;
        this.currentQuantityBefore = currentQuantityBefore;
        this.currentQuantityAfter = currentQuantityAfter;
        this.initialQuantityBefore = initialQuantityBefore;
        this.initialQuantityAfter = initialQuantityAfter;
    }

    public MedicationItem getMedicationItem() { return medicationItem; }
    public int getRowNumber() { return rowNumber; }
    public Integer getUploadedDelta() { return uploadedDelta; }
    public boolean getReplacesQuantity() { return replacesQuantity; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String value) { medicationName = value == null ? "" : value.trim(); medicationItem.setFullName(medicationName); }
    public boolean getIsNew() { return isNew; }
    public void setIsNew(boolean value) { isNew = value; }
    public Integer getCurrentQuantityBefore() { return currentQuantityBefore; }
    public void setCurrentQuantityBefore(Integer value) { currentQuantityBefore = value; }
    public int getCurrentQuantityAfter() { return currentQuantityAfter; }
    public void setCurrentQuantityAfter(int value) { currentQuantityAfter = value; medicationItem.setQuantityCurrent(value); }
    public Integer getInitialQuantityBefore() { return initialQuantityBefore; }
    public void setInitialQuantityBefore(Integer value) { initialQuantityBefore = value; }
    public Integer getInitialQuantityAfter() { return initialQuantityAfter; }
    public void setInitialQuantityAfter(Integer value) { initialQuantityAfter = value; medicationItem.setQuantityTotal(value); }
    public boolean getRemoved() { return removed; }
    public void setRemoved(boolean value) { removed = value; }
    public Map<String, String> getErrors() { return errors; }
    public void clearErrors() { errors.clear(); }
    public void addError(String field, String message) { errors.put(field, message); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public boolean getCurrentQuantityChanged() {
        return currentQuantityBefore == null || currentQuantityBefore != currentQuantityAfter;
    }

    public boolean getInitialQuantityChanged() {
        return initialQuantityAfter != null &&
                (initialQuantityBefore == null || !initialQuantityBefore.equals(initialQuantityAfter));
    }
}
