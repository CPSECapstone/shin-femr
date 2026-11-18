package femr.common.models;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** A completed CSV import attempt; no CSV contents are stored. */
public class InventoryImportHistoryItem {
    private final String uploadedBy, filename;
    private final Timestamp uploadedAt;
    private final boolean successful;
    private final Integer added, updated;
    public InventoryImportHistoryItem(String uploadedBy, String filename, Timestamp uploadedAt,
                                      boolean successful, Integer added, Integer updated) {
        this.uploadedBy = uploadedBy; this.filename = filename; this.uploadedAt = uploadedAt;
        this.successful = successful; this.added = added; this.updated = updated;
    }
    public String getUploadedBy() { return uploadedBy; }
    public String getFilename() { return filename; }
    public String getUploadedAt() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
                .withZone(ZoneOffset.UTC).format(uploadedAt.toInstant());
    }
    public boolean getSuccessful() { return successful; }
    public String getOutcome() {
        if (successful) return "Successful";
        if ((added != null && added > 0) || (updated != null && updated > 0)) return "Partially successful";
        return "Failed";
    }
    public Integer getAdded() { return added; }
    public Integer getUpdated() { return updated; }
}
