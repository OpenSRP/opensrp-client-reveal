package org.smartregister.reveal.interactor;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.mapbox.geojson.Feature;

import net.sqlcipher.Cursor;
import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
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
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.util.Constants;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseContract;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.widget.GeoWidgetFactory;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.Cache;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.BLOOD_SCREENING_EVENT;
import static org.smartregister.reveal.util.Constants.DETAILS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.EventType.CDD_SUPERVISOR_DAILY_SUMMARY;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM;
import static org.smartregister.reveal.util.Constants.JsonForm.LOCATION;
import static org.smartregister.reveal.util.Constants.JsonForm.PAOT_STATUS;
import static org.smartregister.reveal.util.Constants.JsonForm.TABLET_ACCOUNTABILITY_FORM;
import static org.smartregister.reveal.util.Constants.Properties.APP_VERSION_NAME;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_PARENT;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_UUID;
import static org.smartregister.reveal.util.Constants.Properties.PLAN_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.Properties.TASK_BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_CODE;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.Properties.TASK_STATUS;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.VALUES;

/**
 * Created by samuelgithengi on 5/23/19.
 */
public class BaseInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BaseContract.BasePresenter presenter;

    @Mock
    private SQLiteDatabase database;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private RevealClientProcessor clientProcessor;

    @Mock
    protected TaskRepository taskRepository;

    @Mock
    private EventClientRepository eventClientRepository;

    @Captor
    private ArgumentCaptor<CommonPersonObjectClient> clientArgumentCaptor;

    @Captor
    private ArgumentCaptor<JSONObject> eventJSONObjectCaptor;

    @Captor
    private ArgumentCaptor<List<EventClient>> eventClientCaptor;

    @Captor
    private ArgumentCaptor<JSONArray> featureCoordinatesCaptor;

    @Captor
    private ArgumentCaptor<Feature> featureArgumentCaptor;

    @Captor
    private ArgumentCaptor<Double> doubleArgumentCaptor;

    @Captor
    private ArgumentCaptor<Task> taskArgumentCaptor;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    private BaseInteractor interactor;

    private Context context = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        interactor = new BaseInteractor(presenter, commonRepository);
        Whitebox.setInternalState(interactor, "structureRepository", structureRepository);
        Whitebox.setInternalState(interactor, "database", database);
        Whitebox.setInternalState(interactor, "clientProcessor", clientProcessor);
        Whitebox.setInternalState(interactor, "eventClientRepository", eventClientRepository);
    }


    @Test
    public void testFetchFamilyDetails() {
        Whitebox.setInternalState(interactor, "commonRepository", commonRepository);
        String structureId = UUID.randomUUID().toString();
        String query = "SELECT base_entity_id FROM EC_FAMILY WHERE structure_id = ? AND date_removed IS NULL";
        when(database.rawQuery(query, new String[]{structureId})).thenReturn(createFamilyCursor());
        CommonPersonObjectClient family = TestingUtils.getCommonPersonObjectClient();
        CommonPersonObject familyObject = new CommonPersonObject(family.getCaseId(),
                null, family.getDetails(), "");
        familyObject.setColumnmaps(family.getColumnmaps());
        when(commonRepository.findByBaseEntityId("69df212c-33a7-4443-a8d5-289e48d90468")).thenReturn(familyObject);
        interactor.fetchFamilyDetails(structureId);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(query, new String[]{structureId});
        verify(presenter, timeout(ASYNC_TIMEOUT)).onFamilyFound(clientArgumentCaptor.capture());
        assertEquals(family.entityId(), clientArgumentCaptor.getValue().entityId());
        assertEquals(family.getColumnmaps(), clientArgumentCaptor.getValue().getColumnmaps());
    }


    @Test
    public void testSavePaotForm() throws JSONException {
        Cache<Location> cache = mock(Cache.class);
        when(cache.get(anyString(), any())).thenReturn(mock(Location.class));
        Whitebox.setInternalState(Utils.class, "cache", cache);
        String form = AssetHandler.readFileFromAssetsFolder(org.smartregister.reveal.util.Constants.JsonForm.PAOT_FORM, context);
        JSONObject formObject = new JSONObject(form);
        String structureId = UUID.randomUUID().toString();
        formObject.put("entity_id", structureId);
        JSONObject details = new JSONObject();
        String taskId = UUID.randomUUID().toString();
        details.put(TASK_IDENTIFIER, taskId);
        details.put(LOCATION_UUID, structureId);
        formObject.put(DETAILS, details);
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), PAOT_STATUS).put(VALUE, "Active");
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "lastUpdatedDate").put(VALUE, "19-07-2019");
        interactor.saveJsonForm(formObject.toString());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(eq(structureId), eventJSONObjectCaptor.capture());
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).processClient(eventClientCaptor.capture(), eq(true));
        verify(presenter, timeout(ASYNC_TIMEOUT)).onFormSaved(structureId, taskId, Task.TaskStatus.COMPLETED, null, PAOT);
        assertEquals(org.smartregister.reveal.util.Constants.EventType.PAOT_EVENT, eventJSONObjectCaptor.getValue().getString("eventType"));
        JSONArray obs = eventJSONObjectCaptor.getValue().getJSONArray("obs");
        assertEquals(3, obs.length());
        assertEquals("Active", obs.getJSONObject(0).getJSONArray(VALUES).get(0));
        assertEquals("19-07-2019", obs.getJSONObject(1).getJSONArray(VALUES).get(0));
        assertEquals("Complete", obs.getJSONObject(2).getJSONArray(VALUES).get(0));
        assertEquals(details.toString(), eventJSONObjectCaptor.getValue().getJSONObject(DETAILS).toString());

        assertEquals(1, eventClientCaptor.getValue().size());

        Event event = eventClientCaptor.getValue().get(0).getEvent();
        assertEquals(3, event.getObs().size());
        assertEquals("Active", event.getObs().get(0).getValue());
        assertEquals("19-07-2019", event.getObs().get(1).getValue());
        assertEquals("Complete", event.getObs().get(2).getValue());
        assertEquals(2, event.getDetails().size());
        assertEquals(taskId, event.getDetails().get(TASK_IDENTIFIER));
        assertEquals(structureId, event.getBaseEntityId());
    }

    @Test
    public void testSaveRegisterStructureForm() throws JSONException {
        String form = AssetHandler.readFileFromAssetsFolder(org.smartregister.reveal.util.Constants.JsonForm.ADD_STRUCTURE_FORM, context);
        String planIdentifier = UUID.randomUUID().toString();
        PreferencesUtil.getInstance().setCurrentPlanId(planIdentifier);
        JSONObject formObject = new JSONObject(form);
        String locationId = UUID.randomUUID().toString();
        formObject.put("entity_id", locationId);
        JSONObject details = new JSONObject();
        details.put(LOCATION_PARENT, locationId);
        formObject.put(DETAILS, details);
        Whitebox.setInternalState(interactor, "operationalAreaId", locationId);
        String structureOb = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[28.35228319086664,-15.421616685545176,0]},\"properties\":null}";
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "structure").put(VALUE, structureOb);
        double zoomLevel = 18.2d;
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), GeoWidgetFactory.ZOOM_LEVEL).put(VALUE, zoomLevel);
        interactor.saveJsonForm(formObject.toString());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(anyString(), eventJSONObjectCaptor.capture());
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).processClient(eventClientCaptor.capture(), eq(true));
        assertEquals(REGISTER_STRUCTURE_EVENT, eventJSONObjectCaptor.getValue().getString("eventType"));
        JSONArray obs = eventJSONObjectCaptor.getValue().getJSONArray("obs");
        assertEquals(4, obs.length());
        assertEquals(structureOb, obs.getJSONObject(0).getJSONArray(VALUES).get(0));
        assertEquals("Residential Structure", obs.getJSONObject(1).getJSONArray(VALUES).get(0));
        assertEquals("Home", obs.getJSONObject(2).getJSONArray(VALUES).get(0));
        assertEquals("18.2", obs.getJSONObject(3).getJSONArray(VALUES).get(0));
        assertEquals(BuildConfig.VERSION_NAME, eventJSONObjectCaptor.getValue().getJSONObject(DETAILS).get(APP_VERSION_NAME));
        assertEquals(planIdentifier, eventJSONObjectCaptor.getValue().getJSONObject(DETAILS).get(PLAN_IDENTIFIER));
        assertEquals(locationId, eventJSONObjectCaptor.getValue().getJSONObject(DETAILS).get(LOCATION_PARENT));

        assertEquals(1, eventClientCaptor.getValue().size());

        Event event = eventClientCaptor.getValue().get(0).getEvent();
        assertEquals(4, event.getObs().size());
        assertEquals(structureOb, event.getObs().get(0).getValue());
        assertEquals("Residential Structure", event.getObs().get(1).getValue());
        assertEquals("Home", event.getObs().get(2).getValue());
        assertEquals("18.2", event.getObs().get(3).getValue());
        assertEquals(BuildConfig.VERSION_NAME, event.getDetails().get(APP_VERSION_NAME));
        assertEquals(planIdentifier, event.getDetails().get(PLAN_IDENTIFIER));
        assertEquals(locationId, event.getDetails().get(LOCATION_PARENT));
    }

    @Test
    public void testSaveMemberForm() throws JSONException {
        String form = AssetHandler.readFileFromAssetsFolder(org.smartregister.reveal.util.Constants.JsonForm.BLOOD_SCREENING_FORM, context);
        JSONObject formObject = new JSONObject(form);
        String entityId = UUID.randomUUID().toString();
        String taskId = UUID.randomUUID().toString();
        formObject.put("entity_id", entityId);
        JSONObject details = new JSONObject();
        details.put(TASK_IDENTIFIER, taskId);
        formObject.put(DETAILS, details);
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "business_status").put(VALUE, org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE);

        interactor.saveJsonForm(formObject.toString());

        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(anyString(), eventJSONObjectCaptor.capture());
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).processClient(eventClientCaptor.capture(), eq(true));
        assertEquals(BLOOD_SCREENING_EVENT, eventJSONObjectCaptor.getValue().getString("eventType"));
        JSONArray obs = eventJSONObjectCaptor.getValue().getJSONArray("obs");
        assertEquals(3, obs.length());
        assertEquals("1", obs.getJSONObject(0).getJSONArray(VALUES).get(0));
        assertEquals("Microscopy", obs.getJSONObject(1).getJSONArray(VALUES).get(0));
        assertEquals(org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE, obs.getJSONObject(2).getJSONArray(VALUES).get(0));
    }

    @Test
    public void testSaveCaseConfirmation() throws JSONException {
        String form = AssetHandler.readFileFromAssetsFolder(org.smartregister.reveal.util.Constants.JsonForm.CASE_CONFIRMATION_FORM, context);
        String planIdentifier = UUID.randomUUID().toString();
        PreferencesUtil.getInstance().setCurrentPlanId(planIdentifier);
        JSONObject formObject = new JSONObject(form);
        String entityId = UUID.randomUUID().toString();
        String taskId = UUID.randomUUID().toString();
        formObject.put("entity_id", entityId);
        JSONObject details = new JSONObject();
        details.put(TASK_IDENTIFIER, taskId);
        formObject.put(DETAILS, details);
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "business_status").put(VALUE, org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE);
        when(taskRepository.getTaskByIdentifier(taskId)).thenReturn(TestingUtils.getTask(taskId));

        interactor.saveJsonForm(formObject.toString());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(anyString(), eventJSONObjectCaptor.capture());
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).calculateBusinessStatus(any());
        assertEquals(org.smartregister.reveal.util.Constants.EventType.CASE_CONFIRMATION_EVENT, eventJSONObjectCaptor.getValue().getString("eventType"));
        assertFalse(RevealApplication.getInstance().getSynced());
    }

    @Test
    public void testFindLastEvent() {
        Event bloodScreeningEvent = RevealApplication.getInstance().getContext().getEventClientRepository().convert(TestingUtils.bloodScreeningEventJSON, Event.class);
        String query = String.format("select %s from %s where %s = ? and %s =? order by %s desc limit 1",
                EventClientRepository.event_column.json, EventClientRepository.Table.event.name(), EventClientRepository.event_column.baseEntityId, EventClientRepository.event_column.eventType, EventClientRepository.event_column.updatedAt);
        String eventBaseEntityId = "event-base-entity-id1";
        when(database.rawQuery(query, new String[]{eventBaseEntityId, BLOOD_SCREENING_EVENT})).thenReturn(createEventCursor());
        when(eventClientRepository.convert(anyString(), any())).thenReturn(bloodScreeningEvent);

        interactor = spy(interactor);
        interactor.findLastEvent(eventBaseEntityId, BLOOD_SCREENING_EVENT);
        verify(database, timeout(ASYNC_TIMEOUT)).rawQuery(query, new String[]{eventBaseEntityId, BLOOD_SCREENING_EVENT});
        verify(interactor, timeout(ASYNC_TIMEOUT)).handleLasteventFound(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue());
        assertEquals(bloodScreeningEvent, eventCaptor.getValue());
    }

    @Test
    public void testSaveRegisterStructureFormShouldInvokeOnStructureAddedWithTaskDetails() throws JSONException {
        String form = AssetHandler.readFileFromAssetsFolder(org.smartregister.reveal.util.Constants.JsonForm.ADD_STRUCTURE_FORM, context);
        String planIdentifier = UUID.randomUUID().toString();
        PreferencesUtil.getInstance().setCurrentPlanId(planIdentifier);
        JSONObject formObject = new JSONObject(form);
        String locationId = UUID.randomUUID().toString();
        formObject.put("entity_id", locationId);
        JSONObject details = new JSONObject();
        details.put(LOCATION_PARENT, locationId);
        formObject.put(DETAILS, details);
        Whitebox.setInternalState(interactor, "operationalAreaId", locationId);
        String structureOb = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[28.35228319086664,-15.421616685545176,0]},\"properties\":null}";
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "structure").put(VALUE, structureOb);
        double zoomLevel = 18.2d;
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), GeoWidgetFactory.ZOOM_LEVEL).put(VALUE, zoomLevel);
        interactor.saveJsonForm(formObject.toString());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(anyString(), eventJSONObjectCaptor.capture());
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).processClient(eventClientCaptor.capture(), eq(true));
        Task task = TestingUtils.getTask(locationId);
        Intent taskGeneratedIntent = new Intent("task_generated_event");
        taskGeneratedIntent.putExtra("task_generated", task);
        LocalBroadcastManager.getInstance(context).sendBroadcast(taskGeneratedIntent);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onStructureAdded(featureArgumentCaptor.capture(), featureCoordinatesCaptor.capture(), doubleArgumentCaptor.capture());
        assertEquals(task.getIdentifier(), featureArgumentCaptor.getValue().getStringProperty((TASK_IDENTIFIER)));
        assertEquals(task.getBusinessStatus(), featureArgumentCaptor.getValue().getStringProperty((TASK_BUSINESS_STATUS)));
        assertEquals(task.getStatus().name(), featureArgumentCaptor.getValue().getStringProperty((TASK_STATUS)));
        assertEquals(task.getCode(), featureArgumentCaptor.getValue().getStringProperty((TASK_CODE)));
        assertEquals(new JSONArray(new double[]{28.35228319086664, -15.421616685545176, 0}), featureCoordinatesCaptor.getValue());
        assertEquals(zoomLevel, doubleArgumentCaptor.getValue(), 0);

    }

    @Test
    public void testSaveCDDSupervisionForm() throws JSONException {
        String form = AssetHandler.readFileFromAssetsFolder(CDD_SUPERVISOR_DAILY_SUMMARY_FORM,context);
        JSONObject formObject = new JSONObject(form);
        String entityId = UUID.randomUUID().toString();
        String taskId = UUID.randomUUID().toString();
        formObject.put("entity_id",entityId);
        JSONObject details = new JSONObject();
        details.put(DETAILS,details);
        details.put(TASK_IDENTIFIER, taskId);
        JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject), "business_status").put(VALUE, org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE);
        interactor.saveJsonForm(formObject.toString());

        verify(eventClientRepository,timeout(ASYNC_TIMEOUT)).addEvent(anyString(),eventJSONObjectCaptor.capture());
        verify(clientProcessor,timeout(ASYNC_TIMEOUT)).processClient(eventClientCaptor.capture(),eq(true));
        assertEquals(CDD_SUPERVISOR_DAILY_SUMMARY,eventJSONObjectCaptor.getValue().get("eventType"));

    }

    @Test
    public void testEntityIdOnSaveTableAccountabilityForm() throws Exception {
        String form = AssetHandler.readFileFromAssetsFolder(TABLET_ACCOUNTABILITY_FORM,context);
        JSONObject formObject = new JSONObject(form);
        String locationName = "sampleLocation";
        String locationId = UUID.randomUUID().toString();
        JSONObject locationField = JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formObject),LOCATION);
        locationField.put("value",locationName);
        String searchQuery = String.format("select %s from  %s where %s = ? limit 1", ID_, STRUCTURES_TABLE, org.smartregister.reveal.util.Constants.DatabaseKeys.NAME);
        when(database.rawQuery(searchQuery, new String[]{locationName})).thenReturn(createFindStructureCursor(locationName,locationId));
        interactor.saveJsonForm(formObject.toString());
        verify(eventClientRepository,timeout(ASYNC_TIMEOUT)).addEvent(eq(locationId),eventJSONObjectCaptor.capture());
    }

    private Cursor createFamilyCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Constants.INTENT_KEY.BASE_ENTITY_ID
        });
        cursor.addRow(new Object[]{
                "69df212c-33a7-4443-a8d5-289e48d90468"
        });
        return cursor;
    }

    private Cursor createEventCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                "json"
        });
        cursor.addRow(new Object[]{
                TestingUtils.bloodScreeningEventJSON
        });
        return cursor;
    }

    private Cursor createFindStructureCursor(String locationName,String locationId){
        MatrixCursor cursor = new MatrixCursor(new String[]{locationName});
        cursor.addRow( new Object[]{locationId});

        return cursor;
    }

}
