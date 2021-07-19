package org.smartregister.reveal.interactor;

import android.location.Location;

import androidx.core.util.Pair;

import net.sqlcipher.Cursor;
import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

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
import org.robolectric.RuntimeEnvironment;
import org.smartregister.domain.Task;
import org.smartregister.repository.StructureRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.contract.TaskRegisterFragmentContract;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestingUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.smartregister.domain.Task.TaskStatus.ARCHIVED;
import static org.smartregister.domain.Task.TaskStatus.CANCELLED;
import static org.smartregister.family.util.DBConstants.KEY.FIRST_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.COMPLETED_TASK_COUNT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FAMILY_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.GROUPED_STRUCTURE_TASK_CODE_AND_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LATITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LONGITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_OTHER_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.REFERENCE_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAY_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_COUNT;
import static org.smartregister.reveal.util.Constants.Intervention.FI;
import static org.smartregister.reveal.util.Constants.Properties.FAMILY_MEMBER_NAMES;
import static org.smartregister.reveal.util.FamilyConstants.DatabaseKeys.HOUSE_NUMBER;

/**
 * Created by samuelgithengi on 3/27/19.
 */
public class TaskRegisterFragmentInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private TaskRegisterFragmentContract.Presenter presenter;

    @Mock
    private SQLiteDatabase database;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private InteractorUtils interactorUtils;

    @Captor
    private ArgumentCaptor<List<TaskDetails>> taskListCaptor;

    @Captor
    private ArgumentCaptor<Integer> structuresCaptor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonCaptor;

    @Captor
    private ArgumentCaptor<TaskDetails> taskDetailArgumentCaptor;

    private TaskRegisterFragmentInteractor interactor;

    private String groupId;
    private String planId;
    private String mainSelectQuery;
    private String nonRegisteredStructureTasksQuery;
    private String groupedRegisteredStructureTasksSelectQuery;
    private String bccSelectQuery;
    private String indexSelectQuery;

    @Before
    public void setUp() {
        interactor = new TaskRegisterFragmentInteractor(presenter, 70f);
        Whitebox.setInternalState(interactor, "structureRepository", structureRepository);
        Whitebox.setInternalState(interactor, "database", database);
        Whitebox.setInternalState(interactor, "interactorUtils", interactorUtils);
        groupId = UUID.randomUUID().toString();
        planId = UUID.randomUUID().toString();
        mainSelectQuery = "Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.reason_reference , structure.latitude , structure.longitude , structure.name , sprayed_structures.structure_name , sprayed_structures.family_head_name , sprayed_structures.spray_status , sprayed_structures.not_sprayed_reason , sprayed_structures.not_sprayed_other_reason , structure._id AS structure_id , ec_family.first_name , ec_family.house_number FROM task  JOIN structure ON task.for = structure._id   LEFT JOIN sprayed_structures ON task.for = sprayed_structures.id   LEFT JOIN ec_family ON structure._id = ec_family.structure_id  WHERE task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?) ";
        nonRegisteredStructureTasksQuery = " SELECT grouped_tasks.* , SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END ) AS completed_task_count , COUNT(_id ) AS task_count, GROUP_CONCAT(code || \"-\" || business_status ) AS grouped_structure_task_code_and_status , 'non_registered' as family_member_names  FROM ( Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.reason_reference , structure.latitude , structure.longitude , structure.name , sprayed_structures.structure_name , sprayed_structures.family_head_name , sprayed_structures.spray_status , sprayed_structures.not_sprayed_reason , sprayed_structures.not_sprayed_other_reason , structure._id AS structure_id , ec_family.first_name , ec_family.house_number FROM task  JOIN structure ON task.for = structure._id   LEFT JOIN sprayed_structures ON task.for = sprayed_structures.id   LEFT JOIN ec_family ON structure._id = ec_family.structure_id  WHERE task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?)   AND ec_family.structure_id IS NULL  AND task.code != 'Bednet Distribution' ) AS grouped_tasks GROUP BY structure_id ";
        groupedRegisteredStructureTasksSelectQuery = " SELECT grouped_tasks.* , SUM(CASE WHEN status='COMPLETED' THEN 1 ELSE 0 END ) AS completed_task_count , COUNT(_id ) AS task_count, GROUP_CONCAT(code || \"-\" || business_status ) AS grouped_structure_task_code_and_status , GROUP_CONCAT(family_member_names) as family_member_names  FROM ( Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.reason_reference , structure.latitude , structure.longitude , structure.name , sprayed_structures.structure_name , sprayed_structures.family_head_name , sprayed_structures.spray_status , sprayed_structures.not_sprayed_reason , sprayed_structures.not_sprayed_other_reason , structure._id AS structure_id , ec_family.first_name , ec_family.house_number , ec_family_member.first_name||' '||ec_family_member.last_name as family_member_names  FROM task  JOIN structure ON task.structure_id = structure._id   JOIN ec_family ON structure._id = ec_family.structure_id AND ec_family.date_removed IS NULL COLLATE NOCASE  JOIN ec_family_member ON ec_family.base_entity_id = ec_family_member.relational_id AND ec_family_member.date_removed IS NULL  COLLATE NOCASE  LEFT JOIN sprayed_structures ON task.for = sprayed_structures.id  WHERE task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?)  ) AS grouped_tasks GROUP BY structure_id ";
        bccSelectQuery = "SELECT * FROM task WHERE for = ? AND plan_id = ? AND code ='BCC' AND status NOT IN (?,?)";
        indexSelectQuery = "SELECT * FROM task WHERE group_id = ? AND plan_id = ? AND status NOT IN (?,?) AND code = ? ";
    }

    @Test
    public void testFindTasksWithNullParams() {
        interactor.findTasks(null, null, null, "House");
        verify(presenter).onTasksFound(null, 0);
        verifyNoMoreInteractions(presenter);
        verifyNoMoreInteractions(database);
    }


    @Test
    public void testFindTasksWithOperationalAreaLocation() {
        PreferencesUtil.getInstance().setCurrentPlan("FI_2019_TV01_IRS");
        Pair<String, String[]> pair = new Pair<>("task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?)", new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        Location center = new Location("Test");
        center.setLatitude(-14.152197);
        center.setLongitude(32.643570);
        String taskId = UUID.randomUUID().toString();
        String bccTask = UUID.randomUUID().toString();
        when(database.rawQuery(mainSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(taskId, Intervention.IRS));
        when(database.rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(bccTask, Intervention.BCC));
        when(database.rawQuery(indexSelectQuery, new String[]{groupId, planId, CANCELLED.name(), Intervention.CASE_CONFIRMATION})).thenReturn(createEmptyCursor());
        interactor.findTasks(pair, null, center, "House");
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(mainSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onTasksFound(taskListCaptor.capture(), structuresCaptor.capture());
        verifyNoMoreInteractions(presenter);
        assertEquals(2, taskListCaptor.getValue().size());
        TaskDetails taskDetails = taskListCaptor.getValue().get(1);
        assertEquals(taskId, taskDetails.getTaskId());
        assertEquals(Intervention.IRS, taskDetails.getTaskCode());
        assertEquals(BusinessStatus.NOT_SPRAYED, taskDetails.getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED.name(), taskDetails.getTaskStatus());
        assertEquals(-14.15191915, taskDetails.getLocation().getLatitude(), 0.00001);
        assertEquals(32.64302015, taskDetails.getLocation().getLongitude(), 0.00001);
        assertEquals("Structure 976", taskDetails.getStructureName());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Partial Spray", taskDetails.getSprayStatus());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Not Completed", taskDetails.getTaskDetails());
        assertEquals(66.850830078125, taskDetails.getDistanceFromUser(), 0.00001);
        assertEquals(1, structuresCaptor.getValue().intValue());

        TaskDetails bccTaskDetails = taskListCaptor.getValue().get(0);
        assertEquals(bccTask, bccTaskDetails.getTaskId());
        assertEquals(Intervention.BCC, bccTaskDetails.getTaskCode());
        assertEquals(-2, bccTaskDetails.getDistanceFromUser(), 0);
    }

    @Test
    public void testFindTasksWithUserLocation() {
        PreferencesUtil.getInstance().setCurrentPlan("FI_2019_TV01_IRS");
        Pair<String, String[]> pair = new Pair<>("task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?)", new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        Location userLocation = new Location("Test");
        userLocation.setLatitude(-14.987197);
        userLocation.setLongitude(32.076570);
        String memberTask = UUID.randomUUID().toString();
        String bccTask = UUID.randomUUID().toString();
        when(database.rawQuery(mainSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(memberTask, Intervention.BLOOD_SCREENING));
        when(database.rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(bccTask, Intervention.BCC));
        when(database.rawQuery(indexSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name(), Intervention.CASE_CONFIRMATION})).thenReturn(createEmptyCursor());
        interactor.findTasks(pair, userLocation, null, "House");
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(mainSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onTasksFound(taskListCaptor.capture(), structuresCaptor.capture());
        verifyNoMoreInteractions(presenter);
        assertEquals(2, taskListCaptor.getValue().size());
        TaskDetails taskDetails = taskListCaptor.getValue().get(1);
        assertEquals(memberTask, taskDetails.getTaskId());
        assertEquals(Intervention.BLOOD_SCREENING, taskDetails.getTaskCode());
        assertEquals(BusinessStatus.NOT_SPRAYED, taskDetails.getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED.name(), taskDetails.getTaskStatus());
        assertEquals(-14.15191915, taskDetails.getLocation().getLatitude(), 0.00001);
        assertEquals(32.64302015, taskDetails.getLocation().getLongitude(), 0.00001);
        assertEquals("Structure 976", taskDetails.getStructureName());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Partial Spray", taskDetails.getSprayStatus());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Not Completed", taskDetails.getTaskDetails());
        assertEquals(110757.984375, taskDetails.getDistanceFromUser(), 0.00001);
        assertEquals(0, structuresCaptor.getValue().intValue());

        TaskDetails bccTaskDetails = taskListCaptor.getValue().get(0);
        assertEquals(bccTask, bccTaskDetails.getTaskId());
        assertEquals(Intervention.BCC, bccTaskDetails.getTaskCode());
        assertEquals(-2, bccTaskDetails.getDistanceFromUser(), 0);
    }

    @Test
    public void testCalculateDistanceFromUserWithNullTasks() {
        interactor.calculateDistanceFromUser(null, null);
        verifyNoMoreInteractions(presenter);

    }

    @Test
    public void testCalculateDistanceFromUser() {
        Location center = new Location("Test");
        center.setLatitude(-14.152197);
        center.setLongitude(32.643570);
        List<TaskDetails> tasks = new ArrayList<>();
        TaskDetails task1 = TestingUtils.getTaskDetails();
        Location location1 = new Location("Test");
        location1.setLongitude(-14.152197);
        location1.setLongitude(32.643232);
        task1.setLocation(location1);

        tasks.add(task1);

        TaskDetails task2 = TestingUtils.getTaskDetails();
        task2.setTaskId("task_id_2");
        task2.setLocation(center);
        tasks.add(task2);

        interactor.calculateDistanceFromUser(tasks, center);


        verify(presenter, timeout(ASYNC_TIMEOUT)).onTasksFound(taskListCaptor.capture(), structuresCaptor.capture());
        List<TaskDetails> orderedTasks = taskListCaptor.getValue();

        assertEquals(1, orderedTasks.indexOf(task1));
        assertEquals(0, orderedTasks.indexOf(task2));
        verifyNoMoreInteractions(presenter);

    }

    @Test
    public void testFindTasksWithTaskGrouping() {
        PreferencesUtil.getInstance().setCurrentPlan("FI_2019_TV01_Focus");
        PreferencesUtil.getInstance().setInterventionTypeForPlan("FI_2019_TV01_Focus", FI);
        Pair<String, String[]> pair = new Pair<>("task.group_id = ? AND task.plan_id = ? AND status NOT IN (?,?)", new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        Location userLocation = new Location("Test");
        userLocation.setLatitude(-14.987197);
        userLocation.setLongitude(32.076570);
        String memberTask = UUID.randomUUID().toString();
        String bccTask = UUID.randomUUID().toString();
        String indexTask = UUID.randomUUID().toString();
        when(database.rawQuery(groupedRegisteredStructureTasksSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(memberTask, Intervention.BEDNET_DISTRIBUTION));
        when(database.rawQuery(nonRegisteredStructureTasksQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(UUID.randomUUID().toString(), Intervention.LARVAL_DIPPING));
        when(database.rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor(bccTask, Intervention.BCC));
        when(database.rawQuery(indexSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name(), Intervention.CASE_CONFIRMATION})).thenReturn(createCursor(indexTask, Intervention.CASE_CONFIRMATION));
        interactor.findTasks(pair, userLocation, null, "House");
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(groupedRegisteredStructureTasksSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(nonRegisteredStructureTasksQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(bccSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(indexSelectQuery, new String[]{groupId, planId, CANCELLED.name(), ARCHIVED.name(), Intervention.CASE_CONFIRMATION});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onTasksFound(taskListCaptor.capture(), structuresCaptor.capture());
        verifyNoMoreInteractions(presenter);
        assertEquals(4, taskListCaptor.getValue().size());


        TaskDetails bccTaskDetails = taskListCaptor.getValue().get(0);
        assertEquals(bccTask, bccTaskDetails.getTaskId());
        assertEquals(Intervention.BCC, bccTaskDetails.getTaskCode());
        assertEquals(-2, bccTaskDetails.getDistanceFromUser(), 0);

        TaskDetails indexTaskDetails = taskListCaptor.getValue().get(1);
        assertEquals(indexTask, indexTaskDetails.getTaskId());
        assertEquals(Intervention.CASE_CONFIRMATION, indexTaskDetails.getTaskCode());
        assertEquals(-1, indexTaskDetails.getDistanceFromUser(), 0);


        TaskDetails taskDetails = taskListCaptor.getValue().get(2);
        assertEquals(memberTask, taskDetails.getTaskId());
        assertEquals(Intervention.BEDNET_DISTRIBUTION, taskDetails.getTaskCode());
        assertEquals(BusinessStatus.NOT_SPRAYED, taskDetails.getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED.name(), taskDetails.getTaskStatus());
        assertEquals(-14.15191915, taskDetails.getLocation().getLatitude(), 0.00001);
        assertEquals(32.64302015, taskDetails.getLocation().getLongitude(), 0.00001);
        assertEquals("Structure 976", taskDetails.getStructureName());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Partial Spray", taskDetails.getSprayStatus());
        assertEquals("Ali House", taskDetails.getFamilyName());
        assertEquals("Not Completed", taskDetails.getTaskDetails());
        assertEquals(110757.984375, taskDetails.getDistanceFromUser(), 0.00001);
        assertEquals(0, structuresCaptor.getValue().intValue());
    }

    @Test
    public void testGetStructure() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        taskDetails.setStructureId(UUID.randomUUID().toString());
        org.smartregister.domain.Location structure = new org.smartregister.domain.Location();
        structure.setId(UUID.randomUUID().toString());
        when(structureRepository.getLocationById(taskDetails.getStructureId())).thenReturn(structure);
        interactor.getStructure(taskDetails);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureFound(structure, taskDetails);
    }

    @Test
    public void testGetIndexCaseDetailsWillNullParams() {
        interactor.getIndexCaseDetails(null, "", planId);
        verify(database, never()).rawQuery(anyString(), any());
        verify(presenter, timeout(ASYNC_TIMEOUT)).onIndexCaseFound(null, false);
        verifyNoMoreInteractions(database);
        verifyNoMoreInteractions(presenter);
    }

    @Test
    public void testGetIndexCaseDetails() {
        String structureId = UUID.randomUUID().toString();
        String query = "SELECT json FROM event WHERE baseEntityId IN (?,?) AND eventType = ?";
        String[] params = new String[]{structureId, groupId, Constants.EventType.CASE_DETAILS_EVENT};
        when(database.rawQuery(query, params)).thenReturn(createIndexCaseCursor(TestingUtils.caseConfirmstionEventJSON));
        interactor.getIndexCaseDetails(structureId, groupId, planId);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(query, new String[]{structureId, groupId, Constants.EventType.CASE_DETAILS_EVENT});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onIndexCaseFound(jsonCaptor.capture(), eq(false));
        assertEquals(TestingUtils.caseConfirmstionEventJSON, jsonCaptor.getValue().toString());
        verifyNoMoreInteractions(database);
        verifyNoMoreInteractions(presenter);
    }


    @Test
    public void testGetIndexCaseDetailsLinkedToJurisdiction() {
        String query = "SELECT json FROM event WHERE baseEntityId IN (?) AND eventType = ?";
        String[] params = new String[]{groupId, Constants.EventType.CASE_DETAILS_EVENT};
        String event = TestingUtils.caseConfirmstionEventJSON.replace("bd73f7d7-4387-4b6b-b632-acb03c4ea160", groupId);
        when(database.rawQuery(query, params)).thenReturn(createIndexCaseCursor(event));
        interactor.getIndexCaseDetails(null, groupId, planId);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(query, new String[]{groupId, Constants.EventType.CASE_DETAILS_EVENT});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onIndexCaseFound(jsonCaptor.capture(), eq(true));
        assertEquals(event, jsonCaptor.getValue().toString());
        verifyNoMoreInteractions(database);
        verifyNoMoreInteractions(presenter);
    }


    @Test
    public void testResetTaskInfo() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();

        when(interactorUtils.archiveEventsForTask(any(), any())).thenReturn(true);
        when(interactorUtils.getFormSubmissionIdsFromEventTask(any(), any())).thenReturn(null);

        interactor.resetTaskInfo(RuntimeEnvironment.application, taskDetails);
        verify(interactorUtils, timeout(ASYNC_TIMEOUT)).resetTaskInfo(any(), taskDetailArgumentCaptor.capture());

        assertEquals(taskDetails.getTaskEntity(), taskDetailArgumentCaptor.getValue().getTaskEntity());
        assertEquals(taskDetails.getTaskStatus(), taskDetailArgumentCaptor.getValue().getTaskStatus());
        assertEquals(taskDetails.getBusinessStatus(), taskDetailArgumentCaptor.getValue().getBusinessStatus());
    }

    @Test
    public void testGroupedRegisteredStructureTasksSelectCheckDoesNotcheckPlanForOtherBuilds() throws Exception {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualMainSelect = Whitebox.invokeMethod(interactor, "mainSelect", "");
        assertFalse(actualMainSelect.contains("AND sprayed_structures.plan_id = ''"));
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    private Cursor createCursor(String taskId, String intervention) {
        MatrixCursor cursor = createEmptyCursor();
        cursor.addRow(new Object[]{
                taskId,
                intervention,
                434343,
                BusinessStatus.NOT_SPRAYED,
                Task.TaskStatus.COMPLETED,
                -14.15191915,
                32.64302015,
                "Structure 976",
                "Ali",
                "Partial Spray",
                "other",
                "Not Completed",
                434343,
                null,
                1,
                1,
                "BedNet Distribution-Complete",
                null,
                12,
                "Jane Doe, John Doe"
        });
        return cursor;
    }

    private MatrixCursor createEmptyCursor() {
        return new MatrixCursor(new String[]{
                ID_,
                CODE,
                FOR,
                BUSINESS_STATUS,
                STATUS,
                LATITUDE,
                LONGITUDE,
                NAME,
                FAMILY_NAME,
                SPRAY_STATUS,
                NOT_SRAYED_REASON,
                NOT_SRAYED_OTHER_REASON,
                STRUCTURE_ID,
                FIRST_NAME,
                TASK_COUNT,
                COMPLETED_TASK_COUNT,
                GROUPED_STRUCTURE_TASK_CODE_AND_STATUS,
                REFERENCE_REASON,
                HOUSE_NUMBER,
                FAMILY_MEMBER_NAMES
        });
    }

    private Cursor createIndexCaseCursor(String event) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "json"
        });

        cursor.addRow(new Object[]{
                event});
        return cursor;
    }
}
