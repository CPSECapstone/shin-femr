package femr.business.services.system;

import femr.common.IItemModelMapper;
import femr.common.dtos.CurrentUser;
import femr.common.dtos.ServiceResponse;
import femr.common.models.MedicationItem;
import femr.data.daos.core.IMedicationRepository;
import femr.data.models.core.IMedication;
import femr.data.models.core.IMedicationInventory;
import org.junit.Before;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryImportSummaryTest {
    private InventoryService service;
    private CurrentUser user;

    @Before public void setup() {
        IMedicationRepository repository = mock(IMedicationRepository.class);
        IItemModelMapper mapper = mock(IItemModelMapper.class);
        IMedication existing = mock(IMedication.class);
        when(existing.getId()).thenReturn(1);
        IMedicationInventory existingInventory = mock(IMedicationInventory.class);
        when(existingInventory.getMedication()).thenReturn(existing);
        doReturn(Collections.singletonList(existingInventory)).when(repository)
                .retrieveMedicationInventoriesByTripId(1, false);
        MedicationItem item = new MedicationItem();
        item.setFullName("Existing");
        when(mapper.createMedicationItem(existing, 0, 0, null, null, null)).thenReturn(item);
        IMedication added = mock(IMedication.class);
        when(added.getId()).thenReturn(2);
        when(repository.createNewMedication("New")).thenReturn(added);
        IMedicationInventory addedInventory = mock(IMedicationInventory.class);
        when(addedInventory.getMedication()).thenReturn(added);
        when(repository.retrieveMedicationInventoryByMedicationIdAndTripId(2, 1)).thenReturn(addedInventory);
        service = spy(new InventoryService(repository, null, null, mapper, null, null));
        doReturn(new ServiceResponse<MedicationItem>()).when(service)
                .createOrUpdateMedicationInventory(anyInt(), eq(1), anyInt(), any(), anyString());
        user = mock(CurrentUser.class);
    }

    private ServiceResponse<String> importRows(String rows) throws Exception {
        Path file = Files.createTempFile("inventory-summary", ".csv");
        try {
            Files.write(file, ("Medication,Quantity\n" + rows).getBytes(StandardCharsets.UTF_8));
            return service.importCSV(1, file.toFile(), user);
        } finally {
            Files.delete(file);
        }
    }

    @Test public void countsAddedAndUpdatedMedicationsOnce() throws Exception {
        ServiceResponse<String> result = importRows("Existing,10\nNew,20\nNew,30\nExisting,40\n");
        assertFalse(result.hasErrors());
        assertEquals("Import successful: 1 medication added and 1 medication updated.", result.getResponseObject());
    }

    @Test public void reportsAddedOnly() throws Exception {
        assertEquals("Import successful: 1 medication added and 0 medications updated.",
                importRows("New,10\n").getResponseObject());
    }

    @Test public void failedSaveDoesNotReturnSuccessSummary() throws Exception {
        ServiceResponse<MedicationItem> failure = new ServiceResponse<>();
        failure.addError("save", "Save failed");
        doReturn(failure).when(service)
                .createOrUpdateMedicationInventory(eq(1), eq(1), anyInt(), any(), anyString());
        ServiceResponse<String> result = importRows("New,10\nExisting,20\n");
        assertTrue(result.hasErrors());
        assertNull(result.getResponseObject());
    }
}
