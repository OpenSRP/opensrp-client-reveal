package org.smartregister.reveal.util;

import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Client;
import org.smartregister.domain.db.EventClient;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.FamilyConstants.EventType;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.BEHAVIOUR_CHANGE_COMMUNICATION;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CASE_CONFIRMATION_FIELD;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.EVENT_TASK_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.EVENT_TYPE_FIELD;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FORM_SUBMISSION_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_ID;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.util.JsonFormUtils.gson;

public class InteractorUtils {

    private TaskRepository taskRepository;

    private EventClientRepository eventClientRepository;

    private RevealJsonFormUtils jsonFormUtils;

    public InteractorUtils(TaskRepository taskRepository, EventClientRepository eventClientRepository) {
        this.taskRepository = taskRepository;
        this.eventClientRepository = eventClientRepository;
        jsonFormUtils = new RevealJsonFormUtils();
    }


    public InteractorUtils() {
    }

    public CommonPersonObject fetchSprayDetails(String interventionType, String structureId, EventClientRepository eventClientRepository, CommonRepository commonRepository) {
        CommonPersonObject commonPersonObject = null;
        if (IRS.equals(interventionType)) {
            Cursor cursor = null;
            try {
                cursor = eventClientRepository.getWritableDatabase().rawQuery(
                        String.format("select s.*, id as _id from %s s where %s = ?", SPRAYED_STRUCTURES, ID), new String[]{structureId});
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


    public EventClient archiveClient(String baseEntityId, boolean isFamily) {
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

        try {
            eventClientRepository.batchInsertEvents(events, 0);
            clientJsonObject.put("dateVoided", now);
            clientJsonObject.put(EventClientRepository.client_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
            clientJsonObject.getJSONObject("attributes").put("dateRemoved", now);
            eventClientRepository.addorUpdateClient(baseEntityId, clientJsonObject);
            RevealApplication.getInstance().setSynced(false);
            Event archiveEvent = FamilyJsonFormUtils.createFamilyEvent(baseEntityId, Utils.getCurrentLocationId(),
                    null, isFamily ? EventType.ARCHIVE_FAMILY : EventType.ARCHIVE_FAMILY_MEMBER);
            archiveEvent.addObs(new Obs().withValue(now).withFieldCode("dateArchived").withFieldType("formsubmissionField"));
            archiveEvent.addDetails(Constants.Properties.PLAN_IDENTIFIER, PreferencesUtil.getInstance().getCurrentPlanId());

            JSONObject eventJson = new JSONObject(gson.toJson(archiveEvent));
            eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
            eventClientRepository.addEvent(baseEntityId, eventJson);

            return new EventClient(
                    gson.fromJson(eventJson.toString(), org.smartregister.domain.Event.class),
                    gson.fromJson(clientJsonObject.toString(), Client.class));

        } catch (JSONException e) {
            Timber.e(e);
        }
        return null;
    }

    public boolean archiveEventsForTask(SQLiteDatabase db, BaseTaskDetails taskDetails) {
        boolean archived = true;

        if (taskDetails == null) {
            return false;
        }
        List<String> formSubmissionIds = getFormSubmissionIdsFromEventTask(db, taskDetails);
        List<EventClient> eventClients = eventClientRepository.fetchEventClients(formSubmissionIds);
        DateTime now = new DateTime();
        JSONArray taskEvents = new JSONArray();

        try {

            JSONArray eventClientJsonArray = new JSONArray(gson.toJson(eventClients));

            for (int i = 0; i < eventClientJsonArray.length(); i++) {
                JSONObject eventJson = eventClientJsonArray.getJSONObject(i).getJSONObject("event");
                eventJson.put("dateVoided", now);
                eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);
                taskEvents.put(eventJson);
            }
            eventClientRepository.batchInsertEvents(taskEvents, 0);


            Event resetTaskEvent = RevealJsonFormUtils.createTaskEvent(taskDetails.getTaskEntity(), Utils.getCurrentLocationId(),
                    null, Constants.TASK_RESET_EVENT, Constants.STRUCTURE);
            JSONObject eventJson = new JSONObject(gson.toJson(resetTaskEvent));
            eventJson.put(EventClientRepository.event_column.syncStatus.name(), BaseRepository.TYPE_Unsynced);

            eventJson = jsonFormUtils.populateFormDetails(eventJson.toString(), taskDetails.getTaskEntity(), taskDetails.getStructureId(), taskDetails.getTaskId(), taskDetails.getBusinessStatus(), taskDetails.getTaskStatus(), null, null);
            eventClientRepository.addEvent(taskDetails.getTaskEntity(), eventJson);


        } catch (Exception e) {
            Timber.e(e);
            archived = false;
        }

        return archived;
    }

    public List<String> getFormSubmissionIdsFromEventTask(SQLiteDatabase db, BaseTaskDetails taskDetails) {
        List<String> formSubmissionIds = new ArrayList<>();
        Cursor cursor = null;
        String query;

        try {

            if (CASE_CONFIRMATION.equals(taskDetails.getTaskCode()) || BCC.equals(taskDetails.getTaskCode())) {
                String eventTypeField = null;

                if (CASE_CONFIRMATION.equals(taskDetails.getTaskCode())) {
                    eventTypeField = CASE_CONFIRMATION_FIELD;
                } else if (BCC.equals(taskDetails.getTaskCode())) {
                    eventTypeField = BEHAVIOUR_CHANGE_COMMUNICATION;
                }

                query = String.format("select %s from event where baseEntityId = ?  and %s = ?",
                        FORM_SUBMISSION_ID, EVENT_TYPE_FIELD);
                cursor = db.rawQuery(query, new String[]{taskDetails.getTaskEntity(), eventTypeField});

                while (cursor.moveToNext()) {
                    formSubmissionIds.add(cursor.getString(cursor.getColumnIndex(FORM_SUBMISSION_ID)));
                }

            } else {
                query = String.format("select %s from %s where %s = ?",
                        BASE_ENTITY_ID, EVENT_TASK_TABLE, TASK_ID);
                cursor = db.rawQuery(query, new String[]{taskDetails.getTaskId()});

                while (cursor.moveToNext()) {
                    formSubmissionIds.add(cursor.getString(cursor.getColumnIndex(BASE_ENTITY_ID)));
                }
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return formSubmissionIds;
    }

    public boolean resetTaskInfo(SQLiteDatabase db, BaseTaskDetails taskDetails) {
        return archiveEventsForTask(db, taskDetails) && TaskUtils.getInstance().resetTask(taskDetails);
    }

}
