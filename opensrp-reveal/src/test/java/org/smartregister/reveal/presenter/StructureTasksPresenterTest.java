package org.smartregister.reveal.presenter;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.StructureTasksContract;
import org.smartregister.reveal.interactor.BaseFormFragmentInteractor;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by samuelgithengi on 4/18/19.
 */
public class StructureTasksPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private StructureTasksContract.View view;

    @Mock
    private StructureTasksContract.Interactor interactor;

    @Mock
    private PreferencesUtil prefsUtil;

    @Captor
    private ArgumentCaptor<StructureTaskDetails> taskDetailsArgumentCaptor;

    private Context context = RuntimeEnvironment.application;

    private StructureTasksPresenter presenter;


    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        presenter = new StructureTasksPresenter(view, context, interactor, prefsUtil);
    }

    @Test
    public void testFindTasks() {
        String planId = UUID.randomUUID().toString();
        String structureId = UUID.randomUUID().toString();
        String jurisdictionId = UUID.randomUUID().toString();
        when(prefsUtil.getCurrentPlanId()).thenReturn(planId);
        when(prefsUtil.getCurrentOperationalArea()).thenReturn(jurisdictionId);
        Location jurisdiction = new Location();
        jurisdiction.setId(jurisdictionId);
        Cache<Location> cache = mock(Cache.class);
        when(cache.get(anyString(), any())).thenReturn(jurisdiction);
        Whitebox.setInternalState(Utils.class, "cache",cache);
        presenter.findTasks(structureId);
        verify(interactor).findTasks(structureId, planId, jurisdictionId);
        verify(prefsUtil).getCurrentPlanId();
    }

    @Test
    public void testRefreshTasksWithNoStructureId(){
        presenter.refreshTasks();
        verify(interactor, never()).findTasks(anyString(), anyString(), anyString());
    }

    @Test
    public void testRefreshTasks(){
        String planId = UUID.randomUUID().toString();
        String structureId = "UUID.randomUUID().toString()";
        String jurisdictionId = UUID.randomUUID().toString();
        when(prefsUtil.getCurrentPlanId()).thenReturn(planId);
        when(prefsUtil.getCurrentOperationalArea()).thenReturn(jurisdictionId);
        Location jurisdiction = new Location();
        jurisdiction.setId(jurisdictionId);
        Cache<Location> cache = mock(Cache.class);
        when(cache.get(anyString(), any())).thenReturn(jurisdiction);
        Whitebox.setInternalState(Utils.class, "cache", cache);
        Whitebox.setInternalState(presenter, "structureId", structureId);
        presenter.refreshTasks();
        verify(interactor).findTasks(structureId, planId, jurisdictionId);
        verify(prefsUtil).getCurrentPlanId();
    }

    @Test
    public void testOnTasksFound() {
        List<StructureTaskDetails> taskDetailsList = Collections.singletonList(TestingUtils.getStructureTaskDetails());
        presenter.onTasksFound(taskDetailsList, eq(null));
        verify(view).setTaskDetailsList(taskDetailsList);
    }

    @Test
    public void testOnTasksFoundWithIndexCase() {
        List<StructureTaskDetails> taskDetailsList = Collections.singletonList(TestingUtils.getStructureTaskDetails());
        presenter.onTasksFound(taskDetailsList, TestingUtils.getStructureTaskDetails());
        verify(view).setTaskDetailsList(taskDetailsList);
        verify(view).displayDetectCaseButton();
    }

    @Test
    public void testOnTaskSelectedFetchesStructureDetails() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        presenter.onTaskSelected(task, false, false);
        verify(interactor).getStructure(task);
        verify(view).showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
    }


    @Test
    public void testOnTaskSelectedCompletedTaskDisplayMessage() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        presenter.onTaskSelected(task, false, false);
        verify(interactor, never()).getStructure(task);
        verify(view, never()).showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
        verify(view).displayToast(anyString());
    }

    @Test
    public void testUndoCompleteTaskSelectedDisplaysTaskInfoDialog() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        presenter.onTaskSelected(task, false, true);
        verify(view).displayResetTaskInfoDialog(taskDetailsArgumentCaptor.capture());
        assertEquals(task.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
    }

    @Test
    public void testEditCompleteTaskSelectedDisplaysTaskInfoDialog() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        task.setEdit(false);
        presenter.onTaskSelected(task, true, false);
        verify(view).showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
        verify(interactor).getStructure(taskDetailsArgumentCaptor.capture());
        assertEquals(task.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
        assertTrue(taskDetailsArgumentCaptor.getValue().isEdit());
    }

    @Test
    public void testOnTaskSelectedBCCTaskGetStructureDetails() {
        StructureTaskDetails task = TestingUtils.getStructureTaskDetails();
        task.setTaskCode(Intervention.BCC);
        presenter.onTaskSelected(task, false, false);
        verify(interactor).getStructure(task);
        verify(view).showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
    }


    @Test
    public void testGetView() {
        assertEquals(view, presenter.getView());
    }


    @Test
    public void testSaveJsonForm() {
        String form = "{\"id\":\"dgfdfsfssd\"};";
        presenter.saveJsonForm(form);
        verify(interactor).saveJsonForm(form);
        verify(view).showProgressDialog(R.string.saving_title, R.string.saving_message);
    }

    @Test
    public void testOnFormSaved() {
        String structure = "234324323";
        String taskId = UUID.randomUUID().toString();
        presenter.onFormSaved(structure, taskId, Task.TaskStatus.COMPLETED, Constants.BusinessStatus.NOT_SPRAYED, Intervention.IRS);
        verify(view).hideProgressDialog();
        verify(view).updateTask(taskId, Task.TaskStatus.COMPLETED, Constants.BusinessStatus.NOT_SPRAYED);
    }


    @Test
    public void testOnStructureAdded() {
        presenter.onStructureAdded(null, null, 17);
        verifyNoMoreInteractions(interactor);
        verifyNoMoreInteractions(view);
    }

    @Test
    public void testOnFamilyFound() {
        presenter.onFamilyFound(null);
        verifyNoMoreInteractions(interactor);
        verifyNoMoreInteractions(view);
    }


    @Test
    public void testOnFormSaveFailure() {
        presenter.onFormSaveFailure(null);
        verify(view).hideProgressDialog();

    }

    @Test
    public void testOnDetectCase() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        String structureId = UUID.randomUUID().toString();
        Whitebox.setInternalState(presenter, "indexCase", taskDetails);
        Whitebox.setInternalState(presenter, "structureId", structureId);
        presenter.onDetectCase();
        verify(interactor).getStructure(taskDetailsArgumentCaptor.capture());
        assertEquals(structureId, taskDetailsArgumentCaptor.getValue().getStructureId());
    }

    @Test
    public void testOnIndexConfirmationFormSaved() {
        String taskId = UUID.randomUUID().toString();
        Task indexCase = TestingUtils.getTask(taskId);
        presenter.onIndexConfirmationFormSaved(taskId, Task.TaskStatus.COMPLETED, Constants.BusinessStatus.NOT_SPRAYED, Collections.singleton(indexCase));
        verify(view).hideDetectCaseButton();
        verify(view).updateNumberOfTasks();
        verify(view).hideProgressDialog();
        verify(view).updateTasks(taskId, Task.TaskStatus.COMPLETED, Constants.BusinessStatus.NOT_SPRAYED, Collections.singleton(indexCase));
    }

    @Test
    public void testOnIndexConfirmationFormSavedCaseConfirmationNotComplete() {
        String taskId = UUID.randomUUID().toString();
        presenter.onIndexConfirmationFormSaved(taskId, Task.TaskStatus.IN_PROGRESS, Constants.BusinessStatus.NOT_SPRAYED, Collections.EMPTY_SET);
        verify(view, never()).hideDetectCaseButton();
        verify(view).hideProgressDialog();
        verify(view).updateTasks(taskId, Task.TaskStatus.IN_PROGRESS, Constants.BusinessStatus.NOT_SPRAYED, Collections.EMPTY_SET);
    }

    @Test
    public void testResetTaskInfo() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        presenter.resetTaskInfo(taskDetails);
        verify(interactor).resetTaskInfo(any(), taskDetailsArgumentCaptor.capture());
        assertEquals(taskDetails.getTaskId(), taskDetailsArgumentCaptor.getValue().getTaskId());
    }

    @Test
    public void testOnTaskInfoReset() {
        String planId = UUID.randomUUID().toString();
        String structureId = UUID.randomUUID().toString();
        String jurisdictionId = UUID.randomUUID().toString();
        when(prefsUtil.getCurrentPlanId()).thenReturn(planId);
        when(prefsUtil.getCurrentOperationalArea()).thenReturn(jurisdictionId);

        Location jurisdiction = new Location();
        jurisdiction.setId(jurisdictionId);
        Cache<Location> cache = mock(Cache.class);
        when(cache.get(anyString(), any())).thenReturn(jurisdiction);
        Whitebox.setInternalState(Utils.class, "cache",cache);

        presenter.onTaskInfoReset(structureId);
        verify(interactor).findTasks(structureId, planId, jurisdictionId);
    }


    @Test
    public void testLocationValidatedForFamilyRegisterTask() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        presenter.setTaskDetails(taskDetails);
        presenter.onLocationValidated();
        verify(view).registerFamily(taskDetails);
    }

    @Test
    public void testLocationValidatedForEdit() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        taskDetails.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        taskDetails.setEdit(true);
        taskDetails.setTaskCode(Constants.Intervention.BEDNET_DISTRIBUTION);
        presenter.setTaskDetails(taskDetails);
        presenter.onLocationValidated();
        verify(interactor).findLastEvent(taskDetails);
    }

    @Test
    public void testLocationValidatedForCompletedRegisteration() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        taskDetails.setTaskStatus(Task.TaskStatus.COMPLETED.name());
        presenter.setTaskDetails(taskDetails);
        presenter.onLocationValidated();
        verify(view).hideProgressDialog();
    }

    @Test
    public void testOnEventFoundError() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        RevealJsonFormUtils jsonFormUtils = PowerMockito.mock(RevealJsonFormUtils.class);
        when(view.getJsonFormUtils()).thenReturn(jsonFormUtils);
        presenter.setTaskDetails(taskDetails);
        presenter.onEventFound(null);
        verify(view).displayError(R.string.opening_form_title, R.string.form_not_found);
        verify(view).hideProgressDialog();
    }

    @Test
    public void testOnEventBednetDistribution() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        taskDetails.setTaskCode(Intervention.BEDNET_DISTRIBUTION);
        RevealJsonFormUtils jsonFormUtils = PowerMockito.mock(RevealJsonFormUtils.class);
        JSONObject json = mock(JSONObject.class);
        BaseFormFragmentInteractor formInteractor = mock(BaseFormFragmentInteractor.class);
        Whitebox.setInternalState(presenter, "formInteractor", formInteractor);
        when(view.getJsonFormUtils()).thenReturn(jsonFormUtils);
        when(jsonFormUtils.getFormJSON(any(), any(), any(), any())).thenReturn(json);
        when(jsonFormUtils.getFormName(any(), any())).thenReturn("json");
        presenter.setTaskDetails(taskDetails);
        presenter.onEventFound(null);
        verify(formInteractor).findNumberOfMembers(taskDetails.getTaskEntity(), json);
        verify(view, never()).startForm(json);
        verify(view).hideProgressDialog();
    }

    @Test
    public void testOnEventOpenForm() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        RevealJsonFormUtils jsonFormUtils = PowerMockito.mock(RevealJsonFormUtils.class);
        JSONObject json = mock(JSONObject.class);
        when(view.getJsonFormUtils()).thenReturn(jsonFormUtils);
        when(jsonFormUtils.getFormJSON(any(), any(), any(), any())).thenReturn(json);
        when(jsonFormUtils.getFormName(any(), any())).thenReturn("json");
        presenter.setTaskDetails(taskDetails);
        presenter.onEventFound(null);
        verify(view).startForm(json);
        verify(view).hideProgressDialog();
    }

}
