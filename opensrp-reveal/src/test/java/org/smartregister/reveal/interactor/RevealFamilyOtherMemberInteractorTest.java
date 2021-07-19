package org.smartregister.reveal.interactor;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
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
import org.smartregister.Context;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.FamilyOtherMemberProfileContract;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.InteractorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.repository.EventClientRepository.event_column.syncStatus;
import static org.smartregister.reveal.util.FamilyConstants.EventType.ARCHIVE_FAMILY_MEMBER;

public class RevealFamilyOtherMemberInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FamilyOtherMemberProfileContract.Presenter presenter;

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private CommonPersonObject familyHeadPersonObject;

    @Mock
    private CommonPersonObjectClient client;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EventClientRepository eventClientRepository;

    @Mock
    private RevealClientProcessor clientProcessor;

    @Captor
    private ArgumentCaptor<JSONObject> jsonObjectArgumentCaptor;

    @Captor
    private ArgumentCaptor<JSONArray> jsonArrayArgumentCaptor;

    @Captor
    private ArgumentCaptor<List<EventClient>> eventClientArgumentCaptor;

    private RevealFamilyOtherMemberInteractor interactor;


    private JSONObject eventJSON = new JSONObject("{\"identifiers\":{},\"baseEntityId\":\"4d7e735c-1bac-4d16-bea2-e4e4628bb7d5\",\"locationId\":\"4c93649e-30c4-4316-a63b-0597854768fd\",\"eventDate\":\"2019-08-22T02:00:00.000+02:00\",\"eventType\":\"Family Member Registration\",\"formSubmissionId\":\"7cde014b-9dea-4866-b0a9-f6e76a534939\",\"providerId\":\"tak1\",\"duration\":0,\"obs\":[{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"same_as_fam_name\",\"parentCode\":\"\",\"values\":[\"true\"],\"set\":[],\"formSubmissionField\":\"same_as_fam_name\",\"humanReadableValues\":[]},{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"fam_name\",\"parentCode\":\"\",\"values\":[\"_\"],\"set\":[],\"formSubmissionField\":\"fam_name\",\"humanReadableValues\":[]},{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"is_family_head\",\"parentCode\":\"\",\"values\":[\"false\"],\"set\":[],\"formSubmissionField\":\"is_family_head\",\"humanReadableValues\":[]},{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"dob_unknown\",\"parentCode\":\"\",\"values\":[\"true\"],\"set\":[],\"formSubmissionField\":\"dob_unknown\",\"humanReadableValues\":[]},{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"sleeps_outdoors\",\"parentCode\":\"\",\"values\":[\"No\"],\"set\":[],\"formSubmissionField\":\"sleeps_outdoors\",\"humanReadableValues\":[]},{\"fieldType\":\"formsubmissionField\",\"fieldDataType\":\"text\",\"fieldCode\":\"citizenship\",\"parentCode\":\"\",\"values\":[\"Migrant-1\"],\"set\":[],\"formSubmissionField\":\"citizenship\",\"humanReadableValues\":[]},{\"fieldType\":\"concept\",\"fieldDataType\":\"start\",\"fieldCode\":\"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"parentCode\":\"\",\"values\":[\"2019-08-22 10:45:44\"],\"set\":[],\"formSubmissionField\":\"start\",\"humanReadableValues\":[]},{\"fieldType\":\"concept\",\"fieldDataType\":\"end\",\"fieldCode\":\"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"parentCode\":\"\",\"values\":[\"2019-08-22 10:47:17\"],\"set\":[],\"formSubmissionField\":\"end\",\"humanReadableValues\":[]},{\"fieldType\":\"concept\",\"fieldDataType\":\"text\",\"fieldCode\":\"162849AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\",\"parentCode\":\"\",\"values\":[\"true\"],\"set\":[],\"formSubmissionField\":\"wra\",\"humanReadableValues\":[]}],\"entityType\":\"ec_family_member\",\"details\":{\"locationUUID\":\"2a775846-fd14-4ef1-a1fe-05790d699cdd\",\"appVersionName\":\"2.3.3\"},\"version\":1566445637673,\"teamId\":\"0c7fd90f-5b99-4ed9-b337-0b42d51eab97\",\"team\":\"Tak\",\"dateCreated\":\"2019-08-22T07:50:39.441+02:00\",\"serverVersion\":1566453036087,\"clientApplicationVersion\":3,\"clientDatabaseVersion\":3,\"type\":\"Event\",\"id\":\"d45db5da-d4ae-40af-96e3-54b18b29acf8\",\"revision\":\"v1\"}");
    private JSONObject clientJSON = new JSONObject("{\"firstName\":\"asd\",\"lastName\":\"jaon\",\"birthdate\":\"2004-01-01T02:00:00.000+02:00\",\"birthdateApprox\":true,\"deathdateApprox\":false,\"gender\":\"Female\",\"relationships\":{\"family\":[\"18b12d74-23f9-4c8f-9442-6524ba0016a7\"]},\"baseEntityId\":\"4d7e735c-1bac-4d16-bea2-e4e4628bb7d5\",\"identifiers\":{\"opensrp_id\":\"16205205\"},\"addresses\":[],\"attributes\":{\"age\":\"15\",\"residence\":\"2a775846-fd14-4ef1-a1fe-05790d699cdd\"},\"dateCreated\":\"2019-08-22T07:50:35.147+02:00\",\"serverVersion\":1566453031831,\"clientApplicationVersion\":3,\"clientDatabaseVersion\":3,\"type\":\"Client\",\"id\":\"17295d17-bd1f-4ddd-b8d7-b730f5cb7620\",\"revision\":\"v1\"}");

    public RevealFamilyOtherMemberInteractorTest() throws JSONException {
    }

    @Before
    public void setUp() {
        Context.bindtypes = new ArrayList<>();
        interactor = new RevealFamilyOtherMemberInteractor();
        Whitebox.setInternalState(interactor, "commonRepository", commonRepository);
        Whitebox.setInternalState(interactor, "interactorUtils", new InteractorUtils(taskRepository, eventClientRepository));
        Whitebox.setInternalState(interactor, "clientProcessor", clientProcessor);
    }

    @Test
    public void testGetFamilyHead() {
        String familyHead = UUID.randomUUID().toString();
        when(commonRepository.findByBaseEntityId(familyHead)).thenReturn(familyHeadPersonObject);
        interactor.getFamilyHead(presenter, familyHead);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onFetchFamilyHead(familyHeadPersonObject);

    }


    @Test
    public void testArchiveFamilyMember() throws JSONException {
        String entityId = UUID.randomUUID().toString();
        when(client.getCaseId()).thenReturn(entityId);
        JSONObject events = new JSONObject();
        events.put("client", clientJSON);
        events.put("events", new JSONArray().put(eventJSON));
        when(eventClientRepository.getEventsByBaseEntityId(entityId)).thenReturn(events);

        interactor.archiveFamilyMember(presenter, client);
        //assert tasks are cancelled
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).cancelTasksForEntity(entityId);

        //assert tasks are archived
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).archiveTasksForEntity(entityId);


        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).getEventsByBaseEntityId(entityId);
        //assert both events and client is updated
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addorUpdateClient(eq(entityId), jsonObjectArgumentCaptor.capture());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertEvents(jsonArrayArgumentCaptor.capture(), eq(0L));

        DateTime now = new DateTime();
        JSONObject client = jsonObjectArgumentCaptor.getValue();
        assertEquals(0, Minutes.minutesBetween(now, new DateTime(client.getString("dateVoided"))).getMinutes());
        assertEquals(0, Minutes.minutesBetween(now, new DateTime(client.getJSONObject("attributes").getString("dateRemoved"))).getMinutes());
        assertEquals(BaseRepository.TYPE_Unsynced, client.getString(syncStatus.name()));

        for (int i = 0; i < jsonArrayArgumentCaptor.getValue().length(); i++) {
            assertEquals(0, Minutes.minutesBetween(now, new DateTime(jsonArrayArgumentCaptor.getValue().getJSONObject(i).getString("dateVoided"))).getMinutes());
            //assertEquals(BaseRepository.TYPE_Unsynced, jsonArrayArgumentCaptor.getValue().getJSONObject(i).getString(client_column.syncStatus.name()));
        }


        //assert a an archive event is generated
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).addEvent(eq(entityId), jsonObjectArgumentCaptor.capture());

        JSONObject archiveEvent = jsonObjectArgumentCaptor.getAllValues().get(1);
        assertEquals(entityId, archiveEvent.getString("baseEntityId"));
        assertEquals(ARCHIVE_FAMILY_MEMBER, archiveEvent.getString("eventType"));


        //assert client processing is done
        verify(clientProcessor, timeout(ASYNC_TIMEOUT)).processClient(eventClientArgumentCaptor.capture(), eq(true));
        assertEquals(1, eventClientArgumentCaptor.getValue().size());
        assertEquals(entityId, eventClientArgumentCaptor.getValue().get(0).getEvent().getBaseEntityId());
        assertEquals(ARCHIVE_FAMILY_MEMBER, eventClientArgumentCaptor.getValue().get(0).getEvent().getEventType());


    }
}
