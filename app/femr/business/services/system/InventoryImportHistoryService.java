package femr.business.services.system;

import com.google.inject.Inject;
import femr.common.dtos.CurrentUser;
import femr.common.dtos.InventoryImportResponse;
import femr.common.dtos.ServiceResponse;
import femr.common.models.InventoryImportHistoryItem;
import play.db.Database;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persistence for import results, kept separate from the changing CSV parser. */
public class InventoryImportHistoryService {
    public static final int PAGE_SIZE = 25;
    private final Database database;
    @Inject public InventoryImportHistoryService(Database database) { this.database = database; }

    public void record(int tripId, CurrentUser user, String filename, Instant uploadedAt,
                       ServiceResponse<String> result) throws SQLException {
        boolean successful = !result.hasErrors();
        InventoryImportResponse counts = result instanceof InventoryImportResponse
                ? (InventoryImportResponse) result : null;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO inventory_import_history (trip_id,user_id,uploaded_by,filename,uploaded_at,"
                     + "successful,medications_added,medications_updated) VALUES (?,?,?,?,?,?,?,?)")) {
            statement.setInt(1, tripId);
            statement.setInt(2, user.getId());
            statement.setString(3, limit(user.getFirstName() + " " + user.getLastName()));
            statement.setString(4, filename(filename));
            statement.setTimestamp(5, Timestamp.from(uploadedAt));
            statement.setBoolean(6, successful);
            statement.setObject(7, successful && counts != null ? counts.getAdded() : null, Types.INTEGER);
            statement.setObject(8, successful && counts != null ? counts.getUpdated() : null, Types.INTEGER);
            statement.executeUpdate();
        }
    }

    public List<InventoryImportHistoryItem> list(int tripId, int page) throws SQLException {
        if (page < 1 || page > 100000) throw new IllegalArgumentException("Invalid history page.");
        List<InventoryImportHistoryItem> items = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT uploaded_by,filename,uploaded_at,successful,medications_added,medications_updated "
                     + "FROM inventory_import_history WHERE trip_id=? ORDER BY id DESC LIMIT ? OFFSET ?")) {
            statement.setInt(1, tripId);
            statement.setInt(2, PAGE_SIZE + 1);
            statement.setInt(3, (page - 1) * PAGE_SIZE);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) items.add(new InventoryImportHistoryItem(
                        rows.getString(1), rows.getString(2), rows.getTimestamp(3), rows.getBoolean(4),
                        (Integer) rows.getObject(5), (Integer) rows.getObject(6)));
            }
        }
        return items;
    }

    private static String filename(String name) {
        if (name == null || name.trim().isEmpty()) return "No file selected";
        name = name.replace('\\', '/');
        return limit(name.substring(name.lastIndexOf('/') + 1));
    }
    private static String limit(String value) { return value.substring(0, Math.min(value.length(), 255)); }
}
