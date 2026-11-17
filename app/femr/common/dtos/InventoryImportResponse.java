package femr.common.dtos;

/** Structured counts alongside the existing user-facing import summary. */
public class InventoryImportResponse extends ServiceResponse<String> {
    private Integer added;
    private Integer updated;
    public Integer getAdded() { return added; }
    public Integer getUpdated() { return updated; }
    public void complete(int added, int updated) {
        this.added = added;
        this.updated = updated;
        setResponseObject("Import successful: " + added + " medication" + (added == 1 ? "" : "s")
                + " added and " + updated + " " + (updated == 1 ? "medication" : "medications") + " updated.");
    }
}
