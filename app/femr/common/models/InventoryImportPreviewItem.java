package femr.common.models;

/** A single, non-persistent change found while reviewing an inventory CSV upload. */
public class InventoryImportPreviewItem {
    private final String medicationName;
    private final boolean isNew;
    private final Integer currentQuantityBefore;
    private final int currentQuantityAfter;
    private final Integer initialQuantityBefore;
    private final Integer initialQuantityAfter;

    public InventoryImportPreviewItem(String medicationName, boolean isNew,
                                      Integer currentQuantityBefore, int currentQuantityAfter,
                                      Integer initialQuantityBefore, Integer initialQuantityAfter) {
        this.medicationName = medicationName;
        this.isNew = isNew;
        this.currentQuantityBefore = currentQuantityBefore;
        this.currentQuantityAfter = currentQuantityAfter;
        this.initialQuantityBefore = initialQuantityBefore;
        this.initialQuantityAfter = initialQuantityAfter;
    }

    public String getMedicationName() { return medicationName; }
    public boolean getIsNew() { return isNew; }
    public Integer getCurrentQuantityBefore() { return currentQuantityBefore; }
    public int getCurrentQuantityAfter() { return currentQuantityAfter; }
    public Integer getInitialQuantityBefore() { return initialQuantityBefore; }
    public Integer getInitialQuantityAfter() { return initialQuantityAfter; }

    public boolean getCurrentQuantityChanged() {
        return currentQuantityBefore == null || currentQuantityBefore != currentQuantityAfter;
    }

    public boolean getInitialQuantityChanged() {
        return initialQuantityAfter != null &&
                (initialQuantityBefore == null || !initialQuantityBefore.equals(initialQuantityAfter));
    }
}
