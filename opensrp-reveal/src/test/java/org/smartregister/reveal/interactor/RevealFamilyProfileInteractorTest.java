package org.smartregister.reveal.interactor;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import org.smartregister.cloudant.models.Client;
import org.smartregister.cloudant.models.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.FamilyProfileContract;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.FamilyConstants.EventType;
import org.smartregister.reveal.util.FamilyConstants.TABLE_NAME;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.sync.ClientProcessorForJava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.repository.BaseRepository.TYPE_Unsynced;

/**
 * Created by samuelgithengi on 4/25/19.
 */
public class RevealFamilyProfileInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FamilyProfileContract.Presenter presenter;

    @Mock
    private CommonRepository commonRepository;

    @Mock
    private EventClientRepository eventClientRepository;

    @Mock
    private RevealClientProcessor clientProcessor;

    @Mock
    private SQLiteDatabase sqLiteDatabase;

    @Captor
    private ArgumentCaptor<JSONArray> jsonArrayArgumentCaptor;

    @Mock
    private InteractorUtils interactorUtils;

    @Mock
    private TaskRepository taskRepository;

    private RevealFamilyProfileInteractor interactor;

    private Context context = RuntimeEnvironment.application;

    protected final int ASYNC_TIMEOUT = 4000;


    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        interactor = new RevealFamilyProfileInteractor(presenter);
        AppExecutors appExecutors = new AppExecutors(Executors.newSingleThreadExecutor(),
                Executors.newSingleThreadExecutor(), Executors.newSingleThreadExecutor());
        Whitebox.setInternalState(interactor, "appExecutors", appExecutors);
        Whitebox.setInternalState(interactor, "commonRepository", commonRepository);
        Whitebox.setInternalState(interactor, "eventClientRepository", eventClientRepository);
        Whitebox.setInternalState(interactor, "clientProcessor", clientProcessor);
        Whitebox.setInternalState(interactor, "interactorUtils", interactorUtils);
        Whitebox.setInternalState(interactor, "taskRepository", taskRepository);

    }

    @Test
    public void testGetClientProcessorForJava() {
        ClientProcessorForJava clientProcessor = interactor.getClientProcessorForJava();
        assertNotNull(clientProcessor);
        assertTrue(clientProcessor instanceof RevealClientProcessor);
    }

    @Test
    public void testUpdateFamilyMemberSurnameWithoutFamilyMembers() {
        String baseEntityId = UUID.randomUUID().toString();
        interactor.updateFamilyMemberName((Client) new Client().withBaseEntityId(baseEntityId),
                new Event(), "Victor");
        verify(commonRepository, timeout(ASYNC_TIMEOUT)).findByRelational_IDs(baseEntityId);
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertClients(new JSONArray());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertEvents(new JSONArray(), 0);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onMembersUpdated();
    }

    @Test
    public void testUpdateFamilyMemberSurnameNameUpdatesClientAndGeneratesEvents() throws JSONException {
        String baseEntityId = UUID.randomUUID().toString();
        CommonPersonObjectClient client = TestingUtils.getCommonPersonObjectClient();
        CommonPersonObject familyMember = new CommonPersonObject(client.getCaseId(), null, client.getDetails(), "ec_family_member");
        familyMember.setColumnmaps(client.getColumnmaps());
        when(commonRepository.findByRelational_IDs(baseEntityId)).thenReturn(Collections.singletonList(familyMember));
        when(eventClientRepository.getClientByBaseEntityId(familyMember.getCaseId())).thenReturn(new JSONObject());
        interactor.updateFamilyMemberName((Client) new Client().withFirstName("Otale").withBaseEntityId(baseEntityId),
                new Event(), "Otala");
        verify(commonRepository, timeout(ASYNC_TIMEOUT)).findByRelational_IDs(baseEntityId);
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertClients(jsonArrayArgumentCaptor.capture());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertEvents(jsonArrayArgumentCaptor.capture(), eq(0l));
        verify(presenter, timeout(ASYNC_TIMEOUT)).onMembersUpdated();
        assertEquals(2, jsonArrayArgumentCaptor.getAllValues().size());

        //assert Clients is updated
        JSONArray clients = jsonArrayArgumentCaptor.getAllValues().get(0);
        JSONObject clientJSONObject = clients.getJSONObject(0);
        assertEquals(1, clients.length());
        System.out.println(clientJSONObject);
        assertEquals("Otale", clientJSONObject.getString("lastName"));
        assertEquals(TYPE_Unsynced, clientJSONObject.getString("syncStatus"));

        //assert event is generated
        JSONArray events = jsonArrayArgumentCaptor.getAllValues().get(1);
        JSONObject eventJSONObject = events.getJSONObject(0);
        assertEquals(1, events.length());
        assertEquals(TYPE_Unsynced, eventJSONObject.getString("syncStatus"));
        assertEquals(EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, eventJSONObject.getString("eventType"));
        assertEquals(familyMember.getCaseId(), eventJSONObject.getString("baseEntityId"));
        assertEquals(TABLE_NAME.FAMILY_MEMBER, eventJSONObject.getString("entityType"));

    }


    @Test
    public void testUpdateFamilyMemberSurnameWithErrorCallsOnMembersUpdated() {
        String baseEntityId = UUID.randomUUID().toString();
        CommonPersonObjectClient client = TestingUtils.getCommonPersonObjectClient();
        CommonPersonObject familyMember = new CommonPersonObject(client.getCaseId(), null, client.getDetails(), "ec_family_member");
        familyMember.setColumnmaps(client.getColumnmaps());
        when(commonRepository.findByRelational_IDs(baseEntityId)).thenReturn(Collections.singletonList(familyMember));
        when(eventClientRepository.getClientByBaseEntityId(familyMember.getCaseId())).thenReturn(null);
        interactor.updateFamilyMemberName((Client) new Client().withFirstName("Otale").withBaseEntityId(baseEntityId),
                new Event(), "Otala");
        verify(commonRepository, timeout(ASYNC_TIMEOUT)).findByRelational_IDs(baseEntityId);
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertClients(new JSONArray());
        verify(eventClientRepository, timeout(ASYNC_TIMEOUT)).batchInsertEvents(new JSONArray(), 0l);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onMembersUpdated();
    }

    @Test
    public void testProcessClient() {
        List<EventClient> eventClientList = Collections.singletonList(new EventClient(new org.smartregister.domain.Event()));
        interactor.processClient(eventClientList);
        verify(clientProcessor).processClient(eventClientList, true);
    }


    @Test
    public void testArchiveFamily() {
        when(eventClientRepository.getWritableDatabase()).thenReturn(sqLiteDatabase);
        String familyId = UUID.randomUUID().toString();
        String structureId = UUID.randomUUID().toString();
        List<String> familyMembers = new ArrayList<>(Arrays.asList("m1", "m2", "m3"));

        when(commonRepository.findSearchIds(anyString())).thenReturn(familyMembers);

        Task task = TestingUtils.getTask(structureId);
        interactor.archiveFamily(familyId, structureId);

        for (String familyMember : new ArrayList<>(familyMembers))
            verify(interactorUtils, timeout(ASYNC_TIMEOUT)).archiveClient(familyMember, false);
        verify(interactorUtils, timeout(ASYNC_TIMEOUT)).archiveClient(familyId, true);

        verify(taskRepository, timeout(ASYNC_TIMEOUT)).archiveTasksForEntity(structureId);
        verify(taskRepository, timeout(ASYNC_TIMEOUT)).cancelTasksForEntity(structureId);
        Intent taskGeneratedIntent = new Intent("task_generated_event");
        taskGeneratedIntent.putExtra("task_generated", task);
        LocalBroadcastManager.getInstance(context).sendBroadcast(taskGeneratedIntent);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onArchiveFamilyCompleted(true, task);

    }

}
