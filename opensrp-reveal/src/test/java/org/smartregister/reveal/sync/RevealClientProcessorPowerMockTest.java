package org.smartregister.reveal.sync;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.smartregister.CoreLibrary;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Obs;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.AppExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Constants.TASK_RESET_EVENT;

/**
 * @author Vincent Karuri
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({RevealClientProcessor.class, Utils.class, PreferencesUtil.class, RevealApplication.class, CoreLibrary.class})
public class RevealClientProcessorPowerMockTest {

    private RevealClientProcessor clientProcessor;
    private TaskRepository taskRepository = mock(TaskRepository.class);
    private EventClientRepository eventClientRepository = mock(EventClientRepository.class);
    private StructureRepository structureRepository = mock(StructureRepository.class);
    private final String FORM_SUBMISSION_ID = "form_submission_id";
    private Task task;
    private Event event;
    private String TRAP_SET_DATE = "11/11/1977";
    private String TRAP_FOLLOW_UP_DATE = "02/09/1983";

    @Before
    public void setUp() {
        clientProcessor = spy(Whitebox.newInstance(RevealClientProcessor.class));
        Whitebox.setInternalState(clientProcessor, "appExecutors", new AppExecutors());
    }

    @Test
    public void testProcessClientShouldCallProcessSprayEventWhenIsSprayEventType() throws Exception {
        mockStaticMethods();
        mockRepositories();

        List<EventClient> eventClients = new ArrayList<>();
        event.setEventType(SPRAY_EVENT);

        EventClient eventClient = new EventClient(event, null);
        eventClients.add(eventClient);

        PowerMockito.doReturn(new ClientClassification()).when(clientProcessor, "assetJsonToJava", anyString(), any());
        clientProcessor.processClient(eventClients, true);

        verifyPrivate(clientProcessor, times(1)).invoke("processEvent", eq(event), any(ClientClassification.class), anyBoolean(), eq(JsonForm.STRUCTURE_TYPE));
        verify(structureRepository).addOrUpdate(any(Location.class));
    }

    @Test
    public void testProcessClientShouldCallProcessMosquitoHarvestingEventWhenIsMosquitoCollectionEventType() throws Exception {
        mockStaticMethods();
        mockRepositories();

        List<EventClient> eventClients = new ArrayList<>();
        event.setEventType(MOSQUITO_COLLECTION_EVENT);

        EventClient eventClient = new EventClient(event, null);
        eventClients.add(eventClient);

        PowerMockito.doReturn(new ClientClassification()).when(clientProcessor, "assetJsonToJava", anyString(), any());
        clientProcessor.processClient(eventClients, true);

        verifyPrivate(clientProcessor, times(1)).invoke("processEvent", eq(event), any(ClientClassification.class), anyBoolean());
    }


    @Test
    public void testProcessClientShouldCallProcessPOATEventWhenIsPOATEventType() throws Exception {
        mockStaticMethods();
        mockRepositories();

        List<EventClient> eventClients = new ArrayList<>();
        event.setEventType(Constants.EventType.PAOT_EVENT);
        event.withObs(new Obs("", "", JsonForm.PAOT_STATUS, "", Collections.singletonList("Active"), "", "", new ArrayList<>()));

        EventClient eventClient = new EventClient(event, null);
        eventClients.add(eventClient);

        PowerMockito.doReturn(new ClientClassification()).when(clientProcessor, "assetJsonToJava", anyString(), any());
        clientProcessor.processClient(eventClients, true);

        verifyPrivate(clientProcessor, times(1)).invoke("processEvent", eq(event), any(ClientClassification.class), anyBoolean(), eq(JsonForm.PAOT_STATUS));
        verify(structureRepository).addOrUpdate(any(Location.class));
        verify(taskRepository).addOrUpdate(any(Task.class));
    }

    @Test
    public void testUpdateTaskShouldUpdateTask() throws Exception {
        mockRepositories();

        Whitebox.invokeMethod(clientProcessor, "updateTask", event, true);

        verify(taskRepository, times(1)).addOrUpdate(eq(task));
        assertEquals(task.getStatus(), Task.TaskStatus.COMPLETED);
    }

    @Test
    public void testUpdateTaskShouldMarkEventAsTaskUnprocessed() throws Exception {
        mockRepositories();

        task = null;
        doReturn(task).when(taskRepository).getTaskByIdentifier(anyString());

        Whitebox.invokeMethod(clientProcessor, "updateTask", event, false);

        verify(eventClientRepository,never()).markEventAsTaskUnprocessed(eq(FORM_SUBMISSION_ID));
    }

    @Test
    public void testUpdateTaskLocallyShouldNotMarkEventAsSynced() throws Exception {
        mockRepositories();

        Whitebox.invokeMethod(clientProcessor, "updateTask", event, false);

        verify(eventClientRepository, never()).markEventAsSynced(eq(FORM_SUBMISSION_ID));
    }

    @Test
    public void testUpdateTaskShouldMarkEventAsSynced() throws Exception {
        mockRepositories();
        mockStatic(CoreLibrary.class);
        CoreLibrary coreLibrary = mock(CoreLibrary.class);
        PowerMockito.when(coreLibrary.isPeerToPeerProcessing()).thenReturn(false);
        PowerMockito.when(CoreLibrary.getInstance()).thenReturn(coreLibrary);
        event.setServerVersion(System.currentTimeMillis());

        Whitebox.invokeMethod(clientProcessor, "updateTask", event, false);

        verify(eventClientRepository,never()).markEventAsSynced(eq(FORM_SUBMISSION_ID));
    }

    @Test
    public void testUpdateTaskShouldSetTaskSyncStatusToUnsynced() throws Exception {
        mockRepositories();

        Whitebox.invokeMethod(clientProcessor, "updateTask", event, true);

        assertEquals(task.getSyncStatus(), BaseRepository.TYPE_Unsynced);
    }

    @Test
    public void testUpdateTaskShouldNotBeCalledForResetTaskEvent() throws Exception {
        mockStaticMethods();
        mockRepositories();

        List<EventClient> eventClients = new ArrayList<>();
        event.setEventType(TASK_RESET_EVENT);
        event.withObs(new Obs("", "", TASK_RESET_EVENT, "", Collections.singletonList("Active"), "", "", new ArrayList<>()));

        EventClient eventClient = new EventClient(event, null);
        eventClients.add(eventClient);

        PowerMockito.doReturn(new ClientClassification()).when(clientProcessor, "assetJsonToJava", anyString(), any());
        clientProcessor.processClient(eventClients, true);

        verify(structureRepository, never()).addOrUpdate(any(Location.class));
        verify(taskRepository, never()).addOrUpdate(any(Task.class));
    }

    private void mockStaticMethods() {
        mockStatic(Utils.class);
        mockStatic(PreferencesUtil.class);
        mockStatic(RevealApplication.class);

        RevealApplication application = mock(RevealApplication.class);
        when(RevealApplication.getInstance()).thenReturn(application);
        when(application.getApplicationContext()).thenReturn(mock(Context.class));
        doReturn(structureRepository).when(application).getStructureRepository();
        doReturn(taskRepository).when(application).getTaskRepository();

        org.smartregister.Context openSRPContext = mock(org.smartregister.Context.class);
        when(application.getContext()).thenReturn(openSRPContext);
        doReturn(eventClientRepository).when(openSRPContext).getEventClientRepository();

        PreferencesUtil preferencesUtil = mock(PreferencesUtil.class);
        PowerMockito.when(preferencesUtil.getCurrentOperationalArea()).thenReturn("");

        PowerMockito.when(PreferencesUtil.getInstance()).thenReturn(preferencesUtil);
        PowerMockito.when(Utils.getOperationalAreaLocation(anyString())).thenReturn(new Location());
    }

    private void mockRepositories() {
        // generic event
        event = spy(new Event());

        Obs obs = new Obs();
        obs.setFieldCode(JsonForm.STRUCTURE_TYPE);
        obs.setValue("Non residential");
        obs.setFormSubmissionField("form_submission_field_1");
        event.addObs(obs);


        obs = new Obs();
        obs.setFieldCode(JsonForm.TRAP_SET_DATE);
        obs.setValue(TRAP_SET_DATE);
        obs.setFormSubmissionField("form_submission_field_2");
        event.addObs(obs);

        obs = new Obs();
        obs.setFieldCode(JsonForm.TRAP_FOLLOW_UP_DATE);
        obs.setValue(TRAP_FOLLOW_UP_DATE);
        obs.setFormSubmissionField("form_submission_field_3");
        event.addObs(obs);

        doReturn(FORM_SUBMISSION_ID).when(event).getFormSubmissionId();

        Map<String, String> details = new HashMap<>();
        details.put(TASK_IDENTIFIER, "");
        doReturn(details).when(event).getDetails();

        // generic task repository
        taskRepository = mock(TaskRepository.class);
        task = new Task();
        task.setSyncStatus(BaseRepository.TYPE_Unsynced);
        Whitebox.setInternalState(clientProcessor, "taskRepository", taskRepository);
        doReturn(task).when(taskRepository).getTaskByIdentifier(anyString());
        doNothing().when(taskRepository).addOrUpdate(any(Task.class));

        // generic event client repository
        eventClientRepository = mock(EventClientRepository.class);
        Whitebox.setInternalState(clientProcessor, "eventClientRepository", eventClientRepository);
        doNothing().when(eventClientRepository).markEventAsTaskUnprocessed(anyString());

        // generic structure repository
        Location structure = mock(Location.class);
        doReturn(new LocationProperty()).when(structure).getProperties();
        Whitebox.setInternalState(clientProcessor, "structureRepository", structureRepository);
        doReturn(structure).when(structureRepository).getLocationById(AdditionalMatchers.or(anyString(), isNull()));
        doNothing().when(structureRepository).addOrUpdate(any(Location.class));
    }
}
