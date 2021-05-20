package org.smartregister.reveal.presenter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.RectF;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonElement;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Event;
import org.smartregister.domain.Task;
import org.smartregister.domain.Task.TaskStatus;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.ListTaskContract;
import org.smartregister.reveal.contract.PasswordRequestCallback;
import org.smartregister.reveal.contract.UserLocationContract.UserLocationCallback;
import org.smartregister.reveal.interactor.ListTaskInteractor;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.FamilyCardDetails;
import org.smartregister.reveal.model.IRSVerificationCardDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.SprayCardDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.repository.RevealMappingHelper;
import org.smartregister.reveal.task.IndicatorsCalculatorTask;
import org.smartregister.reveal.util.AlertDialogUtils;
import org.smartregister.reveal.util.CardDetailsUtil;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.CONFIGURATION;
import org.smartregister.reveal.util.Constants.Filter;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.PasswordDialogUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.view.EditFociBoundaryActivity;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import timber.log.Timber;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TEXT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.domain.LocationProperty.PropertyStatus.INACTIVE;
import static org.smartregister.reveal.contract.ListTaskContract.ListTaskView;
import static org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.INCOMPLETE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.IN_PROGRESS;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_ELIGIBLE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_SPRAYABLE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_SPRAYED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_VISITED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.PARTIALLY_SPRAYED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.SPRAYED;
import static org.smartregister.reveal.util.Constants.DateFormat.EVENT_DATE_FORMAT_XXX;
import static org.smartregister.reveal.util.Constants.DateFormat.EVENT_DATE_FORMAT_Z;
import static org.smartregister.reveal.util.Constants.GeoJSON.FEATURES;
import static org.smartregister.reveal.util.Constants.GeoJSON.TYPE;
import static org.smartregister.reveal.util.Constants.Intervention.CDD_SUPERVISION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.IRS_VERIFICATION;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;
import static org.smartregister.reveal.util.Constants.JsonForm.DISTRICT_NAME;
import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;
import static org.smartregister.reveal.util.Constants.JsonForm.LOCATION_COMPONENT_ACTIVE;
import static org.smartregister.reveal.util.Constants.JsonForm.PROVINCE_NAME;
import static org.smartregister.reveal.util.Constants.JsonForm.VALID_OPERATIONAL_AREA;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.Map.CLICK_SELECT_RADIUS;
import static org.smartregister.reveal.util.Constants.Properties.FAMILY_MEMBER_NAMES;
import static org.smartregister.reveal.util.Constants.Properties.FEATURE_SELECT_TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.Properties.TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_CODE;
import static org.smartregister.reveal.util.Constants.Properties.TASK_CODE_LIST;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.Properties.TASK_STATUS;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Utils.formatDate;
import static org.smartregister.reveal.util.Utils.getMaxZoomLevel;
import static org.smartregister.reveal.util.Utils.getPropertyValue;
import static org.smartregister.reveal.util.Utils.isFocusInvestigation;
import static org.smartregister.reveal.util.Utils.isFocusInvestigationOrMDA;
import static org.smartregister.reveal.util.Utils.isKenyaMDALite;
import static org.smartregister.reveal.util.Utils.isZambiaIRSLite;
import static org.smartregister.reveal.util.Utils.validateFarStructures;


/**
 * Created by samuelgithengi on 11/27/18.
 */
public class ListTaskPresenter implements ListTaskContract.Presenter, PasswordRequestCallback,
        UserLocationCallback {

    private ListTaskView listTaskView;

    private ListTaskInteractor listTaskInteractor;

    private PreferencesUtil prefsUtil;

    private FeatureCollection featureCollection;

    private List<Feature> filterFeatureCollection;

    private List<Feature> searchFeatureCollection;

    private Feature operationalArea;

    private Feature selectedFeature;

    private String selectedFeatureInterventionType;

    private LatLng clickedPoint;

    private AlertDialog passwordDialog;

    private ValidateUserLocationPresenter locationPresenter;

    private CardDetails cardDetails;

    private boolean changeInterventionStatus;

    private BaseDrawerContract.Presenter drawerPresenter;

    private RevealJsonFormUtils jsonFormUtils;

    private boolean changeMapPosition;

    private RevealApplication revealApplication;

    private RevealMappingHelper mappingHelper;

    private boolean markStructureIneligibleConfirmed;

    private String reasonUnEligible;

    private boolean isTasksFiltered;

    private String searchPhrase;

    private TaskFilterParams filterParams;

    private MapboxMap mapboxMap;

    public ListTaskPresenter(ListTaskView listTaskView, BaseDrawerContract.Presenter drawerPresenter) {
        this.listTaskView = listTaskView;
        this.drawerPresenter = drawerPresenter;
        listTaskInteractor = new ListTaskInteractor(this);
        passwordDialog = PasswordDialogUtils.initPasswordDialog(listTaskView.getContext(), this);
        locationPresenter = new ValidateUserLocationPresenter(listTaskView, this);
        prefsUtil = PreferencesUtil.getInstance();
        jsonFormUtils = listTaskView.getJsonFormUtils();
        setChangeMapPosition(true);
        revealApplication = RevealApplication.getInstance();
        mappingHelper = new RevealMappingHelper();
    }

    @Override
    public void onDrawerClosed() {
        if (drawerPresenter.isChangedCurrentSelection()) {
            listTaskView.showProgressDialog(R.string.fetching_structures_title, R.string.fetching_structures_message);
            listTaskInteractor.fetchLocations(prefsUtil.getCurrentPlanId(), prefsUtil.getCurrentOperationalArea());
        }
    }

    public void refreshStructures(boolean localSyncDone) {
        if (localSyncDone) {
            setChangeMapPosition(false);
        } else {
            setChangeMapPosition(true);
        }
        listTaskView.showProgressDialog(R.string.fetching_structures_title, R.string.fetching_structures_message);
        listTaskInteractor.fetchLocations(prefsUtil.getCurrentPlanId(), prefsUtil.getCurrentOperationalArea());
    }

    @Override
    public void onStructuresFetched(JSONObject structuresGeoJson, Feature operationalArea, List<TaskDetails> taskDetailsList, String point, Boolean locationComponentActive) {
        prefsUtil.setCurrentOperationalArea(operationalArea.getStringProperty(Constants.Properties.LOCATION_NAME));
        listTaskView.setOperationalArea(prefsUtil.getCurrentOperationalArea());
        onStructuresFetched(structuresGeoJson, operationalArea, taskDetailsList);
        onAddStructureClicked(locationComponentActive, point);
    }

    @Override
    public void onStructuresFetched(JSONObject structuresGeoJson, Feature operationalArea, List<TaskDetails> taskDetailsList) {
        listTaskView.hideProgressDialog();
        setChangeMapPosition(drawerPresenter.isChangedCurrentSelection() || (drawerPresenter.isChangedCurrentSelection() && changeMapPosition));
        drawerPresenter.setChangedCurrentSelection(false);
        if (structuresGeoJson.has(FEATURES)) {
            featureCollection = FeatureCollection.fromJson(structuresGeoJson.toString());
            isTasksFiltered = false;
            if (filterParams != null && !filterParams.getCheckedFilters().isEmpty() && StringUtils.isBlank(searchPhrase)) {
                filterFeatureCollection = null;
                filterTasks(filterParams);
            } else if (filterParams != null && !filterParams.getCheckedFilters().isEmpty()) {
                searchFeatureCollection = null;
                searchTasks(searchPhrase);
            } else {
                listTaskView.setGeoJsonSource(getFeatureCollection(), operationalArea, isChangeMapPosition());
            }
            this.operationalArea = operationalArea;
            if (Utils.isEmptyCollection(getFeatureCollection().features())) {
                listTaskView.displayNotification(R.string.fetching_structures_title, R.string.no_structures_found);
            }
        } else {
            listTaskView.displayNotification(R.string.fetching_structures_title,
                    R.string.fetch_location_and_structures_failed, prefsUtil.getCurrentOperationalArea());
            try {
                structuresGeoJson.put(FEATURES, new JSONArray());
                listTaskView.setGeoJsonSource(FeatureCollection.fromJson(structuresGeoJson.toString()), operationalArea, isChangeMapPosition());
                listTaskView.clearSelectedFeature();
                listTaskView.closeCardView(R.id.btn_collapse_spray_card_view);
            } catch (JSONException e) {
                Timber.e("error resetting structures");
            }
        }

        if (taskDetailsList != null && (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA
                || BuildConfig.BUILD_COUNTRY == Country.NAMIBIA
                || BuildConfig.BUILD_COUNTRY == Country.SENEGAL)) {
            new IndicatorsCalculatorTask(listTaskView.getActivity(), taskDetailsList).execute();
        }
    }

    public void onMapReady() {
        String planId = PreferencesUtil.getInstance().getCurrentPlanId();
        String operationalArea = PreferencesUtil.getInstance().getCurrentOperationalArea();
        if (StringUtils.isNotBlank(planId) &&
                StringUtils.isNotBlank(operationalArea)) {
            listTaskInteractor.fetchLocations(planId, operationalArea);
        } else {
            listTaskView.displayNotification(R.string.select_campaign_operational_area_title, R.string.select_campaign_operational_area);
            drawerPresenter.getView().lockNavigationDrawerForSelection();
        }
    }

    public void onMapClicked(MapboxMap mapboxMap, LatLng point, boolean isLongclick) {
        double currentZoom = mapboxMap.getCameraPosition().zoom;
        if (currentZoom < getMaxZoomLevel()) {
            Timber.w("onMapClicked Current Zoom level" + currentZoom);
            listTaskView.displayToast(R.string.zoom_in_to_select);
            return;
        }
        this.clickedPoint = point;
        this.mapboxMap = mapboxMap;
        final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);
        Context context = listTaskView.getContext();
        List<Feature> features = mapboxMap.queryRenderedFeatures(pixel,
                context.getString(R.string.reveal_layer_polygons), context.getString(R.string.reveal_layer_points));
        if (features.isEmpty()) {//try to increase the click area
            RectF clickArea = new RectF(pixel.x - CLICK_SELECT_RADIUS,
                    pixel.y + CLICK_SELECT_RADIUS, pixel.x + CLICK_SELECT_RADIUS,
                    pixel.y - CLICK_SELECT_RADIUS);
            features = mapboxMap.queryRenderedFeatures(clickArea,
                    context.getString(R.string.reveal_layer_polygons), context.getString(R.string.reveal_layer_points));
            Timber.d("Selected structure after increasing click area: " + features.size());
            if (features.size() == 1) {
                onFeatureSelected(features.get(0), isLongclick);
            } else {
                Timber.d("Not Selected structure after increasing click area: " + features.size());
            }
        } else {
            onFeatureSelected(features.get(0), isLongclick);
            if (features.size() > 1) {
                Timber.w("Selected more than 1 structure: " + features.size());
            }
        }

    }

    private void onFeatureSelected(Feature feature, boolean isLongclick) {
        this.selectedFeature = feature;
        this.changeInterventionStatus = false;
        cardDetails = null;

        listTaskView.closeAllCardViews();
        listTaskView.displaySelectedFeature(feature, clickedPoint);
        if (isLongclick && (BuildConfig.BUILD_COUNTRY != Country.ZAMBIA
                || BuildConfig.BUILD_COUNTRY != Country.SENEGAL)) {
            onFeatureSelectedByLongClick(feature);
        } else {
            onFeatureSelectedByNormalClick(feature);
        }
    }

    private void onFeatureSelectedByNormalClick(Feature feature) {
        if (!feature.hasProperty(TASK_IDENTIFIER)) {
            if (isFocusInvestigation() && getPropertyValue(feature, TYPE).equals(Constants.StructureType.RESIDENTIAL)) {
                listTaskInteractor.fetchFamilyDetails(selectedFeature.id()); // check if family registered in other plan
            } else {
                listTaskView.displayNotification(listTaskView.getContext().getString(R.string.task_not_found, prefsUtil.getCurrentOperationalArea()));
            }
            return;
        }

        String businessStatus = getPropertyValue(feature, FEATURE_SELECT_TASK_BUSINESS_STATUS);
        String code = getPropertyValue(feature, TASK_CODE);
        selectedFeatureInterventionType = code;
        if ((IRS.equals(code) || MOSQUITO_COLLECTION.equals(code) || LARVAL_DIPPING.equals(code) || PAOT.equals(code) || IRS_VERIFICATION.equals(code) || REGISTER_FAMILY.equals(code))
                && (NOT_VISITED.equals(businessStatus) || businessStatus == null) || shouldOpenCDDSupervisionForm(businessStatus, code)) {
            if (validateFarStructures()) {
                validateUserLocation();
            } else {
                onLocationValidated();
            }
        } else if (IRS.equals(code) &&
                (NOT_SPRAYED.equals(businessStatus) || SPRAYED.equals(businessStatus) || NOT_SPRAYABLE.equals(businessStatus) || PARTIALLY_SPRAYED.equals(businessStatus)
                        || COMPLETE.equals(businessStatus) || NOT_ELIGIBLE.equals(businessStatus) || NOT_VISITED.equals(businessStatus))) {

            listTaskInteractor.fetchInterventionDetails(IRS, feature.id(), false);
        } else if ((MOSQUITO_COLLECTION.equals(code) || LARVAL_DIPPING.equals(code))
                && (INCOMPLETE.equals(businessStatus) || IN_PROGRESS.equals(businessStatus)
                || NOT_ELIGIBLE.equals(businessStatus) || COMPLETE.equals(businessStatus))) {
            listTaskInteractor.fetchInterventionDetails(code, feature.id(), false);
        } else if (REGISTER_FAMILY.equals(code) && NOT_ELIGIBLE.equals(businessStatus)) {
            listTaskInteractor.fetchInterventionDetails(code, feature.id(), false);
        } else if (PAOT.equals(code)) {
            listTaskInteractor.fetchInterventionDetails(code, feature.id(), false);
        } else if (isFocusInvestigationOrMDA()) {
            listTaskInteractor.fetchFamilyDetails(selectedFeature.id());
        } else if (IRS_VERIFICATION.equals(code) && isZambiaIRSLite()) {
            listTaskInteractor.fetchInterventionDetails(IRS, feature.id(), false);
        }
        else if (IRS_VERIFICATION.equals(code) && COMPLETE.equals(businessStatus)) {
            listTaskInteractor.fetchInterventionDetails(IRS_VERIFICATION, feature.id(), false);
        }
    }

    private boolean shouldOpenCDDSupervisionForm(String businessStatus, String code) {
        return CDD_SUPERVISION.equals(code) && isKenyaMDALite() && (NOT_VISITED.equals(businessStatus) || IN_PROGRESS.equals(businessStatus));
    }

    private void onFeatureSelectedByLongClick(Feature feature) {
        String businessStatus = getPropertyValue(feature, TASK_BUSINESS_STATUS);
        String code = getPropertyValue(feature, TASK_CODE);
        String status = getPropertyValue(feature, LOCATION_STATUS);

        selectedFeatureInterventionType = code;
        if (INACTIVE.name().equals(status)) {
            listTaskView.displayToast(R.string.structure_is_inactive);
        } if (isKenyaMDALite()) {
            listTaskView.displayEditCDDTaskCompleteDialog();
        } else if (NOT_VISITED.equals(businessStatus) || !feature.hasProperty(TASK_IDENTIFIER)) {
            listTaskView.displayMarkStructureInactiveDialog();
        } else {
            listTaskView.displayToast(R.string.cannot_make_structure_inactive);
        }
    }

    @Override
    public void validateUserLocation() {
        Location location = listTaskView.getUserCurrentLocation();
        if (location == null) {
            locationPresenter.requestUserLocation();
        } else {
            locationPresenter.onGetUserLocation(location);
        }
    }

    @Override
    public void onGetUserLocation(Location location) {
        Feature userFeature = getFeatureUserIsIn(location);
        if (userFeature == null || !Objects.equals(userFeature.id(), selectedFeature.id())) {
            requestUserPassword();
        }
        else {
            onLocationValidated();
        }
    }

    @Override
    public void onFilterTasksClicked() {
        listTaskView.openFilterTaskActivity(filterParams);
    }

    @Override
    public void onOpenTaskRegisterClicked() {
        listTaskView.openTaskRegister(filterParams);
    }

    @Override
    public void setTaskFilterParams(TaskFilterParams filterParams) {
        if (filterParams != null) {
            filterTasks(filterParams);
            listTaskView.setSearchPhrase(filterParams.getSearchPhrase());
        }
    }

    @Override
    public void onEventFound(Event event) {
        startForm(selectedFeature, cardDetails, selectedFeatureInterventionType, event);
    }

    @Override
    public void findLastEvent(String featureId, String eventType) {
        listTaskInteractor.findLastEvent(featureId, eventType);
    }

    public void onFociBoundaryLongClicked() {
        revealApplication.setFeatureCollection(featureCollection);
        revealApplication.setOperationalArea(operationalArea);

        Intent intent = new Intent(listTaskView.getContext(), EditFociBoundaryActivity.class);
        listTaskView.getActivity().startActivity(intent);
    }

    @Override
    public void onCDDTaskCompleteStatusEdited(String businessStatus) {
        updateFeatureTaskBusinessStatus(businessStatus);
    }

    @Override
    public void onInterventionFormDetailsFetched(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
        this.changeInterventionStatus = true;
        listTaskView.hideProgressDialog();
        if (validateFarStructures()) {
            validateUserLocation();
        } else {
            onLocationValidated();
        }
    }

    @Override
    public void onInterventionTaskInfoReset(boolean success) {
        listTaskView.hideProgressDialog();
        listTaskView.closeAllCardViews();
        if (revealApplication.isRefreshMapOnEventSaved()) {
            refreshStructures(true);
            listTaskView.clearSelectedFeature();
            revealApplication.setRefreshMapOnEventSaved(false);
        }
    }


    @Override
    public void onCardDetailsFetched(CardDetails cardDetails) {
        if (cardDetails instanceof SprayCardDetails) {
            formatSprayCardDetails((SprayCardDetails) cardDetails);
            listTaskView.openCardView(cardDetails);
        } else if (cardDetails instanceof MosquitoHarvestCardDetails) {
            listTaskView.openCardView(cardDetails);
        } else if (cardDetails instanceof IRSVerificationCardDetails) {
            listTaskView.openCardView(cardDetails);
        } else if (cardDetails instanceof FamilyCardDetails) {
            formatFamilyCardDetails((FamilyCardDetails) cardDetails);
            listTaskView.openCardView(cardDetails);
        }
    }

    private void formatSprayCardDetails(SprayCardDetails sprayCardDetails) {
        try {
            // format date
            String formattedDate = formatDate(sprayCardDetails.getSprayDate(), EVENT_DATE_FORMAT_Z);
            sprayCardDetails.setSprayDate(formattedDate);
        } catch (Exception e) {
            Timber.e(e);
            Timber.i("Date parsing failed, trying another date format");
            try {
                // try another date format
                String formattedDate = formatDate(sprayCardDetails.getSprayDate(), EVENT_DATE_FORMAT_XXX);
                sprayCardDetails.setSprayDate(formattedDate);
            } catch (Exception exception) {
                Timber.e(e);
            }
        }

        CardDetailsUtil.formatCardDetails(sprayCardDetails);
    }

    private void formatFamilyCardDetails(FamilyCardDetails familyCardDetails) {

        Date originalDate = StringUtils.isBlank(familyCardDetails.getDateCreated()) ? null :
                new Date(Long.parseLong(familyCardDetails.getDateCreated()));

        familyCardDetails.setDateCreated(formatDate(originalDate));
    }

    public void startForm(Feature feature, CardDetails cardDetails, String interventionType) {
        startForm(feature, cardDetails, interventionType, null);
    }

    public void startForm(Feature feature, CardDetails cardDetails, String interventionType, Event event) {
        String formName = jsonFormUtils.getFormName(null, interventionType);
        String sprayStatus = cardDetails == null ? null : cardDetails.getStatus();
        String familyHead = null;
        if (cardDetails instanceof SprayCardDetails) {
            familyHead = ((SprayCardDetails) cardDetails).getFamilyHead();
        }
        startForm(formName, feature, sprayStatus, familyHead, event);
    }

    private void startForm(String formName, Feature feature, String sprayStatus, String familyHead, Event event) {
        JSONObject formJson = jsonFormUtils.getFormJSON(listTaskView.getContext()
                , formName, feature, sprayStatus, familyHead);
        if (cardDetails instanceof MosquitoHarvestCardDetails && PAOT.equals(cardDetails.getInterventionType())) {
            jsonFormUtils.populatePAOTForm((MosquitoHarvestCardDetails) cardDetails, formJson);
        } else if ( cardDetails instanceof MosquitoHarvestCardDetails) {
            jsonFormUtils.populateForm(event, formJson);
        } else if (cardDetails instanceof SprayCardDetails && Country.NAMIBIA.equals(BuildConfig.BUILD_COUNTRY)) {
            jsonFormUtils.populateForm(event, formJson);
        } else if (JsonForm.SPRAY_FORM_ZAMBIA.equals(formName) || JsonForm.SPRAY_FORM_SENEGAL.equals(formName)) {
            try {
                jsonFormUtils.populateField(formJson, DISTRICT_NAME, prefsUtil.getCurrentDistrict().trim(), VALUE);
                jsonFormUtils.populateField(formJson, PROVINCE_NAME, prefsUtil.getCurrentProvince().trim(), VALUE);
            } catch (JSONException e) {
                Timber.e(e);
            }
            Map<String, JSONObject> fields = jsonFormUtils.getFields(formJson);
            jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.HEALTH_FACILITIES, fields.get(JsonForm.HFC_SEEK), prefsUtil.getCurrentDistrict());
            jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.HEALTH_FACILITIES, fields.get(JsonForm.HFC_BELONG), prefsUtil.getCurrentDistrict());
            jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.DISTRICTS, fields.get(JsonForm.DISTRICT), prefsUtil.getCurrentProvince());
            jsonFormUtils.populateForm(event, formJson);
            String dataCollector = RevealApplication.getInstance().getContext().allSharedPreferences().fetchRegisteredANM();
            if (StringUtils.isNotBlank(dataCollector)) {
                jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.SPRAY_OPERATORS, fields.get(JsonForm.SPRAY_OPERATOR_CODE),
                        dataCollector);
            }

            if(JsonForm.SPRAY_FORM_SENEGAL.equals(formName)){
                jsonFormUtils.populateCompoundStructureOptions(formJson, org.smartregister.reveal.util.Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea()));
            }
            jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.VILLAGES,fields.get(JsonForm.LOCATION_ZONE),prefsUtil.getCurrentFacility());
        } else if (JsonForm.SPRAY_FORM_REFAPP.equals(formName)) {
            jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.DATA_COLLECTORS, jsonFormUtils.getFields(formJson).get(JsonForm.DATA_COLLECTOR), prefsUtil.getCurrentDistrict());
        } else if (cardDetails instanceof SprayCardDetails && isZambiaIRSLite()) {
            jsonFormUtils.populateForm(event, formJson);
            jsonFormUtils.populateFormWithServerOptions(formName, formJson);
        } else if(isZambiaIRSLite()) {
            jsonFormUtils.populateFormWithServerOptions(formName, formJson);
        }else if(isKenyaMDALite()){
            jsonFormUtils.populateFormWithServerOptions(formName, formJson);
        }
        listTaskView.startJsonForm(formJson);
    }

    public void onChangeInterventionStatus(String interventionType) {
        if (IRS.equals(interventionType)) {
            listTaskView.showProgressDialog(R.string.fetching_structure_title, R.string.fetching_structure_message);
        } else if (MOSQUITO_COLLECTION.equals(interventionType)) {
            listTaskView.showProgressDialog(R.string.fetching_mosquito_collection_points_title, R.string.fetching_mosquito_collection_points_message);
        } else if (LARVAL_DIPPING.equals(interventionType)) {
            listTaskView.showProgressDialog(R.string.fetching_larval_dipping_points_title, R.string.fetching_larval_dipping_points_message);
        } else if (PAOT.equals(interventionType)) {
            listTaskView.showProgressDialog(R.string.fetching_paot_title, R.string.fetching_paot_message);
        }
        listTaskInteractor.fetchInterventionDetails(interventionType, selectedFeature.id(), true);
    }

    public void onUndoInterventionStatus(String interventionType) {
        listTaskView.showProgressDialog(R.string.reseting_task_title, R.string.reseting_task_msg);
        listTaskInteractor.resetInterventionTaskInfo(interventionType, selectedFeature.id());
    }

    public void saveJsonForm(String json) {
        try {
            JSONObject jsonForm = new JSONObject(json);
            String encounterType = jsonForm.getString(ENCOUNTER_TYPE);
            JSONArray fields = JsonFormUtils.getMultiStepFormFields(jsonForm);
            String validOperationalArea = JsonFormUtils.getFieldValue(fields, VALID_OPERATIONAL_AREA);
            if (Constants.REGISTER_STRUCTURE_EVENT.equals(encounterType) && StringUtils.isNotBlank(validOperationalArea)) {
                listTaskView.showProgressDialog(R.string.opening_form_title, R.string.add_structure_form_redirecting, validOperationalArea);
                Boolean locationComponentActive = Boolean.valueOf(JsonFormUtils.getFieldValue(fields, LOCATION_COMPONENT_ACTIVE));
                String point = JsonFormUtils.getFieldValue(fields, JsonForm.STRUCTURE);
                listTaskInteractor.fetchLocations(prefsUtil.getCurrentPlanId(), validOperationalArea, point, locationComponentActive);
            } else {
                listTaskView.showProgressDialog(R.string.saving_title, R.string.saving_message);
                listTaskInteractor.saveJsonForm(json);
            }
        } catch (JSONException e) {
            Timber.e(e);
            listTaskView.displayToast(R.string.error_occurred_saving_form);
        }
    }

    @Override
    public void onFormSaved(@NonNull String structureId, String taskID, @NonNull TaskStatus taskStatus, @NonNull String businessStatus, String interventionType) {
        listTaskView.hideProgressDialog();
        setChangeMapPosition(false);
        for (Feature feature : getFeatureCollection().features()) {
            if (structureId.equals(feature.id())) {
                feature.addStringProperty(TASK_BUSINESS_STATUS, businessStatus);
                feature.addStringProperty(TASK_STATUS, taskStatus.name());
                break;
            }
        }
        listTaskView.setGeoJsonSource(getFeatureCollection(), null, isChangeMapPosition());
       if(!isKenyaMDALite()){
           listTaskInteractor.fetchInterventionDetails(interventionType, structureId, false);
       }
    }

    @Override
    public void resetFeatureTasks(String structureId, Task task) {
        setChangeMapPosition(false);
        for (Feature feature : getFeatureCollection().features()) {
            if (structureId.equals(feature.id())) {
                feature.addStringProperty(TASK_IDENTIFIER, task.getIdentifier());
                feature.addStringProperty(TASK_CODE, task.getCode());
                feature.addStringProperty(TASK_BUSINESS_STATUS, task.getBusinessStatus());
                feature.addStringProperty(TASK_STATUS, task.getStatus().name());
                feature.addStringProperty(FEATURE_SELECT_TASK_BUSINESS_STATUS, task.getBusinessStatus());
                feature.removeProperty(STRUCTURE_NAME);
                break;
            }
        }
        listTaskView.setGeoJsonSource(getFeatureCollection(), null, isChangeMapPosition());
    }

    @Override
    public void onStructureAdded(Feature feature, JSONArray featureCoordinates, double zoomlevel) {
        listTaskView.closeAllCardViews();
        listTaskView.hideProgressDialog();
        getFeatureCollection().features().add(feature);
        setChangeMapPosition(false);
        listTaskView.setGeoJsonSource(getFeatureCollection(), null, isChangeMapPosition());
        try {
            clickedPoint = new LatLng(featureCoordinates.getDouble(1), featureCoordinates.getDouble(0));
            listTaskView.displaySelectedFeature(feature, clickedPoint, zoomlevel);

        } catch (JSONException e) {
            Timber.e(e, "error extracting coordinates of added structure");
        }
    }

    @Override
    public void onFormSaveFailure(String eventType) {
        listTaskView.hideProgressDialog();
        listTaskView.displayNotification(R.string.form_save_failure_title,
                SPRAY_EVENT.equals(eventType) ? R.string.spray_form_save_failure : R.string.add_structure_form_save_failure);
    }


    public void onAddStructureClicked(boolean myLocationComponentActive) {
        onAddStructureClicked(myLocationComponentActive, null);
    }

    public void onAddStructureClicked(boolean myLocationComponentActive, String point) {
        String formName = jsonFormUtils.getFormName(REGISTER_STRUCTURE_EVENT);
        try {
            JSONObject formJson = new JSONObject(jsonFormUtils.getFormString(listTaskView.getContext(), formName, null));
            revealApplication.setFeatureCollection(featureCollection);
            revealApplication.setOperationalArea(operationalArea);
            jsonFormUtils.populateField(formJson, JsonForm.SELECTED_OPERATIONAL_AREA_NAME, prefsUtil.getCurrentOperationalArea(), TEXT);
            if (StringUtils.isNotBlank(point)) {
                jsonFormUtils.populateField(formJson, JsonForm.STRUCTURE, point, VALUE);
            }
            formJson.put(LOCATION_COMPONENT_ACTIVE, myLocationComponentActive);
            listTaskView.startJsonForm(formJson);
        } catch (Exception e) {
            Timber.e(e, "error launching add structure form");
        }

    }

    @Override
    public void onLocationValidated() {
        if (markStructureIneligibleConfirmed) {
            onMarkStructureIneligibleConfirmed();
            markStructureIneligibleConfirmed = false;
        } else if (REGISTER_FAMILY.equals(selectedFeatureInterventionType)) {
            listTaskView.registerFamily();
        } else if (cardDetails == null || !changeInterventionStatus) {
            startForm(selectedFeature, null, selectedFeatureInterventionType);
        } else {
            if (IRS.equals(cardDetails.getInterventionType())) {
                if(isZambiaIRSLite()) {
                    findLastEvent(selectedFeature.id(), Constants.EventType.IRS_LITE_VERIFICATION);
                } else {
                    findLastEvent(selectedFeature.id(), SPRAY_EVENT);
                }
            } else if (MOSQUITO_COLLECTION.equals(cardDetails.getInterventionType())) {
                findLastEvent(selectedFeature.id(), MOSQUITO_COLLECTION_EVENT);
            } else if (LARVAL_DIPPING.equals(cardDetails.getInterventionType())) {
                findLastEvent(selectedFeature.id(), LARVAL_DIPPING_EVENT);
            } else {
                startForm(selectedFeature, cardDetails, selectedFeatureInterventionType);
            }
        }
    }

    @Override
    public void onPasswordVerified() {
        onLocationValidated();
    }

    @Override
    public LatLng getTargetCoordinates() {
        android.location.Location center = mappingHelper.getCenter(selectedFeature.geometry().toJson());
        return new LatLng(center.getLatitude(), center.getLongitude());
    }

    @Override
    public void requestUserPassword() {
        if (passwordDialog != null) {
            passwordDialog.show();
        }
    }

    @Override
    public ValidateUserLocationPresenter getLocationPresenter() {
        return locationPresenter;
    }

    @Override
    public Feature getSelectedFeature() {
        return selectedFeature;
    }

    @Override
    public int getInterventionLabel() {
        return org.smartregister.reveal.util.Utils.getInterventionLabel();
    }

    @Override
    public void onMarkStructureInactiveConfirmed() {
        listTaskInteractor.markStructureAsInactive(selectedFeature);

    }

    @Override
    public void onEditCDDTaskCompleteStatusConfirmed(boolean isTaskComplete) {
        listTaskInteractor.editCDDTaskCompleteStatus(selectedFeature, isTaskComplete);
    }

    @Override
    public void onStructureMarkedInactive() {
        for (Feature feature : getFeatureCollection().features()) {
            if (selectedFeature.id().equals(feature.id())) {
                feature.removeProperty(TASK_BUSINESS_STATUS);
                feature.removeProperty(TASK_IDENTIFIER);
                feature.addStringProperty(LOCATION_STATUS, INACTIVE.name());
                break;
            }
        }

        listTaskView.setGeoJsonSource(getFeatureCollection(), operationalArea, false);
    }

    @Override
    public void onMarkStructureIneligibleConfirmed() {
        listTaskInteractor.markStructureAsIneligible(selectedFeature, reasonUnEligible);
    }

    @Override
    public void onStructureMarkedIneligible() {
        updateFeatureTaskBusinessStatus(NOT_ELIGIBLE);
    }

    @Override
    public void onFamilyFound(CommonPersonObjectClient finalFamily) {
        if (finalFamily == null)
            listTaskView.displayNotification(R.string.fetch_family_failed, R.string.failed_to_find_family);
        else
            listTaskView.openStructureProfile(finalFamily);
    }


    public void onResume() {
        if (revealApplication.isRefreshMapOnEventSaved()) {
            refreshStructures(true);
            listTaskView.clearSelectedFeature();
            revealApplication.setRefreshMapOnEventSaved(false);
        }
        updateLocationComponentState();
    }

    private void updateLocationComponentState() {
        if (revealApplication.isMyLocationComponentEnabled() && !listTaskView.isMyLocationComponentActive()) {
            listTaskView.focusOnUserLocation(true);
        } else if (!revealApplication.isMyLocationComponentEnabled() && listTaskView.isMyLocationComponentActive()
                || !listTaskView.isMyLocationComponentActive()) {
            listTaskView.focusOnUserLocation(false);
            if (!isTasksFiltered && StringUtils.isBlank(searchPhrase)) {
                listTaskView.setGeoJsonSource(getFeatureCollection(), operationalArea, false);
            }
        }
    }

    public void displayMarkStructureIneligibleDialog() {

        AlertDialogUtils.displayNotificationWithCallback(listTaskView.getContext(), R.string.mark_location_ineligible,
                R.string.is_structure_eligible_for_fam_reg, R.string.eligible, R.string.not_eligible_unoccupied, R.string.not_eligible_other, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_NEGATIVE || which == BUTTON_NEUTRAL) {
                            markStructureIneligibleConfirmed = true;
                            reasonUnEligible = which == BUTTON_NEGATIVE ? listTaskView.getContext().getString(R.string.not_eligible_unoccupied) : listTaskView.getContext().getString(R.string.not_eligible_other);
                        }
                        if (validateFarStructures()) {
                            validateUserLocation();
                        } else {
                            onLocationValidated();
                        }
                        dialog.dismiss();
                    }
                });
    }


    public boolean isChangeMapPosition() {
        return changeMapPosition;
    }

    public void setChangeMapPosition(boolean changeMapPosition) {
        this.changeMapPosition = changeMapPosition;
    }

    public void filterTasks(TaskFilterParams filterParams) {
        this.filterParams = filterParams;
        if (filterParams.getCheckedFilters() == null || filterParams.getCheckedFilters().isEmpty()) {
            isTasksFiltered = false;
            listTaskView.setNumberOfFilters(0);
            return;
        }
        filterFeatureCollection = new ArrayList<>();
        Set<String> filterStatus = filterParams.getCheckedFilters().get(Filter.STATUS);
        Set<String> filterTaskCode = filterParams.getCheckedFilters().get(Filter.CODE);
        Set<String> filterInterventionUnitTasks = org.smartregister.reveal.util.Utils.getInterventionUnitCodes(filterParams.getCheckedFilters().get(Filter.INTERVENTION_UNIT));
        Pattern pattern = Pattern.compile("~");
        for (Feature feature : featureCollection.features()) {
            boolean matches = true;
            if (filterStatus != null) {
                matches = feature.hasProperty(TASK_BUSINESS_STATUS) && filterStatus.contains(feature.getStringProperty(TASK_BUSINESS_STATUS));
            }
            if (matches && filterTaskCode != null) {
                matches = matchesTaskCodeFilterList(feature, filterTaskCode, pattern);
            }
            if (matches && filterInterventionUnitTasks != null) {
                matches = matchesTaskCodeFilterList(feature, filterInterventionUnitTasks, pattern);
            }
            if (matches) {
                filterFeatureCollection.add(feature);
            }
        }
        listTaskView.setGeoJsonSource(FeatureCollection.fromFeatures(filterFeatureCollection), operationalArea, false);
        listTaskView.setNumberOfFilters(filterParams.getCheckedFilters().size());
        listTaskView.setSearchPhrase("");
        isTasksFiltered = true;
    }

    private boolean matchesTaskCodeFilterList(Feature feature, Set<String> filterList, Pattern pattern) {
        boolean matches = false;
        JsonElement taskCodes = feature.getProperty(TASK_CODE_LIST);
        if (taskCodes != null) {
            String[] array = pattern.split(taskCodes.getAsString());
            matches = CollectionUtils.containsAny(Arrays.asList(array), filterList);
        }
        return matches;

    }

    public void searchTasks(String searchPhrase) {
        if (searchPhrase.isEmpty()) {
            searchFeatureCollection = null;
            listTaskView.setGeoJsonSource(filterFeatureCollection == null ? getFeatureCollection() : FeatureCollection.fromFeatures(filterFeatureCollection), operationalArea, false);
        } else {
            List<Feature> features = new ArrayList<>();
            for (Feature feature : !Utils.isEmptyCollection(searchFeatureCollection) && searchPhrase.length() > this.searchPhrase.length() ? searchFeatureCollection : Utils.isEmptyCollection(filterFeatureCollection) ? getFeatureCollection().features() : filterFeatureCollection) {
                String structureName = feature.getStringProperty(STRUCTURE_NAME);
                String familyMemberNames = feature.getStringProperty(FAMILY_MEMBER_NAMES);
                if (org.smartregister.reveal.util.Utils.matchesSearchPhrase(structureName, searchPhrase) ||
                        org.smartregister.reveal.util.Utils.matchesSearchPhrase(familyMemberNames, searchPhrase))
                    features.add(feature);
            }
            searchFeatureCollection = features;
            listTaskView.setGeoJsonSource(FeatureCollection.fromFeatures(searchFeatureCollection), operationalArea, false);
        }
        this.searchPhrase = searchPhrase;
    }

    private FeatureCollection getFeatureCollection() {
        return isTasksFiltered && filterFeatureCollection != null ? FeatureCollection.fromFeatures(filterFeatureCollection) : featureCollection;
    }

    private Feature getFeatureUserIsIn(Location location) {
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        final PointF pixel = mapboxMap.getProjection().toScreenLocation(point);

        Context context = listTaskView.getContext();
        List<Feature> features = mapboxMap.queryRenderedFeatures(pixel,
                context.getString(R.string.reveal_layer_polygons), context.getString(R.string.reveal_layer_points));

        if(!features.isEmpty()){
            return features.get(0);
        }
        return null;
    }

    private void updateFeatureTaskBusinessStatus(String businessStatus) {
        for (Feature feature : getFeatureCollection().features()) {
            if (selectedFeature.id().equals(feature.id())) {
                feature.addStringProperty(TASK_BUSINESS_STATUS, businessStatus);
                feature.addStringProperty(FEATURE_SELECT_TASK_BUSINESS_STATUS, businessStatus);
                break;
            }
        }

        listTaskView.setGeoJsonSource(getFeatureCollection(), operationalArea, false);
    }

}
