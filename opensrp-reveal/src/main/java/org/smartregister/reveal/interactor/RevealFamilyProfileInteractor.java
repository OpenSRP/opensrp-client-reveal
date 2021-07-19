package org.smartregister.reveal.interactor;

import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.util.DBConstants.KEY;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.FamilyProfileContract;
import org.smartregister.reveal.receiver.TaskGenerationReceiver;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.FamilyJsonFormUtils;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.smartregister.AllConstants.INTENT_KEY.TASK_GENERATED_EVENT;
import static org.smartregister.family.util.DBConstants.KEY.BASE_ENTITY_ID;
import static org.smartregister.family.util.DBConstants.KEY.DATE_REMOVED;
import static org.smartregister.repository.EventClientRepository.client_column.syncStatus;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_NAME;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;


/**
 * Created by samuelgithengi on 4/15/19.
 */
public class RevealFamilyProfileInteractor extends FamilyProfileInteractor implements FamilyProfileContract.Interactor {


    private AppExecutors appExecutors;
    private FamilyProfileContract.Presenter presenter;
    private EventClientRepository eventClientRepository;
    private RevealClientProcessor clientProcessor;
    private CommonRepository commonRepository;
    private InteractorUtils interactorUtils;
    private TaskRepository taskRepository;

    public RevealFamilyProfileInteractor(FamilyProfileContract.Presenter presenter) {
        this.presenter = presenter;
        appExecutors = RevealApplication.getInstance().getAppExecutors();
        eventClientRepository = CoreLibrary.getInstance().context().getEventClientRepository();
        FamilyMetadata familyMetadata = RevealApplication.getInstance().getMetadata();
        clientProcessor = (RevealClientProcessor) RevealApplication.getInstance().getClientProcessor();
        commonRepository = RevealApplication.getInstance().getContext().commonrepository(familyMetadata.familyMemberRegister.tableName);
        interactorUtils = new InteractorUtils(RevealApplication.getInstance().getTaskRepository(), eventClientRepository);
        taskRepository = RevealApplication.getInstance().getTaskRepository();
    }

    @Override
    public ClientProcessorForJava getClientProcessorForJava() {
        return RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext());
    }

    @Override
    public void updateFamilyMemberName(@NonNull Client family, Event event, @NonNull String oldFamilyName) {
        appExecutors.diskIO().execute(() -> {
            JSONArray familyMembers = new JSONArray();
            JSONArray updateSurnameEvents = new JSONArray();
            List<String> formSubmissionIds = new ArrayList<>();
            for (CommonPersonObject commonPersonObject : commonRepository.findByRelational_IDs(family.getBaseEntityId())) {
                String lastName = commonPersonObject.getColumnmaps().get(KEY.LAST_NAME);
                if (oldFamilyName.equalsIgnoreCase(lastName)) {//surname same as the edited family name
                    JSONObject client = eventClientRepository.getClientByBaseEntityId(commonPersonObject.getCaseId());
                    try {
                        client.put("lastName", family.getFirstName());
                        client.put(syncStatus.name(), BaseRepository.TYPE_Unsynced);
                        familyMembers.put(client);
                        FamilyMetadata familyMetadata = RevealApplication.getInstance().getMetadata();
                        Event updateEvent = FamilyJsonFormUtils.createFamilyEvent(commonPersonObject.getCaseId(),
                                event.getLocationId(), event.getDetails(), familyMetadata.familyMemberRegister.updateEventType);
                        JSONObject eventJson = new JSONObject(JsonFormUtils.gson.toJson(updateEvent));
                        eventJson.put(syncStatus.name(), BaseRepository.TYPE_Unsynced);
                        updateSurnameEvents.put(eventJson);
                        formSubmissionIds.add(updateEvent.getFormSubmissionId());

                    } catch (Exception e) {
                        Timber.e(e, "Error updating Family Surname");
                    }
                }
            }

            //update the client documents
            eventClientRepository.batchInsertClients(familyMembers);
            //generate the events for updating the members surname
            eventClientRepository.batchInsertEvents(updateSurnameEvents, 0);

            //Client Process
            clientProcessor.processClient(eventClientRepository.fetchEventClients(formSubmissionIds), true);

            appExecutors.mainThread().execute(() -> {
                presenter.onMembersUpdated();
            });
        });
    }

    @Override
    public void archiveFamily(String familyBaseEntityId, String structureId) {
        appExecutors.diskIO().execute(() -> {
            SQLiteDatabase db = eventClientRepository.getWritableDatabase();
            try {
                db.beginTransaction();
                IntentFilter filter = new IntentFilter(TASK_GENERATED_EVENT);
                TaskGenerationReceiver taskGenerationReceiver = new TaskGenerationReceiver(task -> {
                    appExecutors.mainThread().execute(() -> presenter.onArchiveFamilyCompleted(task != null, task));
                });
                LocalBroadcastManager.getInstance(RevealApplication.getInstance().getApplicationContext()).registerReceiver(taskGenerationReceiver, filter);
                List<String> familyMembers = commonRepository.findSearchIds(String.format(
                        "SELECT %s FROM %s where %s='%s' AND %s IS NULL",
                        BASE_ENTITY_ID, FAMILY_MEMBER, KEY.RELATIONAL_ID, familyBaseEntityId, DATE_REMOVED));
                List<EventClient> eventClients = new ArrayList<>();
                eventClients.add(interactorUtils.archiveClient(familyBaseEntityId, true));
                for (String baseEntityId : familyMembers) {
                    eventClients.add(interactorUtils.archiveClient(baseEntityId, false));
                }
                taskRepository.cancelTasksForEntity(structureId);
                taskRepository.archiveTasksForEntity(structureId);
                db.execSQL(String.format("update %s set name = null where %s = ? ", STRUCTURES_TABLE, ID_), new String[]{structureId});
                db.execSQL(String.format("update %s set %s = null where %s = ? ", SPRAYED_STRUCTURES, STRUCTURE_NAME, ID), new String[]{structureId});

                clientProcessor.processClient(eventClients, true);
                db.setTransactionSuccessful();
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                db.endTransaction();
            }
        });
    }

    @Override
    protected void processClient(List<EventClient> eventClientList) {
        IntentFilter filter = new IntentFilter(TASK_GENERATED_EVENT);
        TaskGenerationReceiver taskGenerationReceiver = new TaskGenerationReceiver(task -> {
            appExecutors.mainThread().execute(() -> presenter.onTasksGenerated());
        });
        LocalBroadcastManager.getInstance(RevealApplication.getInstance().getApplicationContext()).registerReceiver(taskGenerationReceiver, filter);
        clientProcessor.processClient(eventClientList, true);
    }
}
