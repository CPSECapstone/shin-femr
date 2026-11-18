package femr.business.services.system;

import femr.common.dtos.CurrentUser;
import femr.common.dtos.InventoryImportResponse;
import femr.common.dtos.ServiceResponse;
import femr.common.models.InventoryImportHistoryItem;
import org.junit.*;
import play.db.Database;
import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryImportHistoryServiceTest {
    private Database database;
    private InventoryImportHistoryService history;
    private CurrentUser user;
    private Connection keepAlive;
    private final Instant uploadedAt = Instant.parse("2026-11-12T12:30:00Z");

    @Before public void setup() throws Exception {
        String url = "jdbc:h2:mem:history" + System.nanoTime() + ";MODE=MySQL";
        keepAlive = DriverManager.getConnection(url);
        String migration = new String(Files.readAllBytes(Paths.get("conf/evolutions/default/129.sql")), StandardCharsets.UTF_8);
        String schema = migration.split("# --- !Downs")[0].replace("# --- !Ups", "")
                .replace("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "").trim();
        try (Statement statement = keepAlive.createStatement()) { statement.execute(schema); }
        database = mock(Database.class);
        when(database.getConnection()).thenAnswer(call -> DriverManager.getConnection(url));
        history = new InventoryImportHistoryService(database);
        user = mock(CurrentUser.class);
        when(user.getId()).thenReturn(7);
        when(user.getFirstName()).thenReturn("CSV");
        when(user.getLastName()).thenReturn("Tester");
    }
    @After public void cleanup() throws Exception { keepAlive.close(); }

    @Test public void persistsCountsUploaderFilenameAndUploadTime() throws Exception {
        InventoryImportResponse result = new InventoryImportResponse();
        result.complete(2, 3);
        history.record(1, user, "C:/uploads/demo.csv", uploadedAt, result);
        InventoryImportHistoryItem item = new InventoryImportHistoryService(database).list(1, 1).get(0);
        assertEquals("CSV Tester", item.getUploadedBy());
        assertEquals("demo.csv", item.getFilename());
        assertEquals("2026-11-12 12:30 UTC", item.getUploadedAt());
        assertTrue(item.getSuccessful());
        assertEquals("Successful", item.getOutcome());
        assertEquals(Integer.valueOf(2), item.getAdded());
        assertEquals(Integer.valueOf(3), item.getUpdated());
    }
    @Test public void partialSubmissionKeepsSavedCounts() throws Exception {
        InventoryImportResponse result = new InventoryImportResponse();
        result.complete(2, 3);
        result.addError("import", "failed");
        history.record(1, user, "partial.csv", uploadedAt, result);
        InventoryImportHistoryItem item = history.list(1, 1).get(0);
        assertFalse(item.getSuccessful());
        assertEquals("Partially successful", item.getOutcome());
        assertEquals(Integer.valueOf(2), item.getAdded());
        assertEquals(Integer.valueOf(3), item.getUpdated());
    }
    @Test public void failureWithoutSavedCountsStaysFailed() throws Exception {
        ServiceResponse<String> result = new ServiceResponse<>();
        result.addError("import", "failed");
        history.record(1, user, null, uploadedAt, result);
        InventoryImportHistoryItem item = history.list(1, 1).get(0);
        assertEquals("Failed", item.getOutcome());
        assertNull(item.getAdded());
        assertNull(item.getUpdated());
        assertEquals("No file selected", item.getFilename());
    }
    @Test public void zeroSavedMedicationsIsNotPartialSuccess() throws Exception {
        InventoryImportResponse result = new InventoryImportResponse();
        result.complete(0, 0);
        result.addError("import", "invalid rows");
        history.record(1, user, "invalid.csv", uploadedAt, result);
        InventoryImportHistoryItem item = history.list(1, 1).get(0);
        assertEquals("Failed", item.getOutcome());
        assertEquals(Integer.valueOf(0), item.getAdded());
        assertEquals(Integer.valueOf(0), item.getUpdated());
    }
    @Test public void filtersTripAndPaginatesNewestFirst() throws Exception {
        ServiceResponse<String> result = new ServiceResponse<>();
        result.addError("import", "failed");
        history.record(2, user, "other.csv", uploadedAt, result);
        for (int i = 0; i < 27; i++) history.record(1, user, "file" + i + ".csv", uploadedAt, result);
        List<InventoryImportHistoryItem> first = history.list(1, 1);
        assertEquals(26, first.size()); // One extra row determines whether Next is displayed.
        assertEquals("file26.csv", first.get(0).getFilename());
        assertEquals(2, history.list(1, 2).size());
        assertEquals("file1.csv", history.list(1, 2).get(0).getFilename());
        assertTrue(history.list(3, 1).isEmpty());
    }
}
