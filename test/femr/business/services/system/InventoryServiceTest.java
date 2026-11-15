package femr.business.services.system;

import femr.common.IItemModelMapper;
import femr.common.ItemModelMapper;
import femr.common.dtos.CurrentUser;
import femr.common.dtos.ServiceResponse;
import femr.common.models.CSVImportItem;
import femr.common.models.CSVImportRowItem;
import femr.common.models.CSVSubmitEditItem;
import femr.common.models.CSVSubmitItem;
import femr.data.IDataModelMapper;
import femr.data.daos.core.*;
import femr.data.models.core.IMedication;
import femr.data.models.core.IMedicationInventory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class InventoryServiceTest {
    private IMedicationRepository medicationRepository;
    private IUserRepository iUserRepository;
    private IDataModelMapper dataModelMapper;
    private IItemModelMapper itemModelMapper;
    private IBurnRateRepository iBurnRateRepository;
    private IPrescriptionRepository iPrescriptionRepository;
    private ICSVImportCacheRepository iCSVImportCacheRepository;
    private InventoryService inventoryService;

    @Before
    public void setUp() {
        medicationRepository = mock(IMedicationRepository.class);
        iUserRepository = mock(IUserRepository.class);
        dataModelMapper = mock(IDataModelMapper.class);
        itemModelMapper = spy(new ItemModelMapper());
        iBurnRateRepository = mock(IBurnRateRepository.class);
        iPrescriptionRepository = mock(IPrescriptionRepository.class);
        iCSVImportCacheRepository = mock(ICSVImportCacheRepository.class);

        inventoryService = new InventoryService(
                medicationRepository,
                iUserRepository,
                dataModelMapper,
                itemModelMapper,
                iBurnRateRepository,
                iPrescriptionRepository,
                iCSVImportCacheRepository
        );
    }

    @Test
    public void testParseCSVValidRows() throws Exception {
        CSVImportItem result = parseCSVContent("Medication,Quantity\nAspirin,25\nIbuprofen,10\n");

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> csvImportRowItems = result.getCSVImportRowItems();
        assertEquals(2, csvImportRowItems.size());

        CSVImportRowItem firstRow = csvImportRowItems.get(0);
        assertEquals(1L, firstRow.getRecordNumber());
        assertEquals("Aspirin", firstRow.getMedicationCellItem().getValue());
        assertEquals("25", firstRow.getQuantityCellItem().getValue());
        assertNull(firstRow.getMedicationCellItem().getCellError());
        assertNull(firstRow.getQuantityCellItem().getCellError());

        CSVImportRowItem secondRow = csvImportRowItems.get(1);
        assertEquals(2L, secondRow.getRecordNumber());
        assertEquals("Ibuprofen", secondRow.getMedicationCellItem().getValue());
        assertEquals("10", secondRow.getQuantityCellItem().getValue());
        assertNull(secondRow.getMedicationCellItem().getCellError());
        assertNull(secondRow.getQuantityCellItem().getCellError());

        ArrayList<String> csvFormatErrorsExpected = new ArrayList<>(); // Expect an empty arraylist of errors
        verify(itemModelMapper).createCSVImportItem(eq(0L), eq(csvImportRowItems), eq(csvFormatErrorsExpected));

        InOrder cacheOrder = inOrder(iCSVImportCacheRepository);
        cacheOrder.verify(iCSVImportCacheRepository).clearCache();
        cacheOrder.verify(iCSVImportCacheRepository).setCache(csvImportRowItems);
        cacheOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testParseCSVEmptyMedication() throws Exception {
        CSVImportItem result = parseCSVContent("Medication,Quantity\n,25\n");

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> csvImportRowItems = result.getCSVImportRowItems();
        assertEquals(1, csvImportRowItems.size());

        CSVImportRowItem firstRow = csvImportRowItems.get(0);
        assertEquals("", firstRow.getMedicationCellItem().getValue());
        assertEquals("Invalid medication", firstRow.getMedicationCellItem().getCellError());
        assertEquals("25", firstRow.getQuantityCellItem().getValue());
        assertNull(firstRow.getQuantityCellItem().getCellError());

        InOrder cacheOrder = inOrder(iCSVImportCacheRepository);
        cacheOrder.verify(iCSVImportCacheRepository).clearCache();
        cacheOrder.verify(iCSVImportCacheRepository).setCache(csvImportRowItems);
        cacheOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testParseCSVInvalidQuantity() throws Exception {
        CSVImportItem result = parseCSVContent("Medication,Quantity\nAspirin,abc\n");

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> csvImportRowItems = result.getCSVImportRowItems();
        assertEquals(1, csvImportRowItems.size());

        CSVImportRowItem firstRow = csvImportRowItems.get(0);
        assertEquals("Aspirin", firstRow.getMedicationCellItem().getValue());
        assertNull(firstRow.getMedicationCellItem().getCellError());
        assertEquals("abc", firstRow.getQuantityCellItem().getValue());
        assertEquals("Invalid quantity", firstRow.getQuantityCellItem().getCellError());

        InOrder cacheOrder = inOrder(iCSVImportCacheRepository);
        cacheOrder.verify(iCSVImportCacheRepository).clearCache();
        cacheOrder.verify(iCSVImportCacheRepository).setCache(csvImportRowItems);
        cacheOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testParseCSVMissingRequiredHeader() throws Exception {
        CSVImportItem result = parseCSVContent("Medication\nAspirin\n");

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> csvImportRowItems = result.getCSVImportRowItems();
        assertEquals(0, csvImportRowItems.size());

        ArrayList<String> csvFormatErrors = result.getCSVFormatErrors();
        ArrayList<String> cvsFormatErrorsExpected = new ArrayList<>();
        cvsFormatErrorsExpected.add("CSV header is missing required column(s) [Quantity]. Found: [Medication]");
        assertEquals(csvFormatErrors, cvsFormatErrorsExpected);

        InOrder cacheOrder = inOrder(iCSVImportCacheRepository);
        cacheOrder.verify(iCSVImportCacheRepository).clearCache();
        cacheOrder.verify(iCSVImportCacheRepository).setCache(csvImportRowItems);
        cacheOrder.verifyNoMoreInteractions();
    }

    private CSVImportItem parseCSVContent(String content) throws Exception {
        Path csv = Files.createTempFile("inventory-parse-", ".csv");
        try {
            Files.write(csv, content.getBytes(StandardCharsets.UTF_8));
            ServiceResponse<CSVImportItem> response = inventoryService.parseCSV(csv.toFile());
            assertFalse(response.hasErrors());
            assertNotNull(response.getResponseObject());
            return response.getResponseObject();
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    // Create tests for submitCSV
    private static final int TRIP_ID = 7;

    @Test
    public void testSubmitCSVCreatesNewMedications() {
        stubExistingInventory();
        stubCache(createRow(1L, "Aspirin", "25"), createRow(2L, "Ibuprofen", "10"));
        IMedication aspirin = stubNewMedication(1, "Aspirin");
        IMedication ibuprofen = stubNewMedication(2, "Ibuprofen");
        stubInventoryCreation(aspirin, 25);
        stubInventoryCreation(ibuprofen, 10);
        stubSaveReturnsArgument();

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(2, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        assertTrue(result.getCSVFormatErrors().isEmpty());

        verify(medicationRepository).createNewMedication("Aspirin");
        verify(medicationRepository).createNewMedication("Ibuprofen");
        verify(dataModelMapper).createMedicationInventory(25, 25, 1, TRIP_ID);
        verify(dataModelMapper).createMedicationInventory(10, 10, 2, TRIP_ID);
        verify(medicationRepository, times(2)).saveMedicationInventory(any(IMedicationInventory.class));
        // The created by name is formatted as "<last name>, <first name>"
        verify(itemModelMapper).createMedicationItem(eq(aspirin), any(), any(), isNull(), anyString(), eq("Doe, Jane"));

        verify(iCSVImportCacheRepository).setCache(new ArrayList<>());
        verify(iCSVImportCacheRepository, never()).clearCache();
    }

    @Test
    public void testSubmitCSVUpdatesExistingMedication() {
        DateTime originalTimeAdded = new DateTime(2020, 1, 1, 0, 0);
        IMedicationInventory aspirinInventory = createInventory(createMedication(1, "Aspirin"), 5, originalTimeAdded);
        stubExistingInventory(aspirinInventory);
        when(medicationRepository.retrieveMedicationInventoryByMedicationIdAndTripId(1, TRIP_ID)).thenReturn(aspirinInventory);
        stubSaveReturnsArgument();
        stubCache(createRow(1L, "Aspirin", "25"));

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(1, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());

        // The quantity is added onto the existing quantity, and no new medication or inventory entry is created
        verify(aspirinInventory).setQuantityCurrent(30);
        verify(aspirinInventory, never()).setTimeAdded(originalTimeAdded);
        verify(aspirinInventory).setTimeAdded(notNull());
        verify(medicationRepository).saveMedicationInventory(aspirinInventory);
        verify(medicationRepository, never()).createNewMedication(anyString());
        verify(dataModelMapper, never()).createMedicationInventory(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void testSubmitCSVExistingMedicationZeroQuantityKeepsTimeAdded() {
        DateTime originalTimeAdded = new DateTime(2020, 1, 1, 0, 0);
        IMedicationInventory aspirinInventory = createInventory(createMedication(1, "Aspirin"), 5, originalTimeAdded);
        stubExistingInventory(aspirinInventory);
        when(medicationRepository.retrieveMedicationInventoryByMedicationIdAndTripId(1, TRIP_ID)).thenReturn(aspirinInventory);
        stubSaveReturnsArgument();
        stubCache(createRow(1L, "Aspirin", "0"));

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(1, result.getNumberOfRowsAdded());
        verify(aspirinInventory).setQuantityCurrent(5);
        verify(aspirinInventory).setTimeAdded(originalTimeAdded);
    }

    @Test
    public void testSubmitCSVDuplicateNewMedicationCreatedOnce() {
        stubExistingInventory();
        stubCache(createRow(1L, "Aspirin", "25"), createRow(2L, "Aspirin", "10"));
        IMedication aspirin = stubNewMedication(1, "Aspirin");
        IMedicationInventory aspirinInventory = stubInventoryCreation(aspirin, 25);
        // Not in the inventory for the first row's lookup, present afterwards
        when(medicationRepository.retrieveMedicationInventoryByMedicationIdAndTripId(1, TRIP_ID))
                .thenReturn(null)
                .thenReturn(aspirinInventory);
        stubSaveReturnsArgument();

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(2, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        verify(medicationRepository, times(1)).createNewMedication("Aspirin");
        verify(dataModelMapper, times(1)).createMedicationInventory(anyInt(), anyInt(), anyInt(), anyInt());
        verify(aspirinInventory).setQuantityCurrent(35);
        verify(medicationRepository, times(2)).saveMedicationInventory(aspirinInventory);
    }

    @Test
    public void testSubmitCSVSkipsDeletedRows() {
        stubExistingInventory();
        CSVImportRowItem deletedRow = createRow(1L, "Aspirin", "25");
        CSVImportRowItem deletedInvalidRow = createRow(2L, "", "abc");
        stubCache(deletedRow, deletedInvalidRow, createRow(3L, "Ibuprofen", "10"));
        IMedication ibuprofen = stubNewMedication(2, "Ibuprofen");
        stubInventoryCreation(ibuprofen, 10);
        stubSaveReturnsArgument();

        CSVSubmitItem csvSubmitItem = new CSVSubmitItem();
        csvSubmitItem.addToDeletions(1L);
        csvSubmitItem.addToDeletions(2L);
        CSVImportItem result = submitCSV(csvSubmitItem);

        // Deleted rows are neither added to the formulary nor kept for a later submit
        assertEquals(1, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        verify(medicationRepository, never()).createNewMedication("Aspirin");
        verify(medicationRepository).createNewMedication("Ibuprofen");
        verify(iCSVImportCacheRepository).setCache(new ArrayList<>());
    }

    @Test
    public void testSubmitCSVDeletionTakesPrecedenceOverEdit() {
        stubExistingInventory();
        stubCache(createRow(1L, "Aspirin", "25"));

        CSVSubmitItem csvSubmitItem = new CSVSubmitItem();
        csvSubmitItem.addToDeletions(1L);
        csvSubmitItem.addToEdits(1L, createEdit("Ibuprofen", "10"));
        CSVImportItem result = submitCSV(csvSubmitItem);

        assertEquals(0, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        verify(medicationRepository, never()).createNewMedication(anyString());
        verify(medicationRepository, never()).saveMedicationInventory(any());
    }

    @Test
    public void testSubmitCSVInvalidRowsRemainInCache() {
        stubExistingInventory();
        CSVImportRowItem invalidMedicationRow = createRow(1L, "", "25");
        CSVImportRowItem invalidQuantityRow = createRow(2L, "Aspirin", "abc");
        invalidQuantityRow.setRowSubmitError("Stale submit error");
        stubCache(invalidMedicationRow, invalidQuantityRow);

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> remainingRows = result.getCSVImportRowItems();
        assertEquals(Arrays.asList(invalidMedicationRow, invalidQuantityRow), remainingRows);
        assertEquals("Invalid medication", remainingRows.get(0).getMedicationCellItem().getCellError());
        assertEquals("Invalid quantity", remainingRows.get(1).getQuantityCellItem().getCellError());
        // Rows failing cell validation never reach the submit step, so any previous submit error is cleared
        assertNull(remainingRows.get(0).getRowSubmitError());
        assertNull(remainingRows.get(1).getRowSubmitError());

        verify(medicationRepository, never()).createNewMedication(anyString());
        verify(medicationRepository, never()).saveMedicationInventory(any());
        verify(iCSVImportCacheRepository).setCache(remainingRows);
    }

    @Test
    public void testSubmitCSVEditsFixInvalidRow() {
        stubExistingInventory();
        stubCache(createRow(1L, "", "abc"));
        IMedication aspirin = stubNewMedication(1, "Aspirin");
        stubInventoryCreation(aspirin, 25);
        stubSaveReturnsArgument();

        CSVSubmitItem csvSubmitItem = new CSVSubmitItem();
        csvSubmitItem.addToEdits(1L, createEdit("Aspirin", "25"));
        CSVImportItem result = submitCSV(csvSubmitItem);

        assertEquals(1, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        verify(medicationRepository).createNewMedication("Aspirin");
        verify(dataModelMapper).createMedicationInventory(25, 25, 1, TRIP_ID);
    }

    @Test
    public void testSubmitCSVEditOverridesValidRowValues() {
        stubExistingInventory();
        stubCache(createRow(1L, "Aspirin", "25"));
        IMedication ibuprofen = stubNewMedication(2, "Ibuprofen");
        stubInventoryCreation(ibuprofen, 25);
        stubSaveReturnsArgument();

        // Only the medication is edited, a null quantity leaves the cached quantity untouched
        CSVSubmitItem csvSubmitItem = new CSVSubmitItem();
        csvSubmitItem.addToEdits(1L, createEdit("Ibuprofen", null));
        CSVImportItem result = submitCSV(csvSubmitItem);

        assertEquals(1, result.getNumberOfRowsAdded());
        verify(medicationRepository, never()).createNewMedication("Aspirin");
        verify(medicationRepository).createNewMedication("Ibuprofen");
        verify(dataModelMapper).createMedicationInventory(25, 25, 2, TRIP_ID);
    }

    @Test
    public void testSubmitCSVInvalidEditKeepsRowInCache() {
        stubExistingInventory();
        CSVImportRowItem row = createRow(1L, "Aspirin", "25");
        stubCache(row);

        CSVSubmitItem csvSubmitItem = new CSVSubmitItem();
        csvSubmitItem.addToEdits(1L, createEdit("", "xyz"));
        CSVImportItem result = submitCSV(csvSubmitItem);

        assertEquals(0, result.getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> remainingRows = result.getCSVImportRowItems();
        assertEquals(Collections.singletonList(row), remainingRows);
        assertEquals("", row.getMedicationCellItem().getValue());
        assertEquals("Invalid medication", row.getMedicationCellItem().getCellError());
        assertEquals("xyz", row.getQuantityCellItem().getValue());
        assertEquals("Invalid quantity", row.getQuantityCellItem().getCellError());
        assertNull(row.getRowSubmitError());

        verify(medicationRepository, never()).createNewMedication(anyString());
        verify(iCSVImportCacheRepository).setCache(remainingRows);
    }

    @Test
    public void testSubmitCSVInventoryErrorKeepsRowWithSubmitError() {
        stubExistingInventory();
        CSVImportRowItem failingRow = createRow(1L, "Aspirin", "25");
        stubCache(failingRow, createRow(2L, "Ibuprofen", "10"));
        IMedication aspirin = stubNewMedication(1, "Aspirin");
        IMedication ibuprofen = stubNewMedication(2, "Ibuprofen");
        IMedicationInventory aspirinInventory = stubInventoryCreation(aspirin, 25);
        stubInventoryCreation(ibuprofen, 10);
        stubSaveReturnsArgument();
        when(medicationRepository.saveMedicationInventory(aspirinInventory)).thenThrow(new RuntimeException("save failed"));

        ServiceResponse<CSVImportItem> response = inventoryService.submitCSV(TRIP_ID, createCurrentUser(), new CSVSubmitItem());

        // Row level failures are reported on the row, not as errors on the service response
        assertFalse(response.hasErrors());
        // Only the row that saved successfully counts as added
        assertEquals(1, response.getResponseObject().getNumberOfRowsAdded());
        ArrayList<CSVImportRowItem> remainingRows = response.getResponseObject().getCSVImportRowItems();
        assertEquals(Collections.singletonList(failingRow), remainingRows);
        assertEquals("[save failed]", failingRow.getRowSubmitError());
        assertNull(failingRow.getMedicationCellItem().getCellError());
        assertNull(failingRow.getQuantityCellItem().getCellError());
        // The failure of one row does not stop the following rows from being submitted
        verify(dataModelMapper).createMedicationInventory(10, 10, 2, TRIP_ID);
        verify(iCSVImportCacheRepository).setCache(remainingRows);
    }

    @Test
    public void testSubmitCSVEmptyCache() {
        stubExistingInventory();
        stubCache();

        CSVImportItem result = submitCSV(new CSVSubmitItem());

        assertEquals(0, result.getNumberOfRowsAdded());
        assertTrue(result.getCSVImportRowItems().isEmpty());
        assertTrue(result.getCSVFormatErrors().isEmpty());
        verify(medicationRepository).retrieveMedicationInventoriesByTripId(TRIP_ID, false);
        verify(medicationRepository, never()).saveMedicationInventory(any());
        verify(iCSVImportCacheRepository).setCache(new ArrayList<>());
    }

    private CSVImportItem submitCSV(CSVSubmitItem csvSubmitItem) {
        ServiceResponse<CSVImportItem> response = inventoryService.submitCSV(TRIP_ID, createCurrentUser(), csvSubmitItem);
        assertFalse(response.hasErrors());
        assertNotNull(response.getResponseObject());
        return response.getResponseObject();
    }

    private CurrentUser createCurrentUser() {
        return new CurrentUser(1, "Jane", "Doe", "jane.doe@example.com", new ArrayList<>(), TRIP_ID, 0L, "en");
    }

    // Builds a cached row the same way parseCSV would, including the cell validation errors
    private CSVImportRowItem createRow(long recordNumber, String medication, String quantity) {
        String medicationCellError = (medication == null || medication.isEmpty()) ? "Invalid medication" : null;
        String quantityCellError = null;
        try {
            Integer.parseInt(quantity);
        } catch (NumberFormatException ex) {
            quantityCellError = "Invalid quantity";
        }

        CSVImportRowItem rowItem = new CSVImportRowItem();
        rowItem.setRecordNumber(recordNumber);
        rowItem.getMedicationCellItem().setValue(medication);
        rowItem.getMedicationCellItem().setCellError(medicationCellError);
        rowItem.getQuantityCellItem().setValue(quantity);
        rowItem.getQuantityCellItem().setCellError(quantityCellError);
        return rowItem;
    }

    private CSVSubmitEditItem createEdit(String medication, String quantity) {
        CSVSubmitEditItem csvSubmitEditItem = new CSVSubmitEditItem();
        csvSubmitEditItem.setMedication(medication);
        csvSubmitEditItem.setQuantity(quantity);
        return csvSubmitEditItem;
    }

    private void stubCache(CSVImportRowItem... rowItems) {
        when(iCSVImportCacheRepository.getCache()).thenReturn(new ArrayList<>(Arrays.asList(rowItems)));
    }

    private void stubExistingInventory(IMedicationInventory... medicationInventories) {
        List<IMedicationInventory> existentMedicationInventory = Arrays.asList(medicationInventories);
        doReturn(existentMedicationInventory).when(medicationRepository).retrieveMedicationInventoriesByTripId(TRIP_ID, false);
    }

    private IMedication createMedication(int id, String name) {
        IMedication medication = mock(IMedication.class);
        when(medication.getId()).thenReturn(id);
        when(medication.getName()).thenReturn(name);
        return medication;
    }

    // Inventory mock whose current quantity follows setQuantityCurrent, so repeated updates accumulate
    private IMedicationInventory createInventory(IMedication medication, int quantityCurrent, DateTime timeAdded) {
        IMedicationInventory medicationInventory = mock(IMedicationInventory.class);
        final int[] quantity = new int[] {quantityCurrent};
        when(medicationInventory.getMedication()).thenReturn(medication);
        when(medicationInventory.getTimeAdded()).thenReturn(timeAdded);
        when(medicationInventory.getQuantityInitial()).thenReturn(quantityCurrent);
        when(medicationInventory.getQuantityCurrent()).thenAnswer(invocation -> quantity[0]);
        doAnswer(invocation -> {
            quantity[0] = invocation.getArgument(0);
            return null;
        }).when(medicationInventory).setQuantityCurrent(anyInt());
        return medicationInventory;
    }

    private IMedication stubNewMedication(int id, String name) {
        IMedication medication = createMedication(id, name);
        when(medicationRepository.createNewMedication(name)).thenReturn(medication);
        return medication;
    }

    private IMedicationInventory stubInventoryCreation(IMedication medication, int quantity) {
        IMedicationInventory medicationInventory = createInventory(medication, quantity, null);
        int medicationId = medication.getId();
        when(dataModelMapper.createMedicationInventory(quantity, quantity, medicationId, TRIP_ID)).thenReturn(medicationInventory);
        return medicationInventory;
    }

    private void stubSaveReturnsArgument() {
        when(medicationRepository.saveMedicationInventory(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
