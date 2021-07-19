package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.ListTaskContract;
import org.smartregister.reveal.interactor.ListTaskInteractor;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.FamilyCardDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.SprayCardDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.PasswordDialogUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.AssetHandler;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.smartregister.reveal.interactor.ListTaskInteractorPowerMockTest.mosquitoCollectionForm;
import static org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.INCOMPLETE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.IN_PROGRESS;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_ELIGIBLE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_VISITED;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;
import static org.smartregister.reveal.util.Constants.JsonForm.SPRAY_FORM;
import static org.smartregister.reveal.util.Constants.JsonForm.THAILAND_LARVAL_DIPPING_FORM;
import static org.smartregister.reveal.util.Constants.JsonForm.THAILAND_MOSQUITO_COLLECTION_FORM;
import static org.smartregister.reveal.util.Constants.Properties.FEATURE_SELECT_TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_CODE;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;

/**
 * @author Vincent Karuri
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ListTaskPresenter.class, ValidateUserLocationPresenter.class, PasswordDialogUtils.class, PreferencesUtil.class, Utils.class, AssetHandler.class, Context.class})
public class ListTaskPresenterPowerMockTest {
    private ListTaskContract.ListTaskView listTaskViewSpy;
    private ListTaskPresenter listTaskPresenter;
    private ListTaskInteractor listTaskInteractor;
    @Captor
    private ArgumentCaptor<Boolean> isRefreshMapAfterFeatureSelectCaptor;

    @Before
    public void setUp() throws Exception {
        mockStaticMethods();

        ValidateUserLocationPresenter validateUserLocationPresenterMock = mock(ValidateUserLocationPresenter.class);
        whenNew(ValidateUserLocationPresenter.class).withAnyArguments().thenReturn(validateUserLocationPresenterMock);

        listTaskInteractor = mock(ListTaskInteractor.class);
        whenNew(ListTaskInteractor.class).withAnyArguments().thenReturn(listTaskInteractor);

        RevealJsonFormUtils jsonFormUtils = mock(RevealJsonFormUtils.class);
        when(jsonFormUtils.getFormJSON(any(), any(), any(), any(), any())).thenReturn(new JSONObject());
        listTaskViewSpy = mock(ListTaskContract.ListTaskView.class);
        when(listTaskViewSpy.getJsonFormUtils()).thenReturn(jsonFormUtils);
        listTaskPresenter = new ListTaskPresenter(listTaskViewSpy, mock(BaseDrawerContract.Presenter.class));
    }

    @Test
    public void testOnMosquitoCollectionFormSavedHidesProgressDialog() throws Exception {
        Whitebox.setInternalState(listTaskPresenter, "featureCollection", mock(FeatureCollection.class));

        listTaskPresenter.onFormSaved(null, null, null, null, IRS);
        verify(listTaskViewSpy).hideProgressDialog();
    }

    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForIRSEventWithNullCardDetails() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", Constants.Intervention.IRS);

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(SPRAY_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());

        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(SPRAY_FORM), eq(feature), any(), any());

        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }

    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForIRSEventWithCardDetails() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", Constants.Intervention.IRS);

        CardDetails cardDetails = new SprayCardDetails(null, null, null, null, null, null);

        Whitebox.setInternalState(listTaskPresenterSpy, "cardDetails", cardDetails);

        Whitebox.setInternalState(listTaskPresenterSpy, "changeInterventionStatus", true);

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(SPRAY_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());

        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(SPRAY_FORM), eq(feature), any(), any());
        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }

    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForMosquitoCollectionEventWithNullCardDetails() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", MOSQUITO_COLLECTION);

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(THAILAND_MOSQUITO_COLLECTION_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());
        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(THAILAND_MOSQUITO_COLLECTION_FORM), eq(feature), any(), any());

        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }

    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForMosquitoCollectionEventWithCardDetails() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", MOSQUITO_COLLECTION);

        CardDetails mosquitoCollectionCardDetails = new MosquitoHarvestCardDetails(null, null, null, null);

        Whitebox.setInternalState(listTaskPresenterSpy, "cardDetails", mosquitoCollectionCardDetails);

        Whitebox.setInternalState(listTaskPresenterSpy, "changeInterventionStatus", true);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(THAILAND_MOSQUITO_COLLECTION_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());

        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(THAILAND_MOSQUITO_COLLECTION_FORM), eq(feature), any(), any());

        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }

    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForLarvalDippingEventWithNullCardDetails() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", LARVAL_DIPPING);

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(THAILAND_LARVAL_DIPPING_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());

        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(THAILAND_LARVAL_DIPPING_FORM), eq(feature), any(), any());

        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }


    @Test
    public void testOnLocationValidatedCallsStartFormWithCorrectArgumentsForPAO() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);
        MosquitoHarvestCardDetails cardDetails= new MosquitoHarvestCardDetails("Avctive","2019-08-19",null,PAOT);
        Whitebox.setInternalState(listTaskPresenterSpy, "cardDetails", cardDetails);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", PAOT);

        RevealJsonFormUtils formUtils = mock(RevealJsonFormUtils.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "jsonFormUtils", formUtils);
        doReturn(JsonForm.PAOT_FORM).when(formUtils).getFormName(any(), any());

        doReturn(new JSONObject()).when(formUtils).getFormJSON(any(), any(), any(), any(), any());

        listTaskPresenterSpy.onLocationValidated();

        verify(formUtils, times(1)).getFormJSON(any(), eq(JsonForm.PAOT_FORM), eq(feature), any(), any());

        verify(listTaskViewSpy).startJsonForm(any(JSONObject.class));
    }

    @Test
    public void testOnInterventionFormDetailsFetchedShouldSetChangeSprayStatusToTrueForSprayCard() {
        assertFalse(Whitebox.getInternalState(listTaskPresenter, "changeInterventionStatus"));
        listTaskPresenter.onInterventionFormDetailsFetched(mock(SprayCardDetails.class));
        Assert.assertTrue(Whitebox.getInternalState(listTaskPresenter, "changeInterventionStatus"));
    }

    @Test
    public void testOnInterventionFormDetailsFetchedShouldSetChangeMosquitoCollectionStatusToTrueForMosquitoCollectionCard() {
        assertFalse(Whitebox.getInternalState(listTaskPresenter, "changeInterventionStatus"));
        listTaskPresenter.onInterventionFormDetailsFetched(mock(MosquitoHarvestCardDetails.class));
        Assert.assertTrue(Whitebox.getInternalState(listTaskPresenter, "changeInterventionStatus"));
    }

    @Test
    public void testOnInterventionFormDetailsFetchedEnabledPasswordValidationStatus() throws Exception {
        PowerMockito.when(Utils.validateFarStructures()).thenReturn(true);
        listTaskPresenter = spy(listTaskPresenter);
        listTaskPresenter.onInterventionFormDetailsFetched(mock(SprayCardDetails.class));
        PowerMockito.verifyPrivate(listTaskPresenter).invoke("validateUserLocation");
        PowerMockito.verifyPrivate(listTaskPresenter, never()).invoke("onLocationValidated");

    }

    @Test
    public void testOnInterventionFormDetailsFetchedDisabledPasswordValidationStatus() throws Exception {
        PowerMockito.when(Utils.validateFarStructures()).thenReturn(false);
        listTaskPresenter = spy(listTaskPresenter);
        listTaskPresenter.onInterventionFormDetailsFetched(mock(SprayCardDetails.class));
        PowerMockito.verifyPrivate(listTaskPresenter, never()).invoke("validateUserLocation");
        PowerMockito.verifyPrivate(listTaskPresenter).invoke("onLocationValidated");

    }

    @Test
    public void testOnFeatureSelectedShouldShowErrorDialogWhenTaskIdentifierIsNull() throws Exception {
        Feature feature = mock(Feature.class);

        PreferencesUtil preferencesUtil = mock(PreferencesUtil.class);
        Whitebox.setInternalState(listTaskPresenter, "prefsUtil", preferencesUtil);
        doReturn("").when(preferencesUtil).getCurrentOperationalArea();
        doNothing().when(listTaskViewSpy).displayNotification(anyString());

        final String CUSTOM_ERROR_MESSAGE = "My error message";

        Context context = mock(Context.class);
        when(listTaskViewSpy.getContext()).thenReturn(context);
        when(context.getString(anyInt(), any())).thenReturn(CUSTOM_ERROR_MESSAGE);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskViewSpy, times(1)).displayNotification(eq(CUSTOM_ERROR_MESSAGE));
    }

    @Test
    public void testfetchMosquitoCollectionDetailsIsCalledForCompleteMosquitoCollectionTask() throws Exception {
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(FEATURE_SELECT_TASK_BUSINESS_STATUS))).thenReturn(COMPLETE);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(MOSQUITO_COLLECTION);

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testfetchMosquitoCollectionDetailsIsCalledForInCompleteMosquitoCollectionTask() throws Exception {
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(FEATURE_SELECT_TASK_BUSINESS_STATUS))).thenReturn(INCOMPLETE);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(MOSQUITO_COLLECTION);

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testfetchMosquitoCollectionDetailsIsCalledForInProgressMosquitoCollectionTask() throws Exception {
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(FEATURE_SELECT_TASK_BUSINESS_STATUS))).thenReturn(IN_PROGRESS);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(MOSQUITO_COLLECTION);

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testfetchMosquitoCollectionDetailsIsCalledForNotEligibleMosquitoCollectionTask() throws Exception {
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(FEATURE_SELECT_TASK_BUSINESS_STATUS))).thenReturn(NOT_ELIGIBLE);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(MOSQUITO_COLLECTION);

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testValidateUserLocationIsCalledForInCompleteMosquitoCollectionTask() throws Exception {
        ListTaskInteractor listTaskInteractor = mock(ListTaskInteractor.class);

        ListTaskPresenter listTaskPresenter = spy(this.listTaskPresenter);

        Whitebox.setInternalState(listTaskPresenter, "listTaskInteractor", listTaskInteractor);

        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(IRS), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_BUSINESS_STATUS))).thenReturn(NOT_VISITED);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(MOSQUITO_COLLECTION);
        doReturn(mock(android.location.Location.class)).when(listTaskViewSpy).getUserCurrentLocation();

        ValidateUserLocationPresenter locationPresenter = mock(ValidateUserLocationPresenter.class);
        Whitebox.setInternalState(listTaskPresenter, "locationPresenter", locationPresenter);
        doNothing().when(locationPresenter).requestUserLocation();
        doNothing().when(locationPresenter).onGetUserLocation(any(android.location.Location.class));

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);
        when(Utils.validateFarStructures()).thenReturn(true);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        PowerMockito.verifyPrivate(listTaskPresenter, times(1)).invoke("validateUserLocation");
    }

    @Test
    public void testOpenCardViewIsCalledWithCorrectArgumentOnSprayCardDetailsFetched() {
        SprayCardDetails sprayCardDetails = new SprayCardDetails(null, null, null, null, null, null);

        doNothing().when(listTaskViewSpy).openCardView(any(CardDetails.class));
        listTaskPresenter.onCardDetailsFetched(sprayCardDetails);

        verify(listTaskViewSpy, times(1)).openCardView(eq(sprayCardDetails));
    }

    @Test
    public void testOpenCardViewIsCalledWithCorrectArgumentOnMosquitoCollectionCardDetailsFetched() {
        MosquitoHarvestCardDetails mosquitoHarvestCardDetails = new MosquitoHarvestCardDetails(null, null, null, null);

        doNothing().when(listTaskViewSpy).openCardView(any(CardDetails.class));
        listTaskPresenter.onCardDetailsFetched(mosquitoHarvestCardDetails);

        verify(listTaskViewSpy, times(1)).openCardView(eq(mosquitoHarvestCardDetails));
    }

    @Test
    public void testFetchInterventionDetailsIsCalledForChangeSprayStatus() {
        Whitebox.setInternalState(listTaskPresenter, "selectedFeature", mock(Feature.class));

        doNothing().when(listTaskViewSpy).showProgressDialog(anyInt(), anyInt());

        listTaskPresenter.onChangeInterventionStatus(IRS);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(IRS), AdditionalMatchers.or(anyString(), isNull()), eq(true));
    }

    @Test
    public void testFetchInterventionDetailsIsCalledForChangeMosquitoCollectionStatus() {
        Whitebox.setInternalState(listTaskPresenter, "selectedFeature", mock(Feature.class));

        doNothing().when(listTaskViewSpy).showProgressDialog(anyInt(), anyInt());

        listTaskPresenter.onChangeInterventionStatus(MOSQUITO_COLLECTION);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(true));
    }

    @Test
    public void testFetchInterventionDetailsIsCalledForRecordLarvalDipping() {
        Whitebox.setInternalState(listTaskPresenter, "selectedFeature", mock(Feature.class));

        doNothing().when(listTaskViewSpy).showProgressDialog(anyInt(), anyInt());

        listTaskPresenter.onChangeInterventionStatus(LARVAL_DIPPING);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(LARVAL_DIPPING), AdditionalMatchers.or(anyString(), isNull()), eq(true));
    }



    @Test
    public void testFetchInterventionDetailsIsCalledForOpenPAOTForm() {
        Whitebox.setInternalState(listTaskPresenter, "selectedFeature", mock(Feature.class));

        doNothing().when(listTaskViewSpy).showProgressDialog(anyInt(), anyInt());

        listTaskPresenter.onChangeInterventionStatus(PAOT);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(PAOT), AdditionalMatchers.or(anyString(), isNull()), eq(true));
    }

    @Test
    public void testFetchSprayDetailsIsCalledAfterSprayFormIsSaved() {
        Whitebox.setInternalState(listTaskPresenter, "featureCollection", mock(FeatureCollection.class));

        doNothing().when(listTaskViewSpy).hideProgressDialog();
        doNothing().when(listTaskViewSpy).setGeoJsonSource(any(FeatureCollection.class), any(Feature.class), anyBoolean());
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(IRS), anyString(), anyBoolean());


        listTaskPresenter.onFormSaved(null, null, null, null, IRS);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(IRS), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testFetchMosquitoCollectionDetailsIsCalledAfterMosquitoCollectionFormIsSaved() {
        Whitebox.setInternalState(listTaskPresenter, "featureCollection", mock(FeatureCollection.class));

        doNothing().when(listTaskViewSpy).hideProgressDialog();
        doNothing().when(listTaskViewSpy).setGeoJsonSource(any(FeatureCollection.class), any(Feature.class), anyBoolean());
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), anyString(), anyBoolean());

        listTaskPresenter.onFormSaved(null, null, null, null, MOSQUITO_COLLECTION);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(MOSQUITO_COLLECTION), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testChangeMapPositionIsSetToTrueWhenPresenterIsInitialized() throws Exception {

        Assert.assertTrue(listTaskPresenter.isChangeMapPosition());
    }

    @Test
    public void testChangeMapPositionIsSetToFalseOnStructureAddedIscalled() throws Exception {

        Whitebox.setInternalState(listTaskPresenter, "featureCollection", mock(FeatureCollection.class));
        Whitebox.setInternalState(listTaskPresenter, "clickedPoint", mock(LatLng.class));

        JSONArray featureCoordinates = new JSONArray("[32.64555352892119, -14.15491759447286]");

        Assert.assertTrue(listTaskPresenter.isChangeMapPosition());

        listTaskPresenter.onStructureAdded(null, featureCoordinates, 17);

        assertFalse(listTaskPresenter.isChangeMapPosition());
    }

    @Test
    public void testChangeMapPositionIsSetToFalseOnFormSavedIscalled() throws Exception {

        Whitebox.setInternalState(listTaskPresenter, "featureCollection", mock(FeatureCollection.class));
        Whitebox.setInternalState(listTaskPresenter, "clickedPoint", mock(LatLng.class));

        Assert.assertTrue(listTaskPresenter.isChangeMapPosition());

        listTaskPresenter.onFormSaved("1", null, Task.TaskStatus.COMPLETED, COMPLETE, null);

        assertFalse(listTaskPresenter.isChangeMapPosition());
    }

    @Test
    public void testChangeMapPositionIsSetToTrueWhenNonLocalSyncIsDone() throws Exception {

        listTaskPresenter.setChangeMapPosition(false);
        assertFalse(listTaskPresenter.isChangeMapPosition());

        listTaskPresenter.refreshStructures(false);

        Assert.assertTrue(listTaskPresenter.isChangeMapPosition());
    }

    @Test
    public void testChangeMapPositionIsSetToFalseWhenLocalSyncIsDone() throws Exception {

        Assert.assertTrue(listTaskPresenter.isChangeMapPosition());

        listTaskPresenter.refreshStructures(true);

        assertFalse(listTaskPresenter.isChangeMapPosition());
    }

    @Test
    public void testOnResume() {

        RevealApplication revealApplication = mock(RevealApplication.class);
        when(revealApplication.isRefreshMapOnEventSaved()).thenReturn(true);
        Whitebox.setInternalState(listTaskPresenter, "revealApplication", revealApplication);
        listTaskPresenter.onResume();
        assertFalse(listTaskPresenter.isChangeMapPosition());

        verify(listTaskViewSpy).showProgressDialog(R.string.fetching_structures_title, R.string.fetching_structures_message);
        verify(listTaskInteractor).fetchLocations(anyString(), anyString());
    }

    @Test
    public void testOnResumeMapIsRefreshedAfterFeatureSelect() {

        RevealApplication revealApplication = mock(RevealApplication.class);
        when(revealApplication.isRefreshMapOnEventSaved()).thenReturn(false);
        when(revealApplication.isRefreshMapOnEventSaved()).thenReturn(true);
        Whitebox.setInternalState(listTaskPresenter, "revealApplication", revealApplication);
        listTaskPresenter.onResume();

        verify(listTaskViewSpy).showProgressDialog(R.string.fetching_structures_title, R.string.fetching_structures_message);
        verify(listTaskInteractor).fetchLocations(anyString(), anyString());
        verify(listTaskViewSpy).clearSelectedFeature();
        verify(revealApplication).setRefreshMapOnEventSaved(isRefreshMapAfterFeatureSelectCaptor.capture());
        assertFalse(isRefreshMapAfterFeatureSelectCaptor.getValue());
    }

    @Test
    public void testSaveJsonForm() {
        String form = "{\"encounter_type\":\"my_form\"}";
        listTaskPresenter.saveJsonForm(form);
        verify(listTaskViewSpy).showProgressDialog(R.string.saving_title, R.string.saving_message);
        verify(listTaskInteractor).saveJsonForm(form);

    }

    @Test
    public void testOnFormSaveFailure() {
        listTaskPresenter.onFormSaveFailure(Constants.REGISTER_STRUCTURE_EVENT);
        verify(listTaskViewSpy).hideProgressDialog();
        verify(listTaskViewSpy).displayNotification(R.string.form_save_failure_title, R.string.add_structure_form_save_failure);
    }

    @Test
    public void testOnSprayFormSaveFailure() {
        listTaskPresenter.onFormSaveFailure(Constants.SPRAY_EVENT);
        verify(listTaskViewSpy).hideProgressDialog();
        verify(listTaskViewSpy).displayNotification(R.string.form_save_failure_title, R.string.spray_form_save_failure);

    }

    @Test
    public void testFetchIneligibleFamilyRegDetailsIsCalledForNotEligibleFamilyRegistrationTask() throws Exception {
        doNothing().when(listTaskInteractor).fetchInterventionDetails(eq(REGISTER_FAMILY), anyString(), anyBoolean());
        when(Utils.getPropertyValue(any(Feature.class), eq(FEATURE_SELECT_TASK_BUSINESS_STATUS))).thenReturn(NOT_ELIGIBLE);
        when(Utils.getPropertyValue(any(Feature.class), eq(TASK_CODE))).thenReturn(REGISTER_FAMILY);

        Feature feature = mock(Feature.class);
        doReturn(true).when(feature).hasProperty(TASK_IDENTIFIER);

        Whitebox.invokeMethod(listTaskPresenter, "onFeatureSelected", feature, false);

        verify(listTaskInteractor, times(1)).fetchInterventionDetails(eq(REGISTER_FAMILY), AdditionalMatchers.or(anyString(), isNull()), eq(false));
    }

    @Test
    public void testOpenCardViewIsCalledWithCorrectArgumentOnFamilyCardDetailsFetched() {
        FamilyCardDetails familyCardDetails = new FamilyCardDetails(null, null, null);

        doNothing().when(listTaskViewSpy).openCardView(any(CardDetails.class));
        listTaskPresenter.onCardDetailsFetched(familyCardDetails);

        verify(listTaskViewSpy, times(1)).openCardView(eq(familyCardDetails));
    }

    public void testRequestUserPassword() {
        listTaskPresenter.requestUserPassword();
        AlertDialog passwordDialog = mock(AlertDialog.class);
        Whitebox.setInternalState(listTaskInteractor, "passwordDialog", passwordDialog);
        verify(passwordDialog).show();
    }

    public void testOnPasswordVerified() throws Exception {
        listTaskPresenter = spy(listTaskPresenter);
        listTaskPresenter.onPasswordVerified();
        PowerMockito.verifyPrivate(listTaskPresenter).invoke("onLocationValidated");
    }

    @Test
    public void testOnLocationValidatedCallsDisplaysMarkStructureIneligibleDialog() {
        mockStaticMethods();

        ListTaskPresenter listTaskPresenterSpy = spy(listTaskPresenter);

        Feature feature = mock(Feature.class);
        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeature", feature);

        doNothing().when(listTaskViewSpy).startJsonForm(any(JSONObject.class));

        Whitebox.setInternalState(listTaskPresenterSpy, "selectedFeatureInterventionType", REGISTER_FAMILY);

        Whitebox.setInternalState(listTaskPresenterSpy, "markStructureIneligibleConfirmed", true);

        listTaskPresenterSpy.onLocationValidated();

        verify(listTaskPresenterSpy).onMarkStructureIneligibleConfirmed();
    }

    @Test
    public void testOnUndoingInterventionStatus() {
        Whitebox.setInternalState(listTaskPresenter, "selectedFeature", mock(Feature.class));

        doNothing().when(listTaskViewSpy).showProgressDialog(anyInt(), anyInt());

        listTaskPresenter.onUndoInterventionStatus(BEDNET_DISTRIBUTION);

        verify(listTaskInteractor, times(1)).resetInterventionTaskInfo(eq(BEDNET_DISTRIBUTION), any());
    }


    private void mockStaticMethods() {
        mockStatic(Utils.class);
        mockStatic(PreferencesUtil.class);
        mockStatic(PasswordDialogUtils.class);
        mockStatic(AssetHandler.class);

        when(AssetHandler.readFileFromAssetsFolder(AdditionalMatchers.or(isNull(), anyString()), AdditionalMatchers.or(isNull(), any(Context.class)))).thenReturn(mosquitoCollectionForm);

        PreferencesUtil preferencesUtil = mock(PreferencesUtil.class);
        PowerMockito.when(preferencesUtil.getCurrentOperationalArea()).thenReturn(new JSONObject().toString());
        PowerMockito.when(PreferencesUtil.getInstance()).thenReturn(preferencesUtil);
        PowerMockito.when(preferencesUtil.getCurrentPlanId()).thenReturn(new JSONObject().toString());

        PowerMockito.when(Utils.getOperationalAreaLocation(anyString())).thenReturn(new Location());
    }


}