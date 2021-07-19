package org.smartregister.reveal.util;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.joda.time.DateTime;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Task;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.Constants.Intervention;

import java.util.List;

import timber.log.Timber;

import static org.smartregister.domain.Task.TaskStatus.READY;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_TABLE;

/**
 * Created by samuelgithengi on 4/14/19.
 */
public class TaskUtils {

    private final TaskRepository taskRepository;

    private final PreferencesUtil prefsUtil;

    private static TaskUtils instance;

    private final RevealApplication revealApplication;

    public static TaskUtils getInstance() {
        if (instance == null) {
            instance = new TaskUtils();
        }
        return instance;
    }

    private TaskUtils() {
        taskRepository = RevealApplication.getInstance().getTaskRepository();
        prefsUtil = PreferencesUtil.getInstance();
        revealApplication = RevealApplication.getInstance();
    }

    public void tagEventTaskDetails(List<Event> events, SQLiteDatabase sqLiteDatabase) {
        for (Event event : events) {
            try (Cursor cursor = sqLiteDatabase.rawQuery(String.format("select * from %s where %s =? and %s =? and %s =? limit 1", TASK_TABLE, FOR, STATUS, CODE),
                    new String[]{event.getBaseEntityId(), Task.TaskStatus.COMPLETED.name(), Intervention.IRS})) {
                while (cursor.moveToNext()) {
                    Task task = taskRepository.readCursor(cursor);
                    event.addDetails(Constants.Properties.TASK_IDENTIFIER, task.getIdentifier());
                    event.addDetails(Constants.Properties.TASK_BUSINESS_STATUS, task.getBusinessStatus());
                    event.addDetails(Constants.Properties.TASK_STATUS, task.getStatus().name());
                    event.addDetails(Constants.Properties.LOCATION_ID, task.getForEntity());
                    event.addDetails(Constants.Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
                    event.addDetails(Constants.Properties.PLAN_IDENTIFIER, task.getPlanIdentifier());
                    event.setLocationId(task.getGroupIdentifier());
                }
            }
        }
    }

    public boolean resetTask(BaseTaskDetails taskDetails) {

        boolean taskResetSuccessful = false;
        try {
            Task task = taskRepository.getTaskByIdentifier(taskDetails.getTaskId());
            String operationalAreaId = Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea()).getId();

            if (Intervention.CASE_CONFIRMATION.equals(taskDetails.getTaskCode())) {
                task.setForEntity(operationalAreaId);
            }

            task.setBusinessStatus(BusinessStatus.NOT_VISITED);
            task.setStatus(READY);
            task.setLastModified(new DateTime());
            task.setSyncStatus(BaseRepository.TYPE_Unsynced);
            taskRepository.addOrUpdate(task);
            revealApplication.setSynced(false);

            revealApplication.setRefreshMapOnEventSaved(true);

            taskResetSuccessful = true;
        } catch (Exception e) {
            Timber.e(e);
        }

        return taskResetSuccessful;

    }

}
