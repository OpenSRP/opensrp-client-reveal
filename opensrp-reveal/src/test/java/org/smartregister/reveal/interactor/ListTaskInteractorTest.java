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
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.SprayCardDetails;
import org.smartregister.reveal.presenter.ListTaskPresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestDataUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.Cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;

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

    @Captor
    private ArgumentCaptor<CardDetails> cardDetailsCaptor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonArgumentCaptor;

    @Captor
    private ArgumentCaptor<Feature> featureArgumentCaptor;

    private ListTaskInteractor listTaskInteractor;

    private Location operationArea = TestDataUtils.gson.fromJson(TestingUtils.operationalAreaGeoJSON, Location.class);

    private Location structure = TestingUtils.gson.fromJson(TestingUtils.structureJSON, Location.class);

    private Task task = TestingUtils.getTask(structure.getId());

    @Before
    public void setUp() {
        listTaskInteractor = new ListTaskInteractor(presenter);
        Whitebox.setInternalState(listTaskInteractor, "database", database);
        Whitebox.setInternalState(listTaskInteractor, "taskRepository", taskRepository);
        Whitebox.setInternalState(listTaskInteractor, "structureRepository", structureRepository);
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
        verify(presenter,timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
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
        verify(presenter,timeout(ASYNC_TIMEOUT)).onCardDetailsFetched(cardDetailsCaptor.capture());
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
        verify(presenter,timeout(ASYNC_TIMEOUT)).onInterventionFormDetailsFetched(cardDetailsCaptor.capture());
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
        verify(presenter,timeout(ASYNC_TIMEOUT)).onCardDetailsFetched(cardDetailsCaptor.capture());
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
        Map<String, Task> taskMap = new HashMap<>();
        taskMap.put(structure.getId(), task);
        when(taskRepository.getTasksByPlanAndGroup(plan, operationAreaId)).thenReturn(taskMap);
        when(structureRepository.getLocationsByParentId(operationAreaId)).thenReturn(Collections.singletonList(structure));
    }

    @Test
    public void testFetchLocations() {
        String plan = UUID.randomUUID().toString();
        String operationAreaId = operationArea.getId();
        setOperationArea(plan);
        listTaskInteractor.fetchLocations(plan, operationAreaId);
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).getTasksByPlanAndGroup(plan, operationAreaId);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationsByParentId(operationAreaId);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructuresFetched(jsonArgumentCaptor.capture(), featureArgumentCaptor.capture());
        assertEquals(operationAreaId, featureArgumentCaptor.getValue().id());
        FeatureCollection featureCollection = FeatureCollection.fromJson(jsonArgumentCaptor.getValue().toString());
        assertEquals("FeatureCollection", featureCollection.type());
        assertEquals(1, featureCollection.features().size());
        Feature feature = featureCollection.features().get(0);
        assertEquals(structure.getId(), feature.id());
        assertEquals(task.getIdentifier(), feature.getStringProperty(Properties.TASK_IDENTIFIER));
        assertEquals(task.getBusinessStatus(), feature.getStringProperty(Properties.TASK_BUSINESS_STATUS));
        assertEquals(task.getStatus().name(), feature.getStringProperty(Properties.TASK_STATUS));
        assertEquals(task.getCode(), feature.getStringProperty(Properties.TASK_CODE));
        assertEquals(Boolean.FALSE.toString(), feature.getStringProperty(Constants.GeoJSON.IS_INDEX_CASE));
    }


    @Test
    public void testGetIndexCaseStructure() {
        String plan = UUID.randomUUID().toString();
        String operationAreaId = operationArea.getId();
        setOperationArea(plan);
        when(database.rawQuery(anyString(), eq(new String[]{plan, CASE_CONFIRMATION}))).thenReturn(createIndexCaseCursor());
        listTaskInteractor.fetchLocations(plan, operationAreaId);
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).getTasksByPlanAndGroup(plan, operationAreaId);
        verify(structureRepository, timeout(ASYNC_TIMEOUT)).getLocationsByParentId(operationAreaId);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructuresFetched(jsonArgumentCaptor.capture(), featureArgumentCaptor.capture());
        verify(database).rawQuery(anyString(), eq(new String[]{plan, CASE_CONFIRMATION}));
        assertEquals(operationAreaId, featureArgumentCaptor.getValue().id());
        FeatureCollection featureCollection = FeatureCollection.fromJson(jsonArgumentCaptor.getValue().toString());
        assertEquals("FeatureCollection", featureCollection.type());
        assertEquals(1, featureCollection.features().size());
        Feature feature = featureCollection.features().get(0);
        assertEquals(structure.getId(), feature.id());
        assertEquals(task.getIdentifier(), feature.getStringProperty(Properties.TASK_IDENTIFIER));
        assertEquals(task.getBusinessStatus(), feature.getStringProperty(Properties.TASK_BUSINESS_STATUS));
        assertEquals(task.getStatus().name(), feature.getStringProperty(Properties.TASK_STATUS));
        assertEquals(task.getCode(), feature.getStringProperty(Properties.TASK_CODE));
        assertEquals(Boolean.TRUE.toString(), feature.getStringProperty(Constants.GeoJSON.IS_INDEX_CASE));
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


}
