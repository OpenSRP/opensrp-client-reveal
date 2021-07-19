package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.domain.Location;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.BaseFormFragmentContract;
import org.smartregister.reveal.interactor.BaseFormFragmentInteractor;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.repository.RevealMappingHelper;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.family.util.Constants.JSON_FORM_KEY.OPTIONS;
import static org.smartregister.reveal.util.Constants.JsonForm.CASE_CONFIRMATION_FORM;

/**
 * Created by samuelgithengi on 4/18/19.
 */
public class FormFragmentPresenterTest extends BaseUnitTest {


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BaseFormFragmentContract.View view;

    @Mock
    private RevealJsonFormUtils jsonFormUtils;

    @Mock
    private RevealMappingHelper mappingHelper;

    @Mock
    private AlertDialog passwordDialog;

    @Mock
    private BaseFormFragmentInteractor interactor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonArgumentCaptor;

    private Context context = RuntimeEnvironment.application;

    private BaseFormFragmentPresenter presenter;

    private StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();

    @Mock
    private Location structure;


    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        presenter = new BaseFormFragmentPresenter(view, context);
        presenter.mappingHelper = mappingHelper;
        Whitebox.setInternalState(presenter, "taskDetails", taskDetails);
        Whitebox.setInternalState(presenter, "structure", structure);
        Whitebox.setInternalState(presenter, "passwordDialog", passwordDialog);
        Whitebox.setInternalState(presenter, "interactor", interactor);
        when(view.getJsonFormUtils()).thenReturn(jsonFormUtils);

    }

    @Test
    public void testOnPasswordVerifiedStartsForm() {
        taskDetails.setTaskCode(Constants.Intervention.BLOOD_SCREENING);
        when(jsonFormUtils.getFormName(null, taskDetails.getTaskCode())).thenReturn(Constants.JsonForm.BLOOD_SCREENING_FORM);
        presenter.onPasswordVerified();
        verify(view).startForm(null);

    }

    @Test
    public void testGetTargetCoordinates() {
        android.location.Location location = new android.location.Location("test");
        when(mappingHelper.getCenter(anyString())).thenReturn(location);
        LatLng target = presenter.getTargetCoordinates();
        assertEquals(location.getLatitude(), target.getLatitude(), 0.00001);
        assertEquals(location.getLongitude(), target.getLongitude(), 0.00001);

    }

    @Test
    public void testRequestUserPassword() {
        presenter.requestUserPassword();
        verify(passwordDialog).show();
    }


    @Test
    public void testOnOpenBednetFormPopulatesNumberOfMembers() {
        taskDetails.setTaskCode(Constants.Intervention.BEDNET_DISTRIBUTION);
        when(jsonFormUtils.getFormName(null, taskDetails.getTaskCode())).thenReturn(Constants.JsonForm.BEDNET_DISTRIBUTION_FORM);
        presenter.onLocationValidated();
        verify(view, never()).startForm(null);
        verify(interactor).findNumberOfMembers(taskDetails.getTaskEntity(), null);
    }


    @Test
    public void testOnFoundMembersCount() throws JSONException {
        presenter.onFetchedMembersCount(new Pair<>(3, 1), new JSONObject("{\"members\": \"[num_fam_members]\", \"sleep_outdoors\":\"[num_sleeps_outdoors]\"}"));
        verify(view).startForm(jsonArgumentCaptor.capture());
        verify(view).hideProgressDialog();
        assertEquals(3, jsonArgumentCaptor.getValue().getInt("members"));
        assertEquals(1, jsonArgumentCaptor.getValue().getInt("sleep_outdoors"));
    }

    @Test
    public void testOnFoundMembersCountExceptionHandling() {
        JSONObject mockedForm = Mockito.mock(JSONObject.class);
        doAnswer(invocation -> {
            throw new JSONException("Hey! I crash!");
        }).when(mockedForm).toString();
        presenter.onFetchedMembersCount(new Pair<>(3, 1), mockedForm);
        verify(view).startForm(jsonArgumentCaptor.capture());
        verify(view).hideProgressDialog();
    }

    @Test
    public void testShowBasicForm() {
        Whitebox.setInternalState(presenter, "jsonFormUtils", jsonFormUtils);
        JSONObject expectedJson = new JSONObject();
        when(jsonFormUtils.getFormJSON(any(), anyString(), any(), any())).thenReturn(expectedJson);
        presenter.showBasicForm("form");
        verify(view).startForm(jsonArgumentCaptor.capture());
        assertEquals(expectedJson, jsonArgumentCaptor.getValue());
    }

    @Test
    public void testGetLocationPresenter() {
        assertNotNull(presenter.getLocationPresenter());
    }

    @Test
    public void testSetTaskDetails() {
        presenter.setTaskDetails(taskDetails);
        assertEquals(taskDetails, presenter.getTaskDetails());
    }

    @Test
    public void testOnFetchedFamilyMembers() throws JSONException {
        JSONArray familyMembers = new JSONArray();
        familyMembers.put(new JSONObject("{\"key\":\"35rfdsfdsf-sdfdsm\"}"));
        JSONObject formJSON = new JSONObject(AssetHandler.readFileFromAssetsFolder(CASE_CONFIRMATION_FORM, context));
        presenter.onFetchedFamilyMembers(familyMembers, formJSON);
        verify(view).startForm(jsonArgumentCaptor.capture());
        assertEquals(familyMembers, JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(jsonArgumentCaptor.getValue()), Constants.JsonForm.FAMILY_MEMBER).getJSONArray(OPTIONS));

    }

    @Test
    public void testOnStructureFound() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        taskDetails.setTaskCode(Constants.Intervention.BLOOD_SCREENING);
        presenter = spy(presenter);
        presenter.onStructureFound(null, taskDetails);
        verify(presenter).validateFarStructures();

    }


    @Test
    public void testOnStructureFoundInvalidTask() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        taskDetails.setTaskCode("UnknownCode");
        presenter = spy(presenter);
        presenter.onStructureFound(null, taskDetails);
        verify(presenter, never()).validateFarStructures();
        verify(presenter).onLocationValidated();

    }


    @Test
    public void testOnLocationValidatedFetchesMemberDetails() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        taskDetails.setStructureId(UUID.randomUUID().toString());
        taskDetails.setTaskCode(Constants.Intervention.CASE_CONFIRMATION);
        Whitebox.setInternalState(presenter, "taskDetails", taskDetails);
        when(jsonFormUtils.getFormName(null, taskDetails.getTaskCode())).thenReturn(CASE_CONFIRMATION_FORM);
        JSONObject form = new JSONObject();
        when(jsonFormUtils.getFormJSON(context, CASE_CONFIRMATION_FORM, taskDetails, structure)).thenReturn(form);
        presenter.onLocationValidated();
        verify(interactor).findMemberDetails(taskDetails.getStructureId(), form);
        verify(view, never()).hideProgressDialog();

    }

    @Test
    public void testOnLocationValidatedDisplaysWarning() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        Whitebox.setInternalState(presenter, "taskDetails", taskDetails);
        presenter.onLocationValidated();
        verify(view).displayError(R.string.opening_form_title, R.string.form_not_found);
        verify(view).hideProgressDialog();

    }

    @Test
    public void testOnFetchedSprayDetails() {
        JSONObject form = new JSONObject();
        CommonPersonObject commonPersonObject = new CommonPersonObject("case-id",
                "relation-id", null, "IRS");
        presenter.onFetchedSprayDetails(commonPersonObject, form);
        verify(jsonFormUtils).populateSprayForm(commonPersonObject, form);
        verify(view).startForm(form);
    }


}
