package org.smartregister.reveal.interactor;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

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
import org.smartregister.Context;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Task;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.FamilyCardDetails;
import org.smartregister.reveal.model.IRSVerificationCardDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.SprayCardDetails;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.presenter.ListTaskPresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.DatabaseKeys;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestDataUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.domain.Task.TaskStatus.COMPLETED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.IN_PROGRESS;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_ELIGIBLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Properties.APP_VERSION_NAME;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_ID;
import static org.smartregister.reveal.util.Constants.Properties.TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.Properties.TASK_STATUS;
import static org.smartregister.reveal.util.Utils.getPropertyValue;

/**
 * Created by samuelgithengi on 5/22/19.
 */
public class ListTaskInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ListTaskPresenter presenter;

    @Mock
    private SQLiteDatabase database;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private InteractorUtils interactorUtils;

    @Mock
    private EventClientRepository eventClientRepository;

    @Captor
    private ArgumentCaptor<CardDetails> cardDetailsCaptor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonArgumentCaptor;

    @Captor
    private ArgumentCaptor<Feature> featureArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<TaskDetails>> taskDetailsCaptor;

    @Captor
    private ArgumentCaptor<BaseTaskDetails> baseTaskDetailsArgumentCaptor;

    @Captor
    private ArgumentCaptor<Location> locationArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    private ListTaskInteractor listTaskInteractor;

    private Location operationArea = TestDataUtils.gson.fromJson(TestingUtils.operationalAreaGeoJSON, Location.class);

    private Location structure = TestingUtils.gson.fromJson(TestingUtils.structureJSON, Location.class);

    private Task task = TestingUtils.getTask(structure.getId());

    @Before
    public void setUp() {
        Context.bindtypes = new ArrayList<>();
        listTaskInteractor = new ListTaskInteractor(presenter);
        Whitebox.setInternalState(listTaskInteractor, "database", database);
        Whitebox.setInternalState(listTaskInteractor, "taskRepository", taskRepository);
        Whitebox.setInternalState(listTaskInteractor, "structureRepository", structureRepository);
        Whitebox.setInternalState(listTaskInteractor, "interactorUtils", interactorUtils);
        Whitebox.setInternalState(listTaskInteractor, "eventClientRepository", eventClientRepository);
    }

    @Test
    public void testFetchIRSCardDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createSprayCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.IRS, feature, false);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT spray_status, not_sprayed_reason, not_sprayed_other_reason, property_type, spray_date, spray_operator, family_head_name FROM sprayed_structures WHERE id=?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onCardDetailsFetched(cardDetailsCaptor.capture());
        assertEquals("Locked", cardDetailsCaptor.getValue().getReason());
        assertEquals("Not Sprayed", cardDetailsCaptor.getValue().getStatus());
    }

    @Test
    public void testFetchIRSFormDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createSprayCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.IRS, feature, true);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT spray_status, not_sprayed_reason, not_sprayed_other_reason, property_type, spray_date, spray_operator, family_head_name FROM sprayed_structures WHERE id=?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
        SprayCardDetails cardDetails = (SprayCardDetails) cardDetailsCaptor.getValue();
        assertEquals("Locked", cardDetails.getReason());
        assertEquals("Not Sprayed", cardDetails.getStatus());
        assertEquals("Doe John", cardDetails.getFamilyHead());
        assertEquals("11/03/1977", cardDetails.getSprayDate());
        assertEquals("John Doe", cardDetails.getSprayOperator());
        assertEquals("Residential", cardDetails.getPropertyType());
    }


    @Test
    public void testFetchMosquitoCardDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createMosquitoLarvalCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.MOSQUITO_COLLECTION, feature, false);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT status, start_date, end_date FROM mosquito_collections WHERE id=?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onCardDetailsFetched(cardDetailsCaptor.capture());
        assertNotNull(cardDetailsCaptor.getValue());
        MosquitoHarvestCardDetails cardDetails = (MosquitoHarvestCardDetails) cardDetailsCaptor.getValue();
        assertEquals("active", cardDetails.getStatus());
        assertEquals("11/02/1977", cardDetails.getStartDate());
        assertEquals("11/02/1977", cardDetails.getEndDate());
        assertEquals(Intervention.MOSQUITO_COLLECTION, cardDetails.getInterventionType());
    }

    @Test
    public void testFetchMosquitoFormDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createMosquitoLarvalCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.MOSQUITO_COLLECTION, feature, true);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT status, start_date, end_date FROM mosquito_collections WHERE id=?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
        assertNotNull(cardDetailsCaptor.getValue());
        MosquitoHarvestCardDetails cardDetails = (MosquitoHarvestCardDetails) cardDetailsCaptor.getValue();
        assertEquals("active", cardDetails.getStatus());
        assertEquals("11/02/1977", cardDetails.getStartDate());
        assertEquals("11/02/1977", cardDetails.getEndDate());
        assertEquals(Intervention.MOSQUITO_COLLECTION, cardDetails.getInterventionType());
    }

    @Test
    public void testFetchLarvalCardDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createMosquitoLarvalCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.LARVAL_DIPPING, feature, false);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT status, start_date, end_date FROM larval_dippings WHERE id=?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onCardDetailsFetched(cardDetailsCaptor.capture());
        assertNotNull(cardDetailsCaptor.getValue());
        MosquitoHarvestCardDetails cardDetails = (MosquitoHarvestCardDetails) cardDetailsCaptor.getValue();
        assertEquals("active", cardDetails.getStatus());
        assertEquals("11/02/1977", cardDetails.getStartDate());
        assertEquals("11/02/1977", cardDetails.getEndDate());
        assertEquals(Intervention.LARVAL_DIPPING, cardDetails.getInterventionType());
    }

    private void setOperationArea(String plan) {
        String operationAreaId = operationArea.getId();
        PreferencesUtil.getInstance().setCurrentOperationalArea(operationAreaId);
        Cache<Location> cache = new Cache<>();
        cache.get(operationAreaId, () -> operationArea);
        Whitebox.setInternalState(Utils.class, "cache", cache);
        Map<String, Set<Task>> taskMap = new HashMap<>();
        taskMap.put(structure.getId(), Collections.singleton(task));
        when(taskRepository.getTasksByPlanAndGroup(plan, operationAreaId)).thenReturn(taskMap);
        when(structureRepository.getLocationsByParentId(operationAreaId)).thenReturn(Collections.singletonList(structure));
    }

    @Test
    public void testFetchLocations() {
        String groupNameQuery = "Select structure._id as _id , COALESCE(ec_family.first_name,structure_name,name,family_head_name) , group_concat(ec_family_member.first_name||' '||ec_family_member.last_name) FROM structure LEFT JOIN ec_family ON structure._id = ec_family.structure_id AND ec_family.date_removed IS NULL collate nocase  LEFT JOIN ec_family_member ON ec_family.base_entity_id = ec_family_member.relational_id AND ec_family_member.date_removed IS NULL collate nocase  LEFT JOIN sprayed_structures ON structure._id = sprayed_structures.id collate nocase  WHERE parent_id=?  GROUP BY structure._id";
        String plan = UUID.randomUUID().toString();
        String operationAreaId = operationArea.getId();
        setOperationArea(plan);
        doReturn(createStructureNameCursor()).when(database).rawQuery(groupNameQuery, new String[]{operationAreaId});
        listTaskInteractor.fetchLocations(plan, operationAreaId);
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).getTasksByPlanAndGroup(plan, operationAreaId);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationsByParentId(operationAreaId);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructuresFetched(jsonArgumentCaptor.capture(), featureArgumentCaptor.capture(), taskDetailsCaptor.capture());
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(groupNameQuery, new String[]{operationAreaId});
        assertEquals(operationAreaId, featureArgumentCaptor.getValue().id());
        FeatureCollection featureCollection = FeatureCollection.fromJson(jsonArgumentCaptor.getValue().toString());
        assertEquals("FeatureCollection", featureCollection.type());
        assertEquals(1, featureCollection.features().size());
        Feature feature = featureCollection.features().get(0);
        assertEquals(structure.getId(), feature.id());
        assertEquals(task.getIdentifier(), feature.getStringProperty(TASK_IDENTIFIER));
        assertEquals(task.getBusinessStatus(), feature.getStringProperty(Properties.FEATURE_SELECT_TASK_BUSINESS_STATUS));
        assertEquals(task.getStatus().name(), feature.getStringProperty(Properties.TASK_STATUS));
        assertEquals(task.getCode(), feature.getStringProperty(Properties.TASK_CODE));
        assertEquals(Boolean.FALSE.toString(), feature.getStringProperty(Constants.GeoJSON.IS_INDEX_CASE));
        assertEquals("Harry House", feature.getStringProperty(Constants.Properties.STRUCTURE_NAME));
        assertEquals("Harry Pin, Jerry Kin", feature.getStringProperty(Properties.FAMILY_MEMBER_NAMES));
    }


    @Test
    public void testGetIndexCaseStructure() {
        String plan = UUID.randomUUID().toString();
        String operationAreaId = operationArea.getId();
        setOperationArea(plan);
        PreferencesUtil.getInstance().setCurrentPlan(plan);
        PreferencesUtil.getInstance().setInterventionTypeForPlan(plan, "FI");
        when(database.rawQuery(anyString(), eq(new String[]{plan, CASE_CONFIRMATION}))).thenReturn(createIndexCaseCursor());
        listTaskInteractor.fetchLocations(plan, operationAreaId);
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).getTasksByPlanAndGroup(plan, operationAreaId);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationsByParentId(operationAreaId);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructuresFetched(jsonArgumentCaptor.capture(), featureArgumentCaptor.capture(), taskDetailsCaptor.capture());
        verify(database).rawQuery(anyString(), eq(new String[]{plan, CASE_CONFIRMATION}));
        assertEquals(operationAreaId, featureArgumentCaptor.getValue().id());
        FeatureCollection featureCollection = FeatureCollection.fromJson(jsonArgumentCaptor.getValue().toString());
        assertEquals("FeatureCollection", featureCollection.type());
        assertEquals(1, featureCollection.features().size());
        Feature feature = featureCollection.features().get(0);
        assertEquals(structure.getId(), feature.id());
        assertEquals(task.getIdentifier(), feature.getStringProperty(TASK_IDENTIFIER));
        assertEquals(task.getBusinessStatus(), feature.getStringProperty(Properties.FEATURE_SELECT_TASK_BUSINESS_STATUS));
        assertEquals(task.getStatus().name(), feature.getStringProperty(Properties.TASK_STATUS));
        assertEquals(task.getCode(), feature.getStringProperty(Properties.TASK_CODE));
    }


    @Test
    public void testEditCDDTaskCompleteStatus(){
        Feature feature = TestingUtils.getStructure();
        String taskIdentifier = getPropertyValue(feature, TASK_IDENTIFIER);
        Task task = TestingUtils.getTask("entity-id");
        task.setIdentifier(taskIdentifier);
        when(taskRepository.getTaskByIdentifier(anyString())).thenReturn(task);
        listTaskInteractor.editCDDTaskCompleteStatus(feature,true);
        assertEquals(COMPLETED,task.getStatus());
        assertEquals("Complete",task.getBusinessStatus());
        listTaskInteractor.editCDDTaskCompleteStatus(feature,false);
        assertEquals(COMPLETED,task.getStatus());
        assertEquals("In Progress",task.getBusinessStatus());

    }

    @Test
    public void testFetchPAOTFormDetails() {
        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createPAOTCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.PAOT, feature, true);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT paot_status, paot_comments, last_updated_date  FROM potential_area_of_transmission WHERE base_entity_id=? ", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
        MosquitoHarvestCardDetails cardDetails = (MosquitoHarvestCardDetails) cardDetailsCaptor.getValue();
        assertEquals("In-active", cardDetails.getStatus());
        assertEquals("Paot Active Comments", cardDetails.getComments());
        assertEquals("11/02/1977", cardDetails.getStartDate());
        assertEquals(Intervention.PAOT, cardDetails.getInterventionType());
    }

    @Test
    public void testGetTaskSelect() {
        String expectedQuery = "Select task._id as _id , task._id , task.code , task.for , task.business_status , task.status , task.structure_id FROM task";
        String actualQuery = listTaskInteractor.getTaskSelect("");
        assertEquals(expectedQuery, actualQuery);
    }

    @Test
    public void testReadTaskDetails() {
        Task expectedTask = TestingUtils.getTask("task-entity-id");
        MatrixCursor taskCursor = TestingUtils.getTaskCursor(expectedTask);
        taskCursor.moveToNext();

        StructureTaskDetails taskdetails = listTaskInteractor.readTaskDetails(taskCursor);

        assertEquals(expectedTask.getIdentifier(), taskdetails.getTaskId());
        assertEquals(expectedTask.getCode(), taskdetails.getTaskCode());
        assertEquals(expectedTask.getForEntity(), taskdetails.getTaskEntity());
        assertEquals(expectedTask.getBusinessStatus(), taskdetails.getBusinessStatus());
        assertEquals(expectedTask.getStatus().name(), taskdetails.getTaskStatus());
        assertEquals(expectedTask.getStructureId(), taskdetails.getStructureId());

    }

    @Test
    public void testResetInterventionTaskInfo() {
        Task expectedTask = TestingUtils.getTask("task-entity-id");
        MatrixCursor taskCursor = TestingUtils.getTaskCursor(expectedTask);

        when(database.rawQuery(any(), any())).thenReturn(taskCursor);
        when(interactorUtils.resetTaskInfo(any(), any())).thenReturn(true);

        listTaskInteractor.resetInterventionTaskInfo(BEDNET_DISTRIBUTION, expectedTask.getStructureId());

        verify(interactorUtils, timeout(ASYNC_TIMEOUT)).resetTaskInfo(any(), baseTaskDetailsArgumentCaptor.capture());

        BaseTaskDetails taskDetails = baseTaskDetailsArgumentCaptor.getValue();

        assertEquals(expectedTask.getCode(), taskDetails.getTaskCode());
        assertEquals(expectedTask.getForEntity(), taskDetails.getTaskEntity());
        assertEquals(expectedTask.getBusinessStatus(), taskDetails.getBusinessStatus());
        assertEquals(expectedTask.getStatus().name(), taskDetails.getTaskStatus());
        assertEquals(expectedTask.getStructureId(), taskDetails.getStructureId());

        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionTaskInfoReset(eq(true));


    }

    @Test
    public void testMarkStructureAsInactive() {

        Location location = TestingUtils.getOperationalArea();
        location.getProperties().setStatus(LocationProperty.PropertyStatus.INACTIVE);
        assertEquals(LocationProperty.PropertyStatus.INACTIVE, location.getProperties().getStatus());

        when(structureRepository.getLocationById(anyString())).thenReturn(location);
        Feature feature = TestingUtils.getStructure();
        feature.addStringProperty(TASK_IDENTIFIER, "task-id-a");

        listTaskInteractor.markStructureAsInactive(feature);

        verify(structureRepository).getLocationById(stringArgumentCaptor.capture());
        assertEquals(feature.id(), stringArgumentCaptor.getValue());
        verify(structureRepository).addOrUpdate(locationArgumentCaptor.capture());
        assertEquals(LocationProperty.PropertyStatus.INACTIVE, locationArgumentCaptor.getValue().getProperties().getStatus());
        verify(taskRepository).cancelTaskByIdentifier(stringArgumentCaptor.capture());
        assertEquals(feature.getStringProperty(TASK_IDENTIFIER), stringArgumentCaptor.getValue());
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureMarkedInactive();
    }

    @Test
    public void testMarkStructureAsIneligible() {

        Feature feature = TestingUtils.getStructure();

        String taskIdentifier = getPropertyValue(feature, TASK_IDENTIFIER);
        String reasonUnEligible = "Not Eligible: Other";
        Task task = TestingUtils.getTask("entity-id");
        task.setIdentifier(taskIdentifier);
        String expectedStatus = task.getStatus().name();
        String expectedBusinessStatus = task.getBusinessStatus();

        when(taskRepository.getTaskByIdentifier(anyString())).thenReturn(task);

        listTaskInteractor.markStructureAsIneligible(feature, reasonUnEligible);

        verify(taskRepository).getTaskByIdentifier(stringArgumentCaptor.capture());
        assertEquals(taskIdentifier, stringArgumentCaptor.getValue());

        verify(taskRepository).addOrUpdate(taskArgumentCaptor.capture());
        Task actualTask = taskArgumentCaptor.getValue();
        assertEquals(NOT_ELIGIBLE, actualTask.getBusinessStatus());
        assertEquals(COMPLETED, task.getStatus());
        assertNotNull(task.getLastModified());
        assertEquals(taskIdentifier, task.getIdentifier());


        verify(eventClientRepository).addEvent(stringArgumentCaptor.capture(), jsonObjectArgumentCaptor.capture());
        Event actualEvent = taskGson.fromJson(jsonObjectArgumentCaptor.getValue().toString(), Event.class);
        assertEquals(BuildConfig.VERSION_NAME, actualEvent.getDetails().get(APP_VERSION_NAME));
        assertEquals(expectedBusinessStatus, actualEvent.getDetails().get(TASK_BUSINESS_STATUS));
        assertEquals(feature.id(), actualEvent.getDetails().get(LOCATION_ID));
        assertEquals(expectedStatus, actualEvent.getDetails().get(TASK_STATUS));

        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureMarkedIneligible();

    }

    @Test
    public void testCreateIRSVerificationCardDetails() {

        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createIRSVerificationCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.IRS_VERIFICATION, feature, true);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT true_structure, eligible_structure, report_spray, chalk_spray, sticker_spray, card_spray FROM irs_verification WHERE id= ?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
        IRSVerificationCardDetails cardDetails = (IRSVerificationCardDetails) cardDetailsCaptor.getValue();
        assertEquals("Yes", cardDetails.getTrueStructure());
        assertEquals("Yes", cardDetails.getEligStruc());
        assertEquals("sprayed", cardDetails.getReportedSprayStatus());
        assertEquals("No chalk", cardDetails.getChalkSprayStatus());
        assertEquals("No sticker", cardDetails.getStickerSprayStatus());
        assertEquals("No card", cardDetails.getCardSprayStatus());

    }

    @Test
    public void testCreateFamilyCardDetails() {

        String feature = UUID.randomUUID().toString();
        when(database.rawQuery(any(), any())).thenReturn(createFamilyCursor());
        listTaskInteractor.fetchInterventionDetails(Intervention.REGISTER_FAMILY, feature, true);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery("SELECT business_status, authored_on, owner FROM task WHERE for = ?", new String[]{feature});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
        FamilyCardDetails cardDetails = (FamilyCardDetails) cardDetailsCaptor.getValue();
        assertEquals(IN_PROGRESS, cardDetails.getStatus());
        assertEquals("11/02/1977", cardDetails.getDateCreated());
        assertEquals("Nifi-User", cardDetails.getOwner());
    }

    @Test
    public void testSaveJsonFormShouldSaveMosquitoCollectionForm() {
        listTaskInteractor.saveJsonForm(TestingUtils.mosquitoCollectionForm);

        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(any(), any());
    }

    private Cursor createSprayCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{"spray_status", "not_sprayed_reason",
                "not_sprayed_other_reason", "property_type", "spray_date", "spray_operator", "family_head_name"});
        cursor.addRow(new Object[]{"Not Sprayed", "other", "Locked", "Residential", "11/03/1977", "John Doe", "Doe John"});
        return cursor;
    }

    private Cursor createMosquitoLarvalCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{"status", "start_date", "end_date"});
        cursor.addRow(new Object[]{"active", "11/02/1977", "11/02/1977"});
        return cursor;
    }

    private Cursor createIndexCaseCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{STRUCTURE_ID});
        cursor.addRow(new Object[]{structure.getId()});
        return cursor;
    }


    private Cursor createPAOTCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{DatabaseKeys.PAOT_STATUS, DatabaseKeys.PAOT_COMMENTS, DatabaseKeys.LAST_UPDATED_DATE});
        cursor.addRow(new Object[]{"In-active", "Paot Active Comments", "11/02/1977"});
        return cursor;
    }

    private Cursor createStructureNameCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[3]);
        cursor.addRow(new Object[]{structure.getId(), "Harry House", "Harry Pin, Jerry Kin"});
        return cursor;
    }

    private Cursor createIRSVerificationCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{DatabaseKeys.TRUE_STRUCTURE, DatabaseKeys.ELIGIBLE_STRUCTURE,
                DatabaseKeys.REPORT_SPRAY, DatabaseKeys.CHALK_SPRAY, DatabaseKeys.STICKER_SPRAY, DatabaseKeys.CARD_SPRAY});
        cursor.addRow(new Object[]{"Yes", "Yes", "sprayed", "No chalk", "No sticker", "No card"});
        return cursor;
    }

    private Cursor createFamilyCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{DatabaseKeys.BUSINESS_STATUS, DatabaseKeys.AUTHORED_ON, DatabaseKeys.OWNER});
        cursor.addRow(new Object[]{IN_PROGRESS, "11/02/1977", "Nifi-User"});
        return cursor;
    }

}
