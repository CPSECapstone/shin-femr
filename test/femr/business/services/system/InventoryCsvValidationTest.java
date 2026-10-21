package femr.business.services.system;

import femr.common.dtos.ServiceResponse;
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
            verify(repository).retrieveMedicationInventoriesByTripId(1, false);
            verifyNoMoreInteractions(repository);
        } finally {
            Files.delete(file);
        }
    }

    @Test public void rejectsIncorrectAndIncompleteHeaders() throws Exception {
        String message = "CSV headers do not match. Expected: Medication,Quantity.";
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
}
