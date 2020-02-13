package org.smartregister.reveal.util;

import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.Client;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.FamilyConstants.EventType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.DETAILS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.EVENT_TASK_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_ID;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Properties.TASK_BUSINESS_STATUS;
import static org.smartregister.util.JsonFormUtils.gson;

public class InteractorUtils {

    private TaskRepository taskRepository;

    private EventClientRepository eventClientRepository;

    private RevealClientProcessor clientProcessor;

    public InteractorUtils(TaskRepository taskRepository, EventClientRepository eventClientRepository, RevealClientProcessor clientProcessor) {
        this.taskRepository = taskRepository;
        this.eventClientRepository = eventClientRepository;
        this.clientProcessor = clientProcessor;
    }


    public InteractorUtils() {
    }

    public CommonPersonObject fetchSprayDetails(String interventionType, String structureId, EventClientRepository eventClientRepository, CommonRepository commonRepository) {
        CommonPersonObject commonPersonObject = null;
        if (IRS.equals(interventionType)) {
            Cursor cursor = null;
            try {
                cursor = eventClientRepository.getWritableDatabase().rawQuery(
                        String.format("select s.*, id as _id from %s s where %s = ?", SPRAYED_STRUCTURES, Constants.DatabaseKeys.BASE_ENTITY_ID), new String[]{structureId});
                if (cursor.moveToFirst()) {
                    commonPersonObject = commonRepository.getCommonPersonObjectFromCursor(cursor);
                }
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return commonPersonObject;
    }


    public boolean archiveClient(String baseEntityId, boolean isFamily) {
        taskRepository.cancelTasksForEntity(baseEntityId);
        taskRepository.archiveTasksForEntity(baseEntityId);
        JSONObject eventsByBaseEntityId = eventClientRepository.getEventsByBaseEntityId(baseEntityId);
        JSONArray events = eventsByBaseEntityId.optJSONArray("events");
        JSONObject clientJsonObject = eventsByBaseEntityId.optJSONObject("client");
        DateTime now = new DateTime();
        if (events != null) {
            for (int i = 0; i < events.length(); i++) {
                try {
                    JSONObject event = events.getJSONObject(i);
                    event.put("dateVoided", now);
                    event.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }
        }

        boolean saved;
        try {
            eventClientRepository.batchInsertEvents(events, 0);
            clientJsonObject.put("dateVoided", now);
            clientJsonObject.put(EventClientRepository.client_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
            clientJsonObject.getJSONObject("attributes").put("dateRemoved", now);
            eventClientRepository.addorUpdateClient(baseEntityId, clientJsonObject);

            Event archiveEvent = FamilyJsonFormUtils.createFamilyEvent(baseEntityId, Utils.getCurrentLocationId(),
                    null, isFamily ? EventType.ARCHIVE_FAMILY : EventType.ARCHIVE_FAMILY_MEMBER);

            JSONObject eventJson = new JSONObject(gson.toJson(archiveEvent));
            eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
            eventClientRepository.addEvent(baseEntityId, eventJson);

            clientProcessor.processClient(Collections.singletonList(new EventClient(
                    gson.fromJson(eventJson.toString(), org.smartregister.domain.db.Event.class),
                    gson.fromJson(clientJsonObject.toString(), Client.class))), true);
            saved = true;

        } catch (JSONException e) {
            Timber.e(e);
            saved = false;
        }
        return saved;
    }

    public boolean archiveEventsForTask(SQLiteDatabase db, BaseTaskDetails taskDetails) {
        boolean archived = true;
        Task task = taskRepository.getTaskByIdentifier(taskDetails.getTaskId());

        if (task == null) {
            return false;
        }
        List<String> formSubmissionIds = getFormSubmissionIdsFromEventTask(db, task.getIdentifier());
        List<EventClient> eventClients = eventClientRepository.fetchEventClients(formSubmissionIds);
        DateTime now = new DateTime();
        JSONArray taskEvents = new JSONArray();

        try {

            if (eventClients != null && !eventClients.isEmpty()) {
                for (EventClient eventClient: eventClients) {
                    org.smartregister.domain.db.Event event = eventClient.getEvent();
                    JSONObject eventJson = new JSONObject(gson.toJson(event));
                    eventJson.put("dateVoided", now);
                    eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
                    taskEvents.put(eventJson);
                }

                eventClientRepository.batchInsertEvents(taskEvents, 0);
            }

            // use original task.for in cases such as Case Confirmation where the task.for changes to point to operational area
            String taskForEntity = StringUtils.isNotEmpty(taskDetails.getTaskEntity()) ? taskDetails.getTaskEntity() : task.getForEntity();

            Event resetTaskEvent = RevealJsonFormUtils.createTaskEvent(taskForEntity, Utils.getCurrentLocationId(),
                    null, Constants.TASK_RESET_EVENT, Constants.STRUCTURE);
            JSONObject eventJson = new JSONObject(gson.toJson(resetTaskEvent));
            eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);

            populateEventDetails(eventJson, task.getIdentifier(), taskDetails.getBusinessStatus(), taskDetails.getTaskStatus(), taskDetails.getStructureId());
            eventClientRepository.addEvent(task.getForEntity(), eventJson);


        } catch (Exception e) {
            Timber.e(e);
            archived = false;
        }

        return archived;
    }

    public List<String> getFormSubmissionIdsFromEventTask(SQLiteDatabase db, String taskIdentifier) {
        List<String> formSubmissionIds = new ArrayList<>();
        Cursor cursor = null;

        try {
            String query = String.format("select %s from %s where %s = ?",
                    BASE_ENTITY_ID, EVENT_TASK_TABLE, TASK_ID);
            cursor = db.rawQuery(query, new String[]{taskIdentifier});

            while (cursor.moveToNext()) {
                formSubmissionIds.add(cursor.getString(cursor.getColumnIndex(BASE_ENTITY_ID)));
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return formSubmissionIds;
    }

    private void populateEventDetails(JSONObject eventJson, String taskIdentifier,
                                      String taskBusinessStatus, String taskStatus, String structureId){
        try {
            JSONObject formData = new JSONObject();
            formData.put(Constants.Properties.TASK_IDENTIFIER, taskIdentifier);
            formData.put(TASK_BUSINESS_STATUS, taskBusinessStatus);
            formData.put(Constants.Properties.TASK_STATUS, taskStatus);
            formData.put(Constants.Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            formData.put(Constants.Properties.LOCATION_ID, structureId);
            eventJson.put(DETAILS, formData);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

}
