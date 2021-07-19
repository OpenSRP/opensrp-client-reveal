package org.smartregister.reveal.sync;

import android.content.Context;

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
import org.smartregister.domain.Address;
import org.smartregister.domain.Client;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Obs;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.util.JsonFormUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.BEDNET_DISTRIBUTION_EVENT;
import static org.smartregister.reveal.util.Constants.BEHAVIOUR_CHANGE_COMMUNICATION;
import static org.smartregister.reveal.util.Constants.EventType.IRS_VERIFICATION;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_PARENT;
import static org.smartregister.reveal.util.Constants.Properties.LOCATION_UUID;
import static org.smartregister.reveal.util.Constants.Properties.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.FamilyConstants.EventType.UPDATE_FAMILY_REGISTRATION;

/**
 * Created by samuelgithengi on 3/13/19.
 */
public class RevealClientProcessorTest extends BaseUnitTest {


    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EventClientRepository eventClientRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private StructureRepository structureRepository;

    @Captor
    private ArgumentCaptor<Task> taskCaptor;

    @Captor
    private ArgumentCaptor<Location> structureCaptor;

    @Captor
    private ArgumentCaptor<Event> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<Client> clientArgumentCaptor;

    private Context context = RuntimeEnvironment.application;

    private RevealClientProcessor clientProcessor;

    private Event event;

    private Event sprayedEvent;

    private Task task;

    @Before
    public void setUp() {
        clientProcessor = new RevealClientProcessor(context);
        String eventJSON = "{\"baseEntityId\":\"b9f60dfd-799e-41f7-9d3c-1370d894bc6d\",\"duration\":0,\"entityType\":\"Structure\",\"eventDate\":\"2019-03-07T00:00:00.000+0100\",\"eventType\":\"Spray\",\"formSubmissionId\":\"f4423e04-047a-40d2-b5f2-baf2a9c831b2\",\"locationId\":\"79496c6b-cb29-405a-bcdd-0dcf8afddf55\",\"obs\":[{\"fieldCode\":\"structureType\",\"fieldDataType\":\"text\",\"fieldType\":\"formsubmissionField\",\"formSubmissionField\":\"structureType\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"Non-Residential Structure\"]},{\"fieldCode\":\"visit_number\",\"fieldDataType\":\"text\",\"fieldType\":\"formsubmissionField\",\"formSubmissionField\":\"visit_number\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"Mop-up\"]},{\"fieldCode\":\"nonresidentialtype\",\"fieldDataType\":\"text\",\"fieldType\":\"formsubmissionField\",\"formSubmissionField\":\"nonresidentialtype\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"Offices\"]},{\"fieldCode\":\"business_status\",\"fieldDataType\":\"text\",\"fieldType\":\"formsubmissionField\",\"formSubmissionField\":\"business_status\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"Not Sprayable\"]},{\"fieldCode\":\"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"start\",\"fieldType\":\"concept\",\"formSubmissionField\":\"start\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"2019-03-07 16:47:04\"]},{\"fieldCode\":\"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"end\",\"fieldType\":\"concept\",\"formSubmissionField\":\"end\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"2019-03-07 16:47:32\"]},{\"fieldCode\":\"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"deviceid\",\"fieldType\":\"concept\",\"formSubmissionField\":\"deviceid\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"358240051111110\"]},{\"fieldCode\":\"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"subscriberid\",\"fieldType\":\"concept\",\"formSubmissionField\":\"subscriberid\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"310260000000000\"]},{\"fieldCode\":\"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"simserial\",\"fieldType\":\"concept\",\"formSubmissionField\":\"simserial\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"89014103211118510720\"]},{\"fieldCode\":\"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"fieldDataType\":\"phonenumber\",\"fieldType\":\"concept\",\"formSubmissionField\":\"phonenumber\",\"humanReadableValues\":[],\"parentCode\":\"\",\"values\":[\"+15555215554\"]}],\"providerId\":\"swana\",\"team\":\"Botswana\",\"teamId\":\"e5470c0b-2349-45ec-bdd1-e0ed5a120a87\",\"version\":1551973652706,\"dateCreated\":\"2019-03-07T16:47:32.706+0100\",\"type\":\"Event\",\"details\":{\"taskIdentifier\":\"c5352384-6578-4477-bd66-1fa95f4006c7\",\"taskBusinessStatus\":\"Not Visited\",\"taskStatus\":\"READY\",\"locationUUID\":\"0d9e76c7-5b83-4fe3-8a50-e3f0dd8d883e\",\"locationVersion\":\"0\"}}";
        event = JsonFormUtils.gson.fromJson(eventJSON, Event.class);

        sprayedEvent = TestingUtils.getSprayedEvent();
        Whitebox.setInternalState(clientProcessor, "eventClientRepository", eventClientRepository);
        Whitebox.setInternalState(clientProcessor, "taskRepository", taskRepository);
        Whitebox.setInternalState(clientProcessor, "structureRepository", structureRepository);

        String taskJSON = "{\"identifier\":\"634fa9fa-736d-4298-96aa-3de68ac02cae\",\"campaignIdentifier\":\"IRS_2019_S1\",\"groupIdentifier\":\"3537\",\"status\":\"Completed\",\"businessStatus\":\"Sprayed\",\"priority\":3,\"code\":\"IRS\",\"description\":\"Spray House\",\"focus\":\"IRS Visit\",\"for\":\"156727\",\"executionStartDate\":\"2018-11-10T2200\",\"executionEndDate\":null,\"authoredOn\":\"2018-11-29T0342\",\"lastModified\":\"2019-01-31T1312\",\"syncStatus\":\"Synced\",\"owner\":\"demoMTI\",\"note\":null,\"serverVersion\":1548933177074}";
        task = taskGson.fromJson(taskJSON, Task.class);

    }

    @Test
    public void testCalculateBusinessStatusUsingCalculatedFormStatus() {
        assertEquals("Not Sprayable", clientProcessor.calculateBusinessStatus(event));
        assertEquals("Sprayed", clientProcessor.calculateBusinessStatus(sprayedEvent));
    }

    @Test
    public void testCalculateBusinessStatusWithoutCalculatedFormStatus() {
        Obs businessStatusObs = event.findObs(null, false, JsonForm.BUSINESS_STATUS);
        event.getObs().remove(businessStatusObs);
        assertEquals("Not Sprayable", clientProcessor.calculateBusinessStatus(event));


        businessStatusObs = sprayedEvent.findObs(null, false, JsonForm.BUSINESS_STATUS);
        sprayedEvent.getObs().remove(businessStatusObs);
        assertEquals("Sprayed", clientProcessor.calculateBusinessStatus(sprayedEvent));
    }

    @Test
    public void testCalculateBusinessStatusWithoutStructureType() {
        Obs businessStatusObs = event.findObs(null, false, JsonForm.BUSINESS_STATUS);
        Obs structureTypeObs = event.findObs(null, false, JsonForm.STRUCTURE_TYPE);
        event.getObs().remove(businessStatusObs);
        event.getObs().remove(structureTypeObs);
        assertNull(clientProcessor.calculateBusinessStatus(event));
    }

    @Test
    public void testCalculateBusinessStatusUsingNullSprayStatus() {
        Obs businessStatusObs = sprayedEvent.findObs(null, false, JsonForm.BUSINESS_STATUS);
        sprayedEvent.getObs().remove(businessStatusObs);

        Obs sprayStatus = sprayedEvent.findObs(null, false, JsonForm.SPRAY_STATUS);
        sprayedEvent.getObs().remove(sprayStatus);

        assertNull(clientProcessor.calculateBusinessStatus(sprayedEvent));
    }


    @Test
    public void testCalculateBusinessStatusUsingSprayStatus() {
        Obs businessStatusObs = sprayedEvent.findObs(null, false, JsonForm.BUSINESS_STATUS);
        sprayedEvent.getObs().remove(businessStatusObs);

        assertEquals("Sprayed", clientProcessor.calculateBusinessStatus(sprayedEvent));
    }


    @Test
    public void testGetInstance() {
        assertNotNull(RevealClientProcessor.getInstance(context));
    }

    @Test
    public void testProcessEventClientWithoutTasksAndLocations() {
        Whitebox.setInternalState(clientProcessor, "eventClientRepository", eventClientRepository);
        clientProcessor.processClient(Arrays.asList(new EventClient(event, null), new EventClient(sprayedEvent, null)), false);

        verify(eventClientRepository, never()).markEventAsTaskUnprocessed(anyString());
    }

    @Test
    public void testProcessEventClientShouldUpdateTaskStatusOnlyForRemoteEvents() {

        when(taskRepository.getTaskByIdentifier(sprayedEvent.getDetails().get(Properties.TASK_IDENTIFIER))).thenReturn(task);
        clientProcessor.processClient(Collections.singletonList(new EventClient(sprayedEvent, null)));

        verify(eventClientRepository, never()).markEventAsTaskUnprocessed(sprayedEvent.getFormSubmissionId());
        verify(eventClientRepository,never()).markEventAsSynced(sprayedEvent.getFormSubmissionId());

        verify(taskRepository).addOrUpdate(taskCaptor.capture());

        assertEquals("Sprayed", taskCaptor.getValue().getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED, taskCaptor.getValue().getStatus());
        assertEquals(BaseRepository.TYPE_Synced, taskCaptor.getValue().getSyncStatus());
    }


    @Test
    public void testProcessEventClientShouldNotMarkAsSynchedEventsWithoutServerVersion() {
        sprayedEvent.setServerVersion(0l);
        when(taskRepository.getTaskByIdentifier(sprayedEvent.getDetails().get(Properties.TASK_IDENTIFIER))).thenReturn(task);
        clientProcessor.processClient(Collections.singletonList(new EventClient(sprayedEvent, null)));

        verify(eventClientRepository, never()).markEventAsTaskUnprocessed(sprayedEvent.getFormSubmissionId());
        verify(eventClientRepository, never()).markEventAsSynced(sprayedEvent.getFormSubmissionId());

        verify(taskRepository).addOrUpdate(taskCaptor.capture());

        assertEquals("Sprayed", taskCaptor.getValue().getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED, taskCaptor.getValue().getStatus());
        assertEquals(BaseRepository.TYPE_Synced, taskCaptor.getValue().getSyncStatus());
    }


    @Test
    public void testProcessEventClientShouldUpdateTaskStatusAndSyncStatusOnlyForLocalEvents() {

        when(taskRepository.getTaskByIdentifier(sprayedEvent.getDetails().get(Properties.TASK_IDENTIFIER))).thenReturn(task);
        clientProcessor.processClient(Collections.singletonList(new EventClient(sprayedEvent, null)), true);

        verify(eventClientRepository, never()).markEventAsTaskUnprocessed(sprayedEvent.getFormSubmissionId());
        verify(eventClientRepository, never()).markEventAsSynced(sprayedEvent.getFormSubmissionId());

        verify(taskRepository).addOrUpdate(taskCaptor.capture());

        assertEquals("Sprayed", taskCaptor.getValue().getBusinessStatus());
        assertEquals(Task.TaskStatus.COMPLETED, taskCaptor.getValue().getStatus());
        assertEquals(BaseRepository.TYPE_Unsynced, taskCaptor.getValue().getSyncStatus());
    }

    @Test
    public void testProcessEventClientShouldUpdateLocation() {

        Location structure = new Location();
        structure.setProperties(new LocationProperty());
        when(structureRepository.getLocationById(sprayedEvent.getBaseEntityId())).thenReturn(structure);
        clientProcessor.processClient(Collections.singletonList(new EventClient(sprayedEvent, null)), true);

        verify(structureRepository).addOrUpdate(structureCaptor.capture());
        assertEquals("Residential Structure", structureCaptor.getValue().getProperties().getType());

    }

    @Test
    public void testProcessUpdateFamilyRegistrationEvent() throws Exception {

        String houseNumber = "Ted House";
        String baseEntityId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        event.setEventId(eventId);
        event.setEventType(UPDATE_FAMILY_REGISTRATION);
        event.getDetails().put(LOCATION_UUID, "baseEntityId");
        Location structure = new Location();
        LocationProperty property = new LocationProperty();
        property.setName("Robin House");
        structure.setProperties(property);
        structure.setProperties(new LocationProperty());
        structure.setSyncStatus(BaseRepository.TYPE_Synced);
        Client client = new Client(baseEntityId);
        Address address = new Address();
        Map<String, String> addressFields = new HashMap<>();
        addressFields.put("address2", houseNumber);
        address.setAddressFields(addressFields);
        client.setAddresses(Collections.singletonList(address));

        when(structureRepository.getLocationById(event.getDetails().get(LOCATION_UUID))).thenReturn(structure);

        clientProcessor = spy(clientProcessor);

        clientProcessor.processClient(Collections.singletonList(new EventClient(event, client)), true);

        verify(structureRepository).addOrUpdate(structureCaptor.capture());
        assertEquals(houseNumber, structureCaptor.getValue().getProperties().getName());
        assertEquals(BaseRepository.TYPE_Created, structureCaptor.getValue().getSyncStatus());

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(client.getBaseEntityId(), clientArgumentCaptor.getValue().getBaseEntityId());

    }

    @Test
    public void testProcessUpdateFamilyRegistrationEventWithNullAddress() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        event.setEventId(eventId);
        event.setEventType(UPDATE_FAMILY_REGISTRATION);
        event.getDetails().put(LOCATION_UUID, "baseEntityId");
        Location structure = new Location();
        LocationProperty property = new LocationProperty();
        property.setName("Robin House");
        structure.setProperties(property);
        structure.setProperties(new LocationProperty());
        structure.setSyncStatus(BaseRepository.TYPE_Unsynced);
        Client client = new Client(baseEntityId);
        client.setFirstName("Jordan");
        Address address = new Address();
        Map<String, String> addressFields = new HashMap<>();
        address.setAddressFields(addressFields);
        client.setAddresses(Collections.singletonList(address));

        when(structureRepository.getLocationById(event.getDetails().get(LOCATION_UUID))).thenReturn(structure);

        clientProcessor = spy(clientProcessor);

        clientProcessor.processClient(Collections.singletonList(new EventClient(event, client)), true);

        verify(structureRepository).addOrUpdate(structureCaptor.capture());
        assertEquals(client.getFirstName(), structureCaptor.getValue().getProperties().getCustomProperties().get(STRUCTURE_NAME));
        assertNull(structureCaptor.getValue().getProperties().getName());
        assertEquals(BaseRepository.TYPE_Unsynced, structureCaptor.getValue().getSyncStatus());

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(client.getBaseEntityId(), clientArgumentCaptor.getValue().getBaseEntityId());

    }

    @Test
    public void testProcessRegisterStructureEvent() throws Exception {

        String locationParent = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        String baseEntityId = UUID.randomUUID().toString();
        event.setBaseEntityId(baseEntityId);
        event.getDetails().put(LOCATION_PARENT, locationParent);
        event.setEventId(eventId);
        event.setEventType(REGISTER_STRUCTURE_EVENT);
        clientProcessor = spy(clientProcessor);

        clientProcessor.processClient(Collections.singletonList(new EventClient(event, null)), true);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());

    }

    @Test
    public void testProcessLarvalDippingEvent() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        setUpLocationInterventionEventTest(LARVAL_DIPPING_EVENT, baseEntityId);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());
    }

    @Test
    public void testProcessBednetDistributionEvent() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        setUpLocationInterventionEventTest(BEDNET_DISTRIBUTION_EVENT, baseEntityId);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());
    }

    @Test
    public void testProcessBehaviouralChangeEvent() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        setUpLocationInterventionEventTest(BEHAVIOUR_CHANGE_COMMUNICATION, baseEntityId);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());
    }

    @Test
    public void testProcessMosquitoCollectionEvent() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        setUpLocationInterventionEventTest(MOSQUITO_COLLECTION_EVENT, baseEntityId);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());
    }

    @Test
    public void testProcessIRSVerificationEvent() throws Exception {

        String baseEntityId = UUID.randomUUID().toString();
        setUpLocationInterventionEventTest(IRS_VERIFICATION, baseEntityId);

        verify(clientProcessor).processEvent(eventArgumentCaptor.capture(), clientArgumentCaptor.capture(), any());
        assertEquals(event.getEventId(), eventArgumentCaptor.getValue().getEventId());
        assertEquals(event.getEventType(), eventArgumentCaptor.getValue().getEventType());
        assertEquals(baseEntityId, clientArgumentCaptor.getValue().getBaseEntityId());

    }

    private void setUpLocationInterventionEventTest(String eventType, String baseEntityId) throws Exception {
        String taskIdentifier = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        event.setBaseEntityId(baseEntityId);
        event.getDetails().put(TASK_IDENTIFIER, taskIdentifier);
        event.setEventId(eventId);
        event.setEventType(eventType);
        clientProcessor = spy(clientProcessor);

        clientProcessor.processClient(Collections.singletonList(new EventClient(event, null)), true);
    }


}
