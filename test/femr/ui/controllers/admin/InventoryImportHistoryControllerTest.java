package femr.ui.controllers.admin;

import femr.business.services.core.*;
import femr.business.services.system.InventoryImportHistoryService;
import femr.common.dtos.*;
import femr.common.models.MedicationItem;
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

    @Before public void setup() {
        inventory = mock(IInventoryService.class);
        history = mock(InventoryImportHistoryService.class);
        ISessionService sessions = mock(ISessionService.class);
        user = mock(CurrentUser.class);
        when(user.getId()).thenReturn(7);
        when(sessions.retrieveCurrentUserSession()).thenReturn(user);
        forms = mock(FormFactory.class, RETURNS_DEEP_STUBS);
        controller = new InventoryController(null, forms, null, inventory, null, null, sessions, history);
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
    @Test public void successfulUploadRecordsCountsAfterImport() throws Exception {
        upload("Medication,Quantity\nDemo,10\n");
        InventoryImportResponse success = new InventoryImportResponse();
        success.complete(1, 2);
        when(inventory.importCSV(eq(1), any(), eq(user))).thenReturn(success);
        controller.importCSV(1);
        org.mockito.InOrder order = inOrder(inventory, history);
        order.verify(inventory).importCSV(eq(1), any(), eq(user));
        order.verify(history).record(eq(1), eq(user), eq("demo.csv"), any(Instant.class), same(success));
        assertEquals(success.getResponseObject(), flash.get("success"));
    }
    @Test public void failedUploadRecordsFailure() throws Exception {
        upload("Medication,Quantity\nDemo,ten\n");
        ServiceResponse<String> failure = new ServiceResponse<>();
        failure.addError("csv", "Invalid quantity");
        when(inventory.importCSV(eq(1), any(), eq(user))).thenReturn(failure);
        controller.importCSV(1);
        verify(history).record(eq(1), eq(user), eq("demo.csv"), any(Instant.class), same(failure));
        assertNull(flash.get("success"));
    }
    @Test public void unexpectedFailureIsRecorded() throws Exception {
        upload("Medication,Quantity\nDemo,10\n");
        when(inventory.importCSV(eq(1), any(), eq(user))).thenThrow(new IllegalArgumentException("bad row"));
        controller.importCSV(1);
        verify(history).record(eq(1), eq(user), eq("demo.csv"), any(Instant.class), argThat(result -> result.hasErrors()));
        assertNotNull(flash.get("error"));
    }
    @Test public void historyFailurePreservesSuccessfulResult() throws Exception {
        upload("Medication,Quantity\nDemo,10\n");
        InventoryImportResponse success = new InventoryImportResponse();
        success.complete(1, 0);
        when(inventory.importCSV(eq(1), any(), eq(user))).thenReturn(success);
        doThrow(new java.sql.SQLException("unavailable")).when(history).record(anyInt(), any(), anyString(), any(), any());
        controller.importCSV(1);
        assertEquals(success.getResponseObject(), flash.get("success"));
        assertNull(flash.get("error"));
        assertNotNull(flash.get("warning"));
    }
}
