/*
     fEMR - fast Electronic Medical Records
     Copyright (C) 2014  Team fEMR

     fEMR is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     fEMR is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     You should have received a copy of the GNU General Public License
     along with fEMR.  If not, see <http://www.gnu.org/licenses/>. If
     you have any questions, contact <info@teamfemr.org>.
*/ 
package femr.ui.controllers.admin;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import femr.business.services.core.*;
import femr.common.dtos.CurrentUser;
import femr.common.dtos.ServiceResponse;
import femr.common.dtos.InventoryImportResponse;
import femr.business.services.system.InventoryImportHistoryService;
import femr.common.models.InventoryImportHistoryItem;
import femr.data.daos.core.ICSVImportCacheRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import play.Logger;
import femr.common.models.MedicationItem;
import femr.common.models.MissionTripItem;
import femr.common.models.CSVImportItem;
import femr.common.models.CSVSubmitEditItem;
import femr.common.models.CSVSubmitItem;
import femr.data.models.mysql.Roles;
import femr.ui.helpers.security.AllowedRoles;
import femr.ui.helpers.security.FEMRAuthenticated;
import femr.ui.models.admin.inventory.*;
import femr.ui.views.html.admin.inventory.custom;
import femr.ui.views.html.admin.inventory.existing;
import femr.ui.views.html.admin.inventory.manage;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Security.Authenticated(FEMRAuthenticated.class)
@AllowedRoles({Roles.ADMINISTRATOR, Roles.SUPERUSER})
public class InventoryController extends Controller {

    private final AssetsFinder assetsFinder;
    private final FormFactory formFactory;
    private final IConceptService conceptService;
    private final IInventoryService inventoryService;
    private final IMedicationService medicationService;
    private final IMissionTripService missionTripService;
    private final ISessionService sessionService;
    private final InventoryImportHistoryService importHistoryService;
    private final ICSVImportCacheRepository importCache;
    private static final Map<String, PendingPreview> previews = new ConcurrentHashMap<>();

    @Inject
    public InventoryController(AssetsFinder assetsFinder,
                               FormFactory formFactory,
                               IConceptService conceptService,
                               IInventoryService inventoryService,
                               IMedicationService medicationService,
                               IMissionTripService missionTripService,
                               ISessionService sessionService, InventoryImportHistoryService importHistoryService, ICSVImportCacheRepository importCache) {

        this.assetsFinder = assetsFinder;
        this.formFactory = formFactory;
        this.conceptService = conceptService;
        this.inventoryService = inventoryService;
        this.medicationService = medicationService;
        this.missionTripService = missionTripService;
        this.sessionService = sessionService;
        this.importHistoryService = importHistoryService;
        this.importCache = importCache;
    }

    /**
     * Serves up the inventory homepage. POST communication for updating quantity
     * is handled by AJAX calls
     *
     * @param tripID trip id of selected mission trip
     *               when initially clicking the inventory page tripID = 0
     * @return returns viewModel with updated inventory
     */
    public Result manageGet(Integer tripID) {
        CurrentUser currentUser = sessionService.retrieveCurrentUserSession();
        return ok(manage.render(currentUser, createManageViewModel(tripID, currentUser, null), assetsFinder));
    }

    private ManageViewModelGet createManageViewModel(Integer tripID, CurrentUser currentUser, CSVImportItem importPreview) {
        ManageViewModelGet viewModel = new ManageViewModelGet();
        viewModel.setImportPreview(importPreview);

        // if tripID = 0 display the user's current trip
        // otherwise use the trip they have selected
        Integer retrieveTripID;
        if(tripID == 0) {
            retrieveTripID = currentUser.getTripId();
        } else {
            retrieveTripID = tripID;
        }

        // If the use does not have a trip ID, we cannot retrieve the list of medications
        // since they are tied to a trip
        if( retrieveTripID != null ){

            ServiceResponse<List<MedicationItem>> medicationServiceResponse = inventoryService.retrieveMedicationInventorysByTripId(retrieveTripID);
            if (medicationServiceResponse.hasErrors()) {
                throw new RuntimeException();
            } else {
                viewModel.setMedications(medicationServiceResponse.getResponseObject());
            }


            ServiceResponse<MissionTripItem> missionTripServiceResponse = missionTripService.retrieveAllTripInformationByTripId(retrieveTripID);
            if (missionTripServiceResponse.hasErrors()) {

                throw new RuntimeException();
            } else {

                viewModel.setMissionTripItem(missionTripServiceResponse.getResponseObject());
            }

            ServiceResponse<List<MissionTripItem>> missionTripListServiceResponse = missionTripService.retrieveAllTripInformationByUserId(currentUser.getId());
            if (missionTripServiceResponse.hasErrors()) {

                throw new RuntimeException();
            } else {

                viewModel.setMissionTripList(missionTripListServiceResponse.getResponseObject());
            }
        } else {

            viewModel.setMedications(new ArrayList<>());
            viewModel.setMissionTripList(new ArrayList<>());
        }

        return viewModel;
    }

    /**
     * Handles the submission of a trip selection for viewing inventory of a specific trip by a user
     *
     * @return redirects the user to manageGet() for the selected trip
     */
    public Result managePost() {

        final Form<ManageViewModelPost> manageViewModelForm = formFactory.form(ManageViewModelPost.class);
        ManageViewModelPost viewModel = manageViewModelForm.bindFromRequest().get();
        return redirect("/admin/inventory/" + viewModel.getSelectedTrip());
    }
    /**
     * Page for adding a new medication from the concept dictionary
     * @param tripId add custom medication to trip with this tripID - defaults to user's current
     *               trip if they do not select another trip
     * @return updated trip inventory
     */
    public Result customGet(Integer tripId) {
        CurrentUser currentUser = sessionService.retrieveCurrentUserSession();

        CustomViewModelGet viewModel = new CustomViewModelGet();

        ServiceResponse<List<String>> availableMedicationFormsResponse = medicationService.retrieveAvailableMedicationForms();
        if (availableMedicationFormsResponse.hasErrors()) {
            throw new RuntimeException();
        } else {
            viewModel.setAvailableForms(availableMedicationFormsResponse.getResponseObject());
        }

        ServiceResponse<List<String>> availableMedicationUnitsResponse = medicationService.retrieveAvailableMedicationUnits();
        if (availableMedicationUnitsResponse.hasErrors()) {
            throw new RuntimeException();
        } else {
            viewModel.setAvailableUnits(availableMedicationUnitsResponse.getResponseObject());
        }

        ServiceResponse<MissionTripItem> missionTripServiceResponse = missionTripService.retrieveAllTripInformationByTripId(tripId);
        if (missionTripServiceResponse.hasErrors()) {

            throw new RuntimeException();
        } else {

            viewModel.setMissionTripItem(missionTripServiceResponse.getResponseObject());
        }

        return ok(custom.render(currentUser, viewModel, assetsFinder));
    }

    /**
     * Handles the submission of a new custom medication from an Admin
     * @param tripId add existing medication to trip with this tripID - defaults to user's current
     *               trip if they do not select another trip
     * @return updated trip inventory
     */
    public Result customPost(Integer tripId){

        final Form<CustomViewModelPost> inventoryViewModelPostForm = formFactory.form(CustomViewModelPost.class);

        Form<CustomViewModelPost> form = inventoryViewModelPostForm.bindFromRequest();

        if (form.hasErrors()) {
            return redirect("/admin/inventory/0");

            //System.out.println(form.errors().toString());
            //if the request gets past the javascript validation and fails validation in the viewmodel, then
            //don't proceed to save anything. In the future, this should alert the user as to what they did
            //wrong.
            /* Should be validated client side and server-side throws error
            //throw new RuntimeException();    */

        }

        CustomViewModelPost inventoryViewModelPost = form.bindFromRequest().get();



        // create a new medicationItem for managing the compilation of active ingredients
        // (this could potentially be moved into the service layer)
        MedicationItem medicationItem = new MedicationItem();

        // assumes medication strength is a good indicator of how many
        // strength/unit/ingredients (active ingredietns) are involved
        // *denominator field not taken into consideration - always false
        if (inventoryViewModelPost.getMedicationStrength().get(0) != null) {

            for (int genericIndex = 0; genericIndex < inventoryViewModelPost.getMedicationStrength().size(); genericIndex++) {

                if (inventoryViewModelPost.getMedicationIngredient().get(genericIndex) != null &&
                        inventoryViewModelPost.getMedicationUnit().get(genericIndex) != null &&
                        inventoryViewModelPost.getMedicationStrength().get(genericIndex) != null) {

                    medicationItem.addActiveIngredient(
                            inventoryViewModelPost.getMedicationIngredient().get(genericIndex),
                            inventoryViewModelPost.getMedicationUnit().get(genericIndex),
                            inventoryViewModelPost.getMedicationStrength().get(genericIndex),
                            false
                    );
                }
            }

            //Creates the medication
            ServiceResponse<MedicationItem> createMedicationServiceResponse = medicationService.createMedication(
                    inventoryViewModelPost.getMedicationName(),
                    inventoryViewModelPost.getMedicationForm(),
                    medicationItem.getActiveIngredients());
            // check for errors before updating the quantity to ensure a medication ID has been returned in the response
            // object.
            if (createMedicationServiceResponse.hasErrors()) {

                return internalServerError();
            }

            int medicationId = createMedicationServiceResponse.getResponseObject().getId();
            int quantity = inventoryViewModelPost.getMedicationQuantity();

            ServiceResponse<MedicationItem> createMedicationInventoryServiceResponse;
            ServiceResponse<Boolean> doesInventoryExistInTrip = inventoryService.existsInventoryMedicationInTrip(medicationId, tripId);
            if (doesInventoryExistInTrip.hasErrors()){
                throw new RuntimeException();
            }
            //Creates an inventory for the Medication
            if (doesInventoryExistInTrip.getResponseObject()){
                createMedicationInventoryServiceResponse = inventoryService.reAddInventoryMedication(medicationId, tripId);
            } else {
                createMedicationInventoryServiceResponse = inventoryService.createOrUpdateMedicationInventory(medicationId, tripId,0,null,null);
            }
            //sets initial total quantity
            ServiceResponse<MedicationItem> setQuantityTotalServiceResponse =
                    inventoryService.setQuantityTotal(medicationId, tripId, quantity);
            //sets initial current quanitty
            ServiceResponse<MedicationItem> setQuantityCurrentServiceResponse =
                    inventoryService.setQuantityCurrent(medicationId, tripId, quantity);



            if (createMedicationInventoryServiceResponse.hasErrors() || setQuantityTotalServiceResponse.hasErrors()) {

                return internalServerError();
            }
        }

        return redirect("/admin/inventory/"+tripId);
    }

    /**
     * Page for adding an existing medication from the concept dictionary
     * @param tripId add existing medication to trip with this tripID - defaults to user's current
     *               trip if they do not select another trip
     * @return updated trip inventory
     */
    public Result existingGet(Integer tripId) {
        CurrentUser currentUser = sessionService.retrieveCurrentUserSession();

        ExistingViewModelGet viewModel = new ExistingViewModelGet();

        ServiceResponse<List<MedicationItem>> conceptMedicationServiceResponse = conceptService.retrieveAllMedicationConcepts();
        if (conceptMedicationServiceResponse.hasErrors()) {
            throw new RuntimeException();
        } else {
            viewModel.setConceptMedications(conceptMedicationServiceResponse.getResponseObject());
        }

        ServiceResponse<MissionTripItem> missionTripServiceResponse = missionTripService.retrieveAllTripInformationByTripId(tripId);
        if (missionTripServiceResponse.hasErrors()) {

            throw new RuntimeException();
        } else {

            viewModel.setMissionTripItem(missionTripServiceResponse.getResponseObject());
        }

        return ok(existing.render(currentUser, viewModel, assetsFinder));
    }

    /**
     * Handles the submission of an existing medication from an Admin
     * @param tripId add existing medication to trip with this tripID - defaults to user's current
     *               trip if they do not select another trip
     * @return updated trip inventory
     */
    public Result existingPost(Integer tripId) {

        final Form<ExistingViewModelPost> existingViewModelPostForm = formFactory.form(ExistingViewModelPost.class);
        Form<ExistingViewModelPost> existingForm = existingViewModelPostForm.bindFromRequest();
        ExistingViewModelPost existingViewModelPost = existingForm.bindFromRequest().get();

        //if just adding medications from the concept dictionary, there won't be any amount involved
        //and knowledge of the ingredients already exists.
        if (existingViewModelPost.getNewConceptMedicationsForInventory() != null) {

            ServiceResponse<MedicationItem> conceptMedicationServiceResponse;
            ServiceResponse<MedicationItem> medicationItemServiceResponse;
            MedicationItem conceptMedicationItem;

            //for each concept medication id that was sent from the select2 form
            for (Integer conceptMedicationId : existingViewModelPost.getNewConceptMedicationsForInventory()) {

                //get the actual concept MedicationItem from the id that was sent in
                conceptMedicationServiceResponse = conceptService.retrieveConceptMedication(conceptMedicationId);
                if (conceptMedicationServiceResponse.hasErrors()) {

                    return internalServerError();
                } else {

                    //create a non-concept MedicationItem from the concept MedicationItem
                    conceptMedicationItem = conceptMedicationServiceResponse.getResponseObject();
                    medicationItemServiceResponse = medicationService.createMedication(conceptMedicationItem.getName(), conceptMedicationItem.getForm(), conceptMedicationItem.getActiveIngredients());

                    if (medicationItemServiceResponse.hasErrors()) {

                        return internalServerError();
                    } else {

                        //Check to see if the medication has ever been added to the trip inventory.
                        // If so, set it's soft deletion state to being 'undeleted'. Otherwise, create the inventory medication.
                        ServiceResponse<MedicationItem> createOrReAddInventoryResponse = null;
                        if(inventoryService.existsInventoryMedicationInTrip(medicationItemServiceResponse.getResponseObject().getId(),tripId).getResponseObject()){
                            createOrReAddInventoryResponse = inventoryService.reAddInventoryMedication(medicationItemServiceResponse.getResponseObject().getId(), tripId);
                        } else {
                            createOrReAddInventoryResponse = inventoryService.createOrUpdateMedicationInventory(medicationItemServiceResponse.getResponseObject().getId(), tripId,0,null,null);
                        }

                        if (createOrReAddInventoryResponse.hasErrors()) {

                            return internalServerError();
                        }
                    }


                }
            }
        }

        return redirect("/admin/inventory/"+tripId);
    }

    /**
     * Called when a user wants to export the data to a CSV file.
     * On Inventory page, clicking on the button 'Export as CSV' calls this method.
     * @param tripId export inventory for trip with this ID - defaults to user's current
     *               trip if they do not select another trip
     * @return inventory CSV file
     */
    public Result exportGet(int tripId) {

      ServiceResponse<String> exportServiceResponse = inventoryService.exportCSV(tripId);

      SimpleDateFormat format = new SimpleDateFormat("MMddyy-HHmmss");
      String timestamp = format.format(new Date());
      String csvFileName = "inventory-"+timestamp+".csv";
      response().setHeader("Content-disposition", "attachment; filename=" + csvFileName);

      return ok(exportServiceResponse.getResponseObject()).as("application/x-download");
    }

    /** Downloads a starter file whose columns match the inventory CSV parser. */
    public Result downloadTemplateGet() {
        response().setHeader("Content-disposition", "attachment; filename=inventory-template.csv");
        return ok("Medication,Quantity\n").as("text/csv; charset=utf-8");
    }



    /**
     * Called when a user wants to import some data from a CSV file.
     * @param tripId import inventory for trip with this ID - defaults to user's current
     *               trip if they do not select another trip
     * @return Result of importing
     */
    private void recordImport(int tripId, CurrentUser user, String filename, Instant uploadedAt,
                              ServiceResponse<String> result) {
        try {
            importHistoryService.record(tripId, user, filename, uploadedAt, result);
        } catch (Exception ex) {
            // An unavailable history store must not misreport or repeat a completed inventory write.
            Logger.error("Could not save CSV import history", ex);
            flash("warning", "The import result could not be saved to history. Check the inventory before uploading again.");
        }
    }

    public Result importHistoryGet(int tripId, int page) {
        if (page < 1 || page > 100000) return badRequest("Invalid history page.");
        CurrentUser user = sessionService.retrieveCurrentUserSession();
        ServiceResponse<MissionTripItem> trip = missionTripService.retrieveAllTripInformationByTripId(tripId);
        if (trip.hasErrors() || trip.getResponseObject() == null) return notFound("Trip not found.");
        try {
            List<InventoryImportHistoryItem> items = importHistoryService.list(tripId, page);
            boolean hasNext = items.size() > InventoryImportHistoryService.PAGE_SIZE;
            if (hasNext) items = new ArrayList<>(items.subList(0, InventoryImportHistoryService.PAGE_SIZE));
            return ok(femr.ui.views.html.admin.inventory.importHistory.render(
                    user, trip.getResponseObject(), items, page, hasNext, assetsFinder));
        } catch (Exception ex) {
            Logger.error("Could not load CSV import history", ex);
            flash("error", "Import history is temporarily unavailable. Please try again.");
            return redirect("/admin/inventory/" + tripId);
        }
    }

    public Result importCSV(int tripId) { return parseCSV(tripId); }

    /** Parses an uploaded CSV and renders it for review without applying changes. */
    public Result parseCSV(int tripId) {
        CurrentUser user = sessionService.retrieveCurrentUserSession();
        Instant uploadedAt = Instant.now();
        String filename = null;
        try {
            Http.MultipartFormData data = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart file = data == null || data.getFiles().isEmpty() ? null
                    : (Http.MultipartFormData.FilePart) data.getFiles().get(0);
            filename = file == null ? null : file.getFilename();
            if (filename == null || filename.trim().isEmpty()) {
                throw new IllegalArgumentException("Please choose a CSV file to upload.");
            }
            ServiceResponse<CSVImportItem> parsed;
            synchronized (importCache) {
                try { parsed = inventoryService.parseCSV(file.getFile()); }
                finally { importCache.setCache(new ArrayList<>()); }
            }
            if (parsed.hasErrors()) throw new IllegalArgumentException(String.join(" ", parsed.getErrors().values()));
            CSVImportItem preview = parsed.getResponseObject();
            if (!preview.getCSVFormatErrors().isEmpty()) {
                throw new IllegalArgumentException(String.join(" ", preview.getCSVFormatErrors()));
            }
            if (preview.getCSVImportRowItems().isEmpty()) {
                throw new IllegalArgumentException("The CSV contains no medication rows.");
            }
            return renderPreview(tripId, user, new PendingPreview(user.getId(), tripId, filename, uploadedAt, preview));
        } catch (Exception ex) {
            Logger.warn("CSV preview failed", ex);
            ServiceResponse<String> failure = new ServiceResponse<>();
            failure.addError("file", ex instanceof IllegalArgumentException ? ex.getMessage()
                    : "Could not read the CSV file. Check its columns and values.");
            flash("error", String.join(" ", failure.getErrors().values()));
            recordImport(tripId, user, filename, uploadedAt, failure);
            return redirect("/admin/inventory/" + tripId);
        }
    }

    protected Result renderPreview(int tripId, CurrentUser user, PendingPreview pending) {
        previews.entrySet().removeIf(entry -> entry.getValue().expiresAt < System.currentTimeMillis());
        String token = UUID.randomUUID().toString();
        previews.put(token, pending);
        return renderPreviewPage(tripId, user, pending, token);
    }

    protected Result renderPreviewPage(int tripId, CurrentUser user, PendingPreview pending, String token) {
        ManageViewModelGet model = createManageViewModel(tripId, user, pending.rows);
        model.setPendingImportToken(token);
        return ok(manage.render(user, model, assetsFinder));
    }

    private PendingPreview claimPreview(String token, int tripId, int userId) {
        if (token == null) return null;
        PendingPreview pending = previews.get(token);
        if (pending == null || pending.userId != userId || pending.tripId != tripId) return null;
        if (!previews.remove(token, pending) || pending.expiresAt < System.currentTimeMillis()) return null;
        return pending;
    }

    public Result cancelCSV(int tripId) {
        claimPreview(formFactory.form().bindFromRequest().get("pendingImportToken"), tripId,
                sessionService.retrieveCurrentUserSession().getId());
        return redirect("/admin/inventory/" + tripId);
    }

    protected static class PendingPreview {
        final int userId, tripId;
        final String filename;
        final Instant uploadedAt;
        final CSVImportItem rows;
        final long expiresAt = System.currentTimeMillis() + 30 * 60 * 1000;
        PendingPreview(int userId, int tripId, String filename, Instant uploadedAt, CSVImportItem rows) {
            this.userId = userId; this.tripId = tripId; this.filename = filename;
            this.uploadedAt = uploadedAt; this.rows = rows;
        }
    }

    /** Applies the user's edits and deletions to the CSV rows currently held by the import cache. */
    public Result submitCSV(int tripId) {
        DynamicForm form = formFactory.form().bindFromRequest();
        Map<Long, CSVSubmitEditItem> edits = new HashMap<>();
        CSVSubmitItem submitItem = new CSVSubmitItem();

        for (Map.Entry<String, String> entry : form.data().entrySet()) {
            String[] field = entry.getKey().split("\\.");
            if (field.length != 3 || !"preview".equals(field[0])) continue;
            long recordNumber;
            try {
                recordNumber = Long.parseLong(field[1]);
            } catch (NumberFormatException ex) {
                continue;
            }

            if ("removed".equals(field[2]) && "true".equals(entry.getValue())) {
                submitItem.addToDeletions(recordNumber);
                continue;
            }
            if ("medication".equals(field[2]) || "quantity".equals(field[2])) {
                CSVSubmitEditItem edit = edits.computeIfAbsent(recordNumber, ignored -> new CSVSubmitEditItem());
                if ("medication".equals(field[2])) edit.setMedication(entry.getValue());
                else edit.setQuantity(entry.getValue());
            }
        }
        edits.forEach(submitItem::addToEdits);

        CurrentUser currentUser = sessionService.retrieveCurrentUserSession();
        PendingPreview pending = claimPreview(form.get("pendingImportToken"), tripId, currentUser.getId());
        if (pending == null) {
            flash("error", "This preview has expired or was already submitted. Please upload the CSV again.");
            return redirect("/admin/inventory/" + tripId);
        }
        // Removing every pending row is cancellation, not a successful zero-row import.
        boolean hasRows = pending.rows.getCSVImportRowItems().stream()
                .anyMatch(row -> !submitItem.getDeletions().contains(row.getRecordNumber()));
        if (!hasRows) return redirect("/admin/inventory/" + tripId);
        try {
            ServiceResponse<CSVImportItem> response;
            synchronized (importCache) {
                try {
                    importCache.setCache(pending.rows.getCSVImportRowItems());
                    response = inventoryService.submitCSV(tripId, currentUser, submitItem);
                } finally { importCache.setCache(new ArrayList<>()); }
            }
            CSVImportItem remaining = response.getResponseObject();
            InventoryImportResponse result = new InventoryImportResponse();
            if (response.hasErrors() || remaining == null) {
                result.addError("import", "The import could not be completed. Check the inventory before retrying.");
            } else if (!remaining.getCSVImportRowItems().isEmpty() || !remaining.getCSVFormatErrors().isEmpty()) {
                result.addError("import", "Some rows were not imported. Correct the highlighted rows and submit again.");
            } else {
                result.complete(remaining.getMedicationsAdded(), remaining.getMedicationsUpdated());
            }
            recordImport(tripId, currentUser, pending.filename, pending.uploadedAt, result);
            if (!result.hasErrors()) {
                flash("success", result.getResponseObject());
                return redirect("/admin/inventory/" + tripId);
            }
            if (remaining != null && !remaining.getCSVImportRowItems().isEmpty()) {
                return renderPreview(tripId, currentUser, new PendingPreview(currentUser.getId(), tripId,
                        pending.filename, pending.uploadedAt, remaining));
            }
            flash("error", String.join(" ", result.getErrors().values()));
        } catch (Exception ex) {
            Logger.error("CSV submission failed", ex);
            ServiceResponse<String> failure = new ServiceResponse<>();
            failure.addError("import", "The import could not be completed. Check the inventory before retrying.");
            recordImport(tripId, currentUser, pending.filename, pending.uploadedAt, failure);
            flash("error", failure.getErrors().get("import"));
        }
        return redirect("/admin/inventory/" + tripId);
    }

    /**
     * Called when a user wants to export shopping list to a CSV file.
     * @param tripId export inventory for trip with this ID
     * @return shopping list CSV file
     */
    public Result exportShoppingListGet(int tripId) {

        final Form<ShoppingListViewModelPost> manageViewModelForm = formFactory.form(ShoppingListViewModelPost.class);
        ShoppingListViewModelPost viewModel = manageViewModelForm.bindFromRequest().get();

        ServiceResponse<String> exportServiceResponse = inventoryService.exportShoppingListCSV(tripId, viewModel.getWeeksOnHand());

        if (!exportServiceResponse.hasErrors()) {

            SimpleDateFormat format = new SimpleDateFormat("MMddyy-HHmmss");
            String timestamp = format.format(new Date());
            String csvFileName = "shopping-list-" + timestamp + ".csv";
            response().setHeader("Content-disposition", "attachment; filename=" + csvFileName);

            return ok(exportServiceResponse.getResponseObject()).as("application/x-download");

        }

        return internalServerError();
    }

    /**
     * Called when a user hits the remove button to remove a medication from the trip formulary.
     * @param medicationId
     * @param tripId
     * @return Result of soft-deletion
     */
    public Result ajaxDelete(int medicationId, int tripId) {
        ServiceResponse<MedicationItem> inventoryServiceResponse = inventoryService.deleteInventoryMedication(medicationId, tripId);

        if (inventoryServiceResponse.hasErrors()) {
            throw new RuntimeException();
        }
        return ok("true");
    }

    /**
     * Called when a user hits the undo button to readd a medication from the trip formulary.
     *
     * @param medicationId
     * @param tripId
     * @return Result of readding (undo-ing soft deletion)
     */
    public Result ajaxReadd(int medicationId, int tripId){
        ServiceResponse<MedicationItem> inventoryServiceResponse = inventoryService.reAddInventoryMedication(medicationId, tripId);

        if (inventoryServiceResponse.hasErrors()) {
            throw new RuntimeException();
        }
        return ok("true");
    }


    /**
     * Alters medication based on submit.
     */
    public Result ajaxEditCurrent(int medicationID, int tripId) {
        // Get POST data
        DynamicForm df = formFactory.form().bindFromRequest();
        int quantity = Integer.parseInt(df.get("quantity"));

        ServiceResponse<MedicationItem> inventoryServiceResponse = inventoryService.setQuantityCurrent(medicationID, tripId, quantity);
        if (inventoryServiceResponse.hasErrors()) {
            throw new RuntimeException();
        }
        return ok("true");
    }

    /**
     * Alters medication based on submit.
     */
    public Result ajaxEditTotal(int medicationID, int tripId) {
        // Get POST data
        DynamicForm df = formFactory.form().bindFromRequest();
        int quantity = Integer.parseInt(df.get("quantity"));

        ServiceResponse<MedicationItem> inventoryServiceResponse = inventoryService.setQuantityTotal(medicationID, tripId, quantity);
        if (inventoryServiceResponse.hasErrors()) {
            throw new RuntimeException();
        }
        return ok("true");
    }


}
