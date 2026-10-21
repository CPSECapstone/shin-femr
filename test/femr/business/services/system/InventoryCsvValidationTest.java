package femr.business.services.system;

import femr.common.dtos.ServiceResponse;
import femr.common.dtos.CurrentUser;
import femr.common.models.MedicationItem;
import femr.data.models.core.IMedication;
import femr.data.daos.core.IMedicationRepository;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InventoryCsvValidationTest {
    private void assertValidation(String csv, String expected) throws Exception {
        IMedicationRepository repository = mock(IMedicationRepository.class);
        when(repository.retrieveMedicationInventoriesByTripId(1, false))
                .thenReturn(Collections.emptyList());
        InventoryService service = new InventoryService(repository, null, null, null, null, null);
        Path file = Files.createTempFile("inventory-validation", ".csv");
        try {
            Files.write(file, csv.getBytes(StandardCharsets.UTF_8));
            ServiceResponse<String> response = service.importCSV(1, file.toFile(), null);
            assertTrue(response.hasErrors());
            assertEquals(expected, response.getErrors().get("csvValidation"));
            verifyNoMoreInteractions(repository);
        } finally {
            Files.delete(file);
        }
    }

    @Test public void rejectsIncorrectAndIncompleteHeaders() throws Exception {
        String message = "CSV headers do not match. Expected: Medication,Quantity. "
                + "For exported inventory files, keep the original six column headers.";
        assertValidation("Medicine,Amount\nDemo Medication,10\n", message);
        assertValidation("Medication\nDemo Medication,10\n", message);
    }

    @Test public void rejectsMissingColumnsAndBlankValues() throws Exception {
        String message = "Row 2 is missing a medication or quantity. Each row must contain both columns and values.";
        for (String row : new String[]{"Demo Medication", "Demo Medication,", ",10"}) {
            assertValidation("Medication,Quantity\n" + row + "\n", message);
        }
    }

    @Test public void rejectsInvalidQuantitiesBeforeWritingMedication() throws Exception {
        String message = "Row 2 has an invalid quantity. Enter a whole number within the supported range.";
        for (String quantity : new String[]{"ten", "1.5", "2147483648"}) {
            assertValidation("Medication,Quantity\nDemo Medication," + quantity + "\n", message);
        }
    }

    @Test public void reportsLaterRowBeforeWritingEarlierValidRows() throws Exception {
        assertValidation("Medication,Quantity\nDemo Medication,10\nAnother Medication,ten\n",
                "Row 3 has an invalid quantity. Enter a whole number within the supported range.");
    }

    @Test public void validQuotedMedicationAndBomStillImport() throws Exception {
        IMedicationRepository repository = mock(IMedicationRepository.class);
        when(repository.retrieveMedicationInventoriesByTripId(1, false))
                .thenReturn(Collections.emptyList());
        IMedication medication = mock(IMedication.class);
        when(medication.getId()).thenReturn(7);
        when(repository.createNewMedication("Demo, Medication")).thenReturn(medication);
        InventoryService service = spy(new InventoryService(repository, null, null, null, null, null));
        doReturn(new ServiceResponse<MedicationItem>()).when(service)
                .createOrUpdateMedicationInventory(eq(7), eq(1), eq(10), any(), eq("Tester, CSV"));
        CurrentUser user = mock(CurrentUser.class);
        when(user.getLastName()).thenReturn("Tester");
        when(user.getFirstName()).thenReturn("CSV");
        Path file = Files.createTempFile("inventory-valid", ".csv");
        try {
            Files.write(file, "\uFEFFMedication,Quantity\n\"Demo, Medication\",10\n".getBytes(StandardCharsets.UTF_8));
            assertFalse(service.importCSV(1, file.toFile(), user).hasErrors());
            verify(service).createOrUpdateMedicationInventory(eq(7), eq(1), eq(10), any(), eq("Tester, CSV"));
        } finally {
            Files.delete(file);
        }
    }
}
