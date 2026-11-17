package femr.ui.controllers.admin;

import femr.business.services.core.*;
import femr.business.services.system.InventoryImportHistoryService;
import femr.common.dtos.*;
import femr.common.models.MedicationItem;
import femr.common.models.CSVImportItem;
import femr.common.models.CSVImportRowItem;
import femr.data.daos.core.ICSVImportCacheRepository;
import org.junit.*;
import org.mockito.ArgumentCaptor;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryImportHistoryControllerTest {
    private IInventoryService inventory;
    private InventoryImportHistoryService history;
    private InventoryController controller;
    private Http.RequestBody body;
    private Http.Flash flash;
    private FormFactory forms;
    private CurrentUser user;
    private File upload;
    private ICSVImportCacheRepository cache;

    @Before public void setup() {
        inventory = mock(IInventoryService.class);
        history = mock(InventoryImportHistoryService.class);
        ISessionService sessions = mock(ISessionService.class);
        user = mock(CurrentUser.class);
        when(user.getId()).thenReturn(7);
        when(sessions.retrieveCurrentUserSession()).thenReturn(user);
        forms = mock(FormFactory.class, RETURNS_DEEP_STUBS);
        cache = mock(ICSVImportCacheRepository.class);
        controller = spy(new InventoryController(null, forms, null, inventory, null, null, sessions, history, cache));
        doAnswer(call -> play.mvc.Results.redirect("/preview?token=" + call.getArgument(3)))
                .when(controller).renderPreviewPage(anyInt(), any(), any(), anyString());
        Http.Context context = mock(Http.Context.class);
        Http.Request request = mock(Http.Request.class);
        body = mock(Http.RequestBody.class);
        flash = new Http.Flash(new HashMap<>());
        when(context.request()).thenReturn(request);
        when(context.flash()).thenReturn(flash);
        when(request.body()).thenReturn(body);
        Http.Context.current.set(context);
        ServiceResponse<java.util.List<MedicationItem>> items = new ServiceResponse<>();
        items.setResponseObject(Collections.emptyList());
        when(inventory.retrieveMedicationInventorysByTripId(1)).thenReturn(items);
    }
    @After public void cleanup() throws Exception {
        Http.Context.current.remove();
        if (upload != null) Files.deleteIfExists(upload.toPath());
    }
    @SuppressWarnings("unchecked")
    private void upload(String contents) throws Exception {
        upload = File.createTempFile("history-upload", ".csv");
        Files.write(upload.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        Http.MultipartFormData data = mock(Http.MultipartFormData.class);
        Http.MultipartFormData.FilePart file = mock(Http.MultipartFormData.FilePart.class);
        when(file.getFilename()).thenReturn("demo.csv");
        when(file.getFile()).thenReturn(upload);
        when(data.getFiles()).thenReturn(Collections.singletonList(file));
        when(body.asMultipartFormData()).thenReturn(data);
    }
    @Test public void missingUploadRecordsFailure() throws Exception {
        controller.importCSV(1);
        verify(history).record(eq(1), eq(user), isNull(), any(Instant.class), argThat(result -> result.hasErrors()));
        assertEquals("Please choose a CSV file to upload.", flash.get("error"));
        verify(inventory, never()).importCSV(anyInt(), any(), any());
    }
    private String preview(String name) throws Exception {
        upload("Medication,Quantity\n" + name + ",10\n");
        CSVImportItem item = new CSVImportItem();
        item.getCSVImportRowItems().add(new CSVImportRowItem());
        ServiceResponse<CSVImportItem> parsed = new ServiceResponse<>();
        parsed.setResponseObject(item);
        when(inventory.parseCSV(any())).thenReturn(parsed);
        return controller.parseCSV(1).redirectLocation().get().split("token=")[1];
    }
    private void token(String value) {
        when(forms.form().bindFromRequest().get("pendingImportToken")).thenReturn(value);
        when(forms.form().bindFromRequest().data()).thenReturn(Collections.emptyMap());
    }
    private ServiceResponse<CSVImportItem> success() {
        CSVImportItem item = new CSVImportItem();
        item.setMedicationCounts(1, 2);
        ServiceResponse<CSVImportItem> response = new ServiceResponse<>();
        response.setResponseObject(item);
        return response;
    }
    @Test public void previewDoesNotRecordSuccessAndSubmitOnlyAppliesOnce() throws Exception {
        String id = preview("Demo");
        verifyNoInteractions(history);
        token(id);
        when(inventory.submitCSV(eq(1), eq(user), any())).thenReturn(success());
        controller.submitCSV(1);
        controller.submitCSV(1);
        verify(inventory, times(1)).submitCSV(eq(1), eq(user), any());
        verify(history, times(1)).record(eq(1), eq(user), eq("demo.csv"), any(),
                argThat(result -> !result.hasErrors() && ((InventoryImportResponse) result).getAdded() == 1));
    }
    @Test public void canceledPreviewDoesNotWriteOrRecordSuccess() throws Exception {
        token(preview("Demo"));
        controller.cancelCSV(1);
        controller.submitCSV(1);
        verify(inventory, never()).submitCSV(anyInt(), any(), any());
        verifyNoInteractions(history);
    }
    @Test public void otherUserCannotSubmitPreview() throws Exception {
        token(preview("Demo"));
        when(user.getId()).thenReturn(99);
        controller.submitCSV(1);
        verify(inventory, never()).submitCSV(anyInt(), any(), any());
        verifyNoInteractions(history);
    }
    @Test public void otherTripCannotSubmitPreview() throws Exception {
        token(preview("Demo"));
        controller.submitCSV(2);
        verify(inventory, never()).submitCSV(anyInt(), any(), any());
    }
    @Test public void remainingRowsRecordFailedAttemptAndCanBeRetried() throws Exception {
        token(preview("Demo"));
        ServiceResponse<CSVImportItem> partial = success();
        partial.getResponseObject().getCSVImportRowItems().add(new CSVImportRowItem());
        when(inventory.submitCSV(eq(1), eq(user), any())).thenReturn(partial);
        String next = controller.submitCSV(1).redirectLocation().get().split("token=")[1];
        verify(history).record(eq(1), eq(user), eq("demo.csv"), any(), argThat(result -> result.hasErrors()));
        token(next);
        when(inventory.submitCSV(eq(1), eq(user), any())).thenReturn(success());
        controller.submitCSV(1);
        verify(history, times(2)).record(eq(1), eq(user), eq("demo.csv"), any(), any());
    }
    @Test public void laterUploadDoesNotReplaceEarlierPreviewRows() throws Exception {
        String first = preview("First");
        org.mockito.ArgumentCaptor<InventoryController.PendingPreview> captured =
                org.mockito.ArgumentCaptor.forClass(InventoryController.PendingPreview.class);
        verify(controller).renderPreviewPage(eq(1), eq(user), captured.capture(), anyString());
        java.util.ArrayList<CSVImportRowItem> firstRows = captured.getValue().rows.getCSVImportRowItems();
        Files.delete(upload.toPath());
        preview("Second");
        token(first);
        when(inventory.submitCSV(eq(1), eq(user), any())).thenReturn(success());
        controller.submitCSV(1);
        verify(cache).setCache(same(firstRows));
    }

    @Test public void historyFailurePreservesSuccessfulResult() throws Exception {
        token(preview("Demo"));
        when(inventory.submitCSV(eq(1), eq(user), any())).thenReturn(success());
        doThrow(new java.sql.SQLException("unavailable")).when(history).record(anyInt(), any(), anyString(), any(), any());
        controller.submitCSV(1);
        assertNotNull(flash.get("success"));
        assertNull(flash.get("error"));
        assertNotNull(flash.get("warning"));
    }
}
