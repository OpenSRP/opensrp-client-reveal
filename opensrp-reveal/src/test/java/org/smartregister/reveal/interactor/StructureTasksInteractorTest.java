package org.smartregister.reveal.interactor;

import net.sqlcipher.Cursor;
import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

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
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.StructureTasksContract;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.domain.Task.TaskStatus.ARCHIVED;
import static org.smartregister.domain.Task.TaskStatus.CANCELLED;
import static org.smartregister.domain.Task.TaskStatus.READY;
import static org.smartregister.family.util.DBConstants.KEY.DOB;
import static org.smartregister.reveal.util.Constants.BLOOD_SCREENING_EVENT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.TestingUtils.bloodScreeningEventJSON;

/**
 * Created by samuelgithengi on 4/24/19.
 */
public class StructureTasksInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private StructureTasksContract.Presenter presenter;

    @Mock
    private SQLiteDatabase database;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private InteractorUtils interactorUtils;

    @Captor
    private ArgumentCaptor<List<StructureTaskDetails>> taskDetailsArgumentCaptor;

    @Captor
    private ArgumentCaptor<StructureTaskDetails> taskArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event> eventArgumentaCaptor;

    private StructureTasksInteractor interactor;

    @Before
    public void setUp() {
        AppExecutors appExecutors = new AppExecutors(Executors.newSingleThreadExecutor(),
                Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
        interactor = new StructureTasksInteractor(presenter, appExecutors, database, structureRepository);
        Whitebox.setInternalState(interactor, "interactorUtils", interactorUtils);
    }

    @Test
    public void testFindTasks() {
        String planId = UUID.randomUUID().toString();
        String structure = UUID.randomUUID().toString();
        String jurisdiction = UUID.randomUUID().toString();
        String taskQuery = "Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.structure_id FROM task WHERE for=? AND plan_id=? AND status NOT IN (?,?) ";
        String memberQuery = "Select structure._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.structure_id , printf('%s %s %s',first_name,middle_name,last_name) AS name , dob , ec_family_member.structure_id FROM structure  JOIN ec_family_member ON ec_family_member.structure_id = structure._id   JOIN task ON task.for = ec_family_member.base_entity_id  WHERE structure._id=? AND plan_id=? AND date_removed IS NULL AND status NOT IN (?,?) ";
        String indexQuery = "Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.structure_id FROM task WHERE group_id = ? AND plan_id = ? AND code = ? AND status = ? ";

        when(database.rawQuery(taskQuery, new String[]{structure, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createCursor());
        when(database.rawQuery(memberQuery, new String[]{structure, planId, CANCELLED.name(), ARCHIVED.name()})).thenReturn(createMemberCursor());
        when(database.rawQuery(indexQuery, new String[]{jurisdiction, planId, Intervention.CASE_CONFIRMATION, READY.name()})).thenReturn(createIndexCursor());

        interactor.findTasks(structure, planId, jurisdiction);

        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(taskQuery, new String[]{structure, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(memberQuery, new String[]{structure, planId, CANCELLED.name(), ARCHIVED.name()});
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(indexQuery, new String[]{jurisdiction, planId, Intervention.CASE_CONFIRMATION, READY.name()});

        verify(presenter, timeout(ASYNC_TIMEOUT)).onTasksFound(taskDetailsArgumentCaptor.capture(), taskArgumentCaptor.capture());

        assertEquals(2, taskDetailsArgumentCaptor.getValue().size());

        StructureTaskDetails irsTask = taskDetailsArgumentCaptor.getValue().get(0);
        assertEquals("task_id_1", irsTask.getTaskId());
        assertEquals(Intervention.IRS, irsTask.getTaskCode());
        assertEquals("434343", irsTask.getTaskEntity());
        assertEquals(BusinessStatus.NOT_SPRAYED, irsTask.getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED.name(), irsTask.getTaskStatus());

        StructureTaskDetails memberTask = taskDetailsArgumentCaptor.getValue().get(1);
        assertEquals("task_id_2", memberTask.getTaskId());
        assertEquals(Intervention.BLOOD_SCREENING, memberTask.getTaskCode());
        assertEquals("w08989-ejkjkj-eere", memberTask.getTaskEntity());
        assertEquals(BusinessStatus.NOT_VISITED, memberTask.getBusinessStatus());
        assertEquals(Task.TaskStatus.READY.name(), memberTask.getTaskStatus());
        String age = org.smartregister.reveal.util.Utils.getAge("1982-01-01T03:00:00.000+03:00");
        assertEquals("Charity Otala, " + age, memberTask.getTaskName());
        assertEquals("1215972243", memberTask.getStructureId());

        StructureTaskDetails indexCase = taskArgumentCaptor.getValue();
        assertEquals("task_id_123", indexCase.getTaskId());
        assertEquals(Intervention.CASE_CONFIRMATION, indexCase.getTaskCode());
        assertEquals("063e4d28-9caa-11e9-a2a3", indexCase.getTaskEntity());

    }


    @Test
    public void testGetStructure() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        Location location = new Location();
        when(structureRepository.getLocationById(taskDetails.getTaskEntity())).thenReturn(location);
        interactor.getStructure(taskDetails);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationById(taskDetails.getTaskEntity());
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureFound(location, taskDetails);
    }


    @Test
    public void testGetStructureForMemberTask() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        taskDetails.setTaskCode(Intervention.BLOOD_SCREENING);
        taskDetails.setStructureId(UUID.randomUUID().toString());
        Location location = new Location();
        when(structureRepository.getLocationById(taskDetails.getStructureId())).thenReturn(location);
        interactor.getStructure(taskDetails);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationById(taskDetails.getStructureId());
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureFound(location, taskDetails);
    }

    @Test
    public void resetTaskInfo() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        String structureId = UUID.randomUUID().toString();
        taskDetails.setStructureId(structureId);

        interactor.resetTaskInfo(RuntimeEnvironment.application, taskDetails);

        verify(interactorUtils, timeout(ASYNC_TIMEOUT)).resetTaskInfo(any(), taskArgumentCaptor.capture());
        assertEquals(taskDetails.getTaskId(), taskArgumentCaptor.getValue().getTaskId());
        assertEquals(structureId, taskArgumentCaptor.getValue().getStructureId());
        verify(presenter, timeout(ASYNC_TIMEOUT)).onTaskInfoReset(stringArgumentCaptor.capture());
        assertEquals(structureId, stringArgumentCaptor.getValue());
    }

    @Test
    public void testFindLastBloodScreeningEvent() {
        StructureTaskDetails taskDetails = TestingUtils.getStructureTaskDetails();
        taskDetails.setTaskCode(Intervention.BLOOD_SCREENING);

        Event expectedEvent = JsonFormUtils.gson.fromJson(bloodScreeningEventJSON, Event.class);
        String eventsSql = String.format("select %s from %s where %s = ? and %s =? order by %s desc limit 1",
                EventClientRepository.event_column.json, EventClientRepository.Table.event.name(), EventClientRepository.event_column.baseEntityId, EventClientRepository.event_column.eventType, EventClientRepository.event_column.updatedAt);
        when(database.rawQuery(eventsSql, new String[]{taskDetails.getTaskEntity(), BLOOD_SCREENING_EVENT})).thenReturn(createEventJsonCursor());
        interactor.findLastEvent(taskDetails);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onEventFound(eventArgumentaCaptor.capture());
        assertEquals(expectedEvent.getBaseEntityId(), eventArgumentaCaptor.getValue().getBaseEntityId());
        assertEquals(expectedEvent.getFormSubmissionId(), eventArgumentaCaptor.getValue().getFormSubmissionId());
        assertEquals(expectedEvent.getEventType(), eventArgumentaCaptor.getValue().getEventType());
        assertEquals(expectedEvent.getLocationId(), eventArgumentaCaptor.getValue().getLocationId());
    }


    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ID_,
                CODE,
                FOR,
                BUSINESS_STATUS,
                STATUS,
                STRUCTURE_ID
        });
        cursor.addRow(new Object[]{
                "task_id_1",
                Intervention.IRS,
                434343,
                BusinessStatus.NOT_SPRAYED,
                Task.TaskStatus.COMPLETED,
                1215972243
        });
        return cursor;
    }

    private Cursor createIndexCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ID_,
                CODE,
                FOR,
                BUSINESS_STATUS,
                STATUS,
                STRUCTURE_ID
        });
        cursor.addRow(new Object[]{
                "task_id_123",
                Intervention.CASE_CONFIRMATION,
                "063e4d28-9caa-11e9-a2a3",
                BusinessStatus.NOT_VISITED,
                READY,
                1215972243
        });
        return cursor;
    }


    private Cursor createMemberCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                ID_,
                CODE,
                FOR,
                BUSINESS_STATUS,
                STATUS,
                NAME,
                DOB,
                STRUCTURE_ID
        });
        cursor.addRow(new Object[]{
                "task_id_2",
                Intervention.BLOOD_SCREENING,
                "w08989-ejkjkj-eere",
                BusinessStatus.NOT_VISITED,
                READY,
                "Charity Otala",
                "1982-01-01T03:00:00.000+03:00",
                1215972243
        });
        return cursor;
    }

    private Cursor createEventJsonCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "json"
        });
        cursor.addRow(new Object[]{
                bloodScreeningEventJSON
        });
        return cursor;
    }


}
