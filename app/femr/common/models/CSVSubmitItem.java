package femr.common.models;

import java.util.HashMap;
import java.util.HashSet;

public class CSVSubmitItem {
    private final HashMap<Long, CSVSubmitEditItem> edits;
    private final HashSet<Long> deletions;

    public CSVSubmitItem() {
        edits = new HashMap<>();
        deletions = new HashSet<>();
    }

    public void addToDeletions(long rowRecord) {
        deletions.add(rowRecord);
    }

    public void removeFromDeletions(long rowRecords) {
        deletions.remove(rowRecords);
    }

    public HashSet<Long> getDeletions() {
        return deletions;
    }

    public void addToEdits(long rowRecord, CSVSubmitEditItem csvSubmitEditItem) {
        // Null-check guard
        if (csvSubmitEditItem == null) {
            return;
        }
        edits.put(rowRecord, csvSubmitEditItem);
    }

    public HashMap<Long, CSVSubmitEditItem> getEdits() {
        return this.edits;
    }
}