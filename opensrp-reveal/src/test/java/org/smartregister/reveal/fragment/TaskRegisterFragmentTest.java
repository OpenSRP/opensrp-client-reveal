package org.smartregister.reveal.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowProgressDialog;
import org.robolectric.shadows.ShadowToast;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Task;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.activity.RevealJsonFormActivity;
import org.smartregister.reveal.adapter.TaskRegisterAdapter;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.presenter.TaskRegisterFragmentPresenter;
import org.smartregister.reveal.presenter.ValidateUserLocationPresenter;
import org.smartregister.reveal.shadow.DrawerMenuViewShadow;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.LocationUtils;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.view.FamilyProfileActivity;
import org.smartregister.reveal.view.FamilyRegisterActivity;
import org.smartregister.reveal.view.FilterTasksActivity;
import org.smartregister.reveal.view.StructureTasksActivity;
import org.smartregister.reveal.view.TaskRegisterActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.ona.kujaku.utils.Constants.RequestCode;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FIRST_NAME;
import static org.smartregister.reveal.util.Constants.Filter.FILTER_SORT_PARAMS;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.JSON_FORM_PARAM_JSON;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_FILTER_TASKS;

/**
 * Created by samuelgithengi on 1/27/20.
 */
@Config(shadows = {DrawerMenuViewShadow.class})
public class TaskRegisterFragmentTest extends BaseUnitTest {


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private TaskRegisterFragment fragment;

    private TaskRegisterActivity activity;

    @Mock
    private TaskRegisterFragmentPresenter presenter;

    @Mock
    private TaskRegisterAdapter taskAdapter;

    @Mock
    private RevealJsonFormUtils jsonFormUtils;

    @Mock
    private LocationUtils locationUtils;

    @Mock
    private CaseClassificationFragment caseClassificationFragment;

    @Mock
    private ValidateUserLocationPresenter locationPresenter;

    @Captor
    private ArgumentCaptor<TaskDetails> taskDetailsArgumentCaptor;


    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        fragment = new TaskRegisterFragment();
        Whitebox.setInternalState(fragment, "presenter", presenter);
        activity = Robolectric.buildActivity(TaskRegisterActivity.class).create().start().get();
        activity.setContentView(R.layout.activity_base_register);
        activity.getSupportFragmentManager().beginTransaction().add(0, fragment).commit();
    }


    @Test
    public void testOnCreate() {
        assertNotNull(fragment);
    }

    @Test
    public void testGetLayout() {
        assertEquals(R.layout.fragment_task_register, fragment.getLayout());

    }

    @Test
    public void testInitializeAdapter() {
        fragment.initializePresenter();
        assertNotNull(Whitebox.getInternalState(fragment, "presenter"));
        assertNotNull(Whitebox.getInternalState(fragment, "locationUtils"));
    }

    @Test
    public void testFilter() {
        fragment.filter("Doe", "", "", false);
        verify(presenter).searchTasks("Doe");
        assertEquals(View.VISIBLE, fragment.getSearchCancelView().getVisibility());
    }


    @Test
    public void testFilterWithEmptyFilter() {
        fragment.filter("", "", "", false);
        verify(presenter).searchTasks("");
        assertEquals(View.INVISIBLE, fragment.getSearchCancelView().getVisibility());
    }

    @Test
    public void testStartMapActivity() {
        TaskFilterParams params =TaskFilterParams.builder().searchPhrase("Doe").build();
        fragment.startMapActivity(params);
        assertTrue(activity.isFinishing());
    }


    @Test
    public void testStartMapActivityWithNullParams() {
        fragment.startMapActivity(null);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void testGetLastLocation() {
        assertNull(fragment.getLastLocation());
        Location location = new Location("Test");
        Intent intent = new Intent();
        intent.putExtra(Constants.TaskRegister.LAST_USER_LOCATION, location);
        activity.setIntent(intent);
        assertEquals(location, fragment.getLastLocation());
    }

    @Test
    public void testSetUniqueID() {
        fragment.setUniqueID("123");
        assertEquals("123", fragment.getSearchView().getText().toString());
    }

    @Test
    public void testOnViewClicked() {
        View view = fragment.getView();
        TaskDetails details = TestingUtils.getTaskDetails();
        details.setTaskCode(Constants.Intervention.REGISTER_FAMILY);
        view.setTag(R.id.task_details, details);
        fragment.onViewClicked(view);
        verify(presenter).onTaskSelected(details, false);
    }

    @Test
    public void testOnViewClickedForTaskReset() {
        View view = fragment.getView();
        TaskDetails details = TestingUtils.getTaskDetails();
        details.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        details.setTaskCode(CASE_CONFIRMATION);
        view.setTag(R.id.task_details, details);

        fragment.onViewClicked(view);
        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());

        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals(getString(R.string.choose_action), tv.getText());
    }

    @Test
    public void testSetTotalTasks() {
        Whitebox.setInternalState(fragment, "taskAdapter", taskAdapter);
        when(taskAdapter.getItemCount()).thenReturn(16);
        fragment.setTotalTasks(5);
        TextView header = fragment.getView().findViewById(R.id.header_text_display);
        assertEquals("5 structures within 25 m (16 total)", header.getText().toString());
    }

    @Test
    public void testSetTaskDetails() {
        Whitebox.setInternalState(fragment, "taskAdapter", taskAdapter);
        List<TaskDetails> taskDetailsList = Collections.singletonList(TestingUtils.getTaskDetails());
        fragment.setTaskDetails(taskDetailsList);
        verify(taskAdapter).setTaskDetails(taskDetailsList);
    }

    @Test
    public void testDisplayNotification() {
        fragment.displayNotification(R.string.saving_title, R.string.saving_message);
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        assertEquals(activity.getString(R.string.saving_message), ((TextView) dialog.findViewById(android.R.id.message)).getText());
    }

    @Test
    public void testDisplayError() {
        fragment.displayNotification(R.string.opening_form_title, R.string.error_unable_to_start_form);
        AlertDialog dialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        assertEquals(activity.getString(R.string.error_unable_to_start_form), ((TextView) dialog.findViewById(android.R.id.message)).getText());
    }

    @Test
    public void testStartForm() {
        JSONObject form = new JSONObject();
        fragment.startForm(form);
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(RevealJsonFormActivity.class, shadowOf(intent).getIntentClass());
        assertEquals(form.toString(), intent.getStringExtra(JSON_FORM_PARAM_JSON));
    }

    @Test
    public void testOnDestroy() {
        fragment.onDestroy();
        verify(presenter).onDestroy();
    }

    @Test
    public void testOnDrawerClosed() {
        fragment.onDrawerClosed();
        verify(presenter).onDrawerClosed();
    }

    @Test
    public void testGetJsonFormUtils() {
        assertNull(fragment.getJsonFormUtils());
        fragment.setJsonFormUtils(jsonFormUtils);
        assertEquals(jsonFormUtils, fragment.getJsonFormUtils());
    }


    @Test
    public void testGetUserCurrentLocation() {
        Whitebox.setInternalState(fragment, "locationUtils", locationUtils);
        Location location = new Location("test");
        when(locationUtils.getLastLocation()).thenReturn(location);
        assertEquals(location, fragment.getUserCurrentLocation());
    }


    @Test
    public void testRequestUserLocation() {
        Whitebox.setInternalState(fragment, "locationUtils", locationUtils);
        fragment.requestUserLocation();
        verify(locationUtils).checkLocationSettingsAndStartLocationServices(activity, presenter);
        assertTrue(Whitebox.getInternalState(fragment, "hasRequestedLocation"));
    }


    @Test
    public void testDisplayToast() {
        fragment.displayToast("Hello");
        Toast toast = ShadowToast.getLatestToast();
        assertEquals(Toast.LENGTH_LONG, toast.getDuration());
        assertEquals("Hello", ShadowToast.getTextOfLatestToast());
    }


    @Test
    public void testGetLocationUtils() {
        fragment.initializePresenter();
        assertNotNull(fragment.getLocationUtils());
    }

    @Test
    public void testShowProgressDialog() {
        fragment.showProgressDialog(R.string.saving_title, R.string.saving_message);
        ProgressDialog progressDialog = (ProgressDialog) ShadowProgressDialog.getLatestDialog();
        assertTrue(progressDialog.isShowing());
        assertEquals(activity.getString(R.string.saving_title), ShadowApplication.getInstance().getLatestDialog().getTitle());
    }


    @Test
    public void testHideProgressDialog() {
        fragment.showProgressDialog(R.string.saving_title, R.string.saving_message);
        ProgressDialog progressDialog = (ProgressDialog) ShadowProgressDialog.getLatestDialog();
        assertTrue(progressDialog.isShowing());
        fragment.hideProgressDialog();
        assertFalse(progressDialog.isShowing());

    }


    @Test
    public void testSetInventionType() {
        fragment.setInventionType(R.string.irs);
        assertEquals(activity.getString(R.string.irs), ((TextView) fragment.getView().findViewById(R.id.intervention_type)).getText());
    }


    @Test
    public void testRegisterFamily() {
        TaskDetails details = TestingUtils.getTaskDetails();
        fragment.registerFamily(details);
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(FamilyRegisterActivity.class, shadowOf(intent).getIntentClass());
    }

    @Test
    public void testOpenFamilyProfile() {
        TaskDetails details = TestingUtils.getTaskDetails();
        CommonPersonObjectClient person = TestingUtils.getCommonPersonObjectClient();
        fragment.openFamilyProfile(person, details);
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(FamilyProfileActivity.class, shadowOf(intent).getIntentClass());

        assertEquals(person.getColumnmaps().get(FIRST_NAME), intent.getStringExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME));
        assertEquals(details.getTaskId(), intent.getStringExtra(Constants.Properties.TASK_IDENTIFIER));
        assertEquals(details.getBusinessStatus(), intent.getStringExtra(Constants.Properties.TASK_BUSINESS_STATUS));
    }

    @Test
    public void testDisplayIndexCaseDetails() {
        Whitebox.setInternalState(activity, "caseClassificationFragment", caseClassificationFragment);
        JSONObject indexCase = new JSONObject();
        fragment.displayIndexCaseDetails(indexCase);
        verify(caseClassificationFragment).displayIndexCase(indexCase);

    }

    @Test
    public void testSetNumberOfFilters() {
        fragment.setNumberOfFilters(2);
        TextView filterTextView = fragment.getView().findViewById(R.id.filter_text_view);
        assertEquals(activity.getResources().getString(R.string.filters, 2), filterTextView.getText());
        assertEquals(activity.getResources().getDimensionPixelSize(R.dimen.filter_toggle_end_margin), filterTextView.getPaddingStart());
    }


    @Test
    public void testClearFilters() {
        fragment.clearFilter();
        TextView filterTextView = fragment.getView().findViewById(R.id.filter_text_view);
        assertEquals(activity.getResources().getString(R.string.filter), filterTextView.getText());
        assertEquals(activity.getResources().getDimensionPixelSize(R.dimen.filter_toggle_padding), filterTextView.getPaddingStart());
    }

    @Test
    public void testOpenFilterActivity() {
        fragment.openFilterActivity(null);
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(FilterTasksActivity.class, shadowOf(intent).getIntentClass());
    }

    @Test
    public void testSetSearchPhrase() {
        fragment.setSearchPhrase("H");
        assertEquals("H", fragment.getSearchView().getText().toString());
    }


    @Test
    public void testOnActivityResultGetUserLocation() {
        Whitebox.setInternalState(fragment, "hasRequestedLocation", true);
        when(presenter.getLocationPresenter()).thenReturn(locationPresenter);
        Whitebox.setInternalState(fragment, "locationUtils", locationUtils);
        fragment.onActivityResult(RequestCode.LOCATION_SETTINGS, RESULT_OK, null);
        verify(locationPresenter).waitForUserLocation();
        verify(locationUtils).requestLocationUpdates(presenter);
    }


    @Test
    public void testOnActivityResultGetUserLocationFailed() {
        Whitebox.setInternalState(fragment, "hasRequestedLocation", true);
        when(presenter.getLocationPresenter()).thenReturn(locationPresenter);
        fragment.onActivityResult(RequestCode.LOCATION_SETTINGS, RESULT_CANCELED, null);
        verify(locationPresenter).onGetUserLocationFailed();
        verify(locationUtils, never()).requestLocationUpdates(presenter);
    }

    @Test
    public void testOnActivityResultFilterTasks() {
        Intent intent = new Intent();
        TaskFilterParams params = TaskFilterParams.builder().build();
        intent.putExtra(FILTER_SORT_PARAMS, params);
        fragment.onActivityResult(REQUEST_CODE_FILTER_TASKS, RESULT_OK, intent);
        verify(presenter).filterTasks(params);
    }

    @Test
    public void testDisplayResetTaskInfoDialog() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        fragment.displayResetTaskInfoDialog(taskDetails);

        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());

        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals(getString(R.string.undo_task_msg), tv.getText());

        Whitebox.setInternalState(fragment, "presenter", presenter);
        alertDialog.getButton(BUTTON_POSITIVE).performClick();
        verify(presenter).resetTaskInfo(taskDetailsArgumentCaptor.capture());
        assertEquals(taskDetails.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
        assertFalse(alertDialog.isShowing());
    }

    @Test
    public void testDisplayTaskActionDialogForEdit() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        fragment.displayTaskActionDialog(taskDetails, mock(View.class));

        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());

        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals(getString(R.string.choose_action), tv.getText());

        Whitebox.setInternalState(fragment, "presenter", presenter);
        alertDialog.getButton(BUTTON_POSITIVE).performClick();
        verify(presenter).onTaskSelected(taskDetailsArgumentCaptor.capture(), anyBoolean());
        assertEquals(taskDetails.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
        assertFalse(alertDialog.isShowing());
    }

    @Test
    public void testDisplayTaskActionDialogForUndo() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        fragment = spy(fragment);
        fragment.displayTaskActionDialog(taskDetails, mock(View.class));

        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());

        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals(getString(R.string.choose_action), tv.getText());

        Whitebox.setInternalState(fragment, "presenter", presenter);
        alertDialog.getButton(BUTTON_NEGATIVE).performClick();
        verify(fragment).displayResetTaskInfoDialog(taskDetailsArgumentCaptor.capture());
        assertEquals(taskDetails.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
        assertFalse(alertDialog.isShowing());
    }

    @Test
    public void testOpenTasksScreen() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        fragment.openTasksScreen(taskDetails);
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertEquals(StructureTasksActivity.class, shadowOf(intent).getIntentClass());
        assertEquals(taskDetails.getStructureId(), intent.getStringExtra(Constants.Properties.LOCATION_UUID));
        assertEquals(taskDetails.getTaskId(), intent.getStringExtra(Constants.Properties.TASK_IDENTIFIER));
        assertEquals(taskDetails.getBusinessStatus(), intent.getStringExtra(Constants.Properties.TASK_BUSINESS_STATUS));
        assertEquals(taskDetails.getTaskStatus(), intent.getStringExtra(Constants.Properties.TASK_STATUS));

    }

}
