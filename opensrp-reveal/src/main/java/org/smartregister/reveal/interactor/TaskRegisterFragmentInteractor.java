package org.smartregister.reveal.interactor;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;

import androidx.core.util.Pair;

import com.google.common.annotations.VisibleForTesting;

import net.sqlcipher.Cursor;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.Event;
import org.smartregister.repository.EventClientRepository.event_column;
import org.smartregister.repository.LocationRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.TaskRegisterFragmentContract;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.EventType;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static org.smartregister.domain.Task.INACTIVE_TASK_STATUS;
import static org.smartregister.domain.Task.TaskStatus.COMPLETED;
import static org.smartregister.family.util.DBConstants.KEY.DATE_REMOVED;
import static org.smartregister.family.util.DBConstants.KEY.FIRST_NAME;
import static org.smartregister.family.util.DBConstants.KEY.RELATIONAL_ID;
import static org.smartregister.repository.EventClientRepository.Table.event;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.COMPLETED_TASK_COUNT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FAMILY_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.GROUPED_STRUCTURE_TASK_CODE_AND_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.GROUPED_TASKS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.GROUPID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LAST_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LATITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LONGITUDE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_OTHER_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NOT_SRAYED_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.OTHER;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PLAN_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.REFERENCE_REASON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAY_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_COUNT;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_TABLE;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Properties.FAMILY_MEMBER_NAMES;
import static org.smartregister.reveal.util.FamilyConstants.DatabaseKeys.HOUSE_NUMBER;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;

/**
 * Created by samuelgithengi on 3/18/19.
 */
public class TaskRegisterFragmentInteractor extends BaseInteractor implements TaskRegisterFragmentContract.Interactor {

    private final LocationRepository locationRepository;
    private final Float locationBuffer;
    private InteractorUtils interactorUtils;


    private int structuresWithinBuffer = 0;

    public TaskRegisterFragmentInteractor(TaskRegisterFragmentContract.Presenter presenter) {
        this(presenter, Utils.getLocationBuffer());
    }

    @VisibleForTesting
    public TaskRegisterFragmentInteractor(TaskRegisterFragmentContract.Presenter presenter,
                                          Float locationBuffer) {
        super(presenter);
        this.locationBuffer = locationBuffer;
        locationRepository = RevealApplication.getInstance().getLocationRepository();
        interactorUtils = new InteractorUtils(RevealApplication.getInstance().getTaskRepository(), eventClientRepository);
    }

    private String mainSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID_);
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, FOR, STRUCTURES_TABLE, ID_));
        if (BuildConfig.BUILD_COUNTRY != Country.NAMIBIA) {
            queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                    SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, ID));
        } else {
            String planIdentifier = PreferencesUtil.getInstance().getCurrentPlanId();
            queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s AND %s.%s = '%s'",
                    SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, ID, SPRAYED_STRUCTURES, PLAN_ID, planIdentifier));
        }
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                FAMILY, STRUCTURES_TABLE, ID_, FAMILY, STRUCTURE_ID));
        return queryBuilder.mainCondition(mainCondition);
    }

    private String nonRegisteredStructureTasksSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(tableName, mainColumns(tableName), ID_);
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, FOR, STRUCTURES_TABLE, ID_));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, ID));
        queryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                FAMILY, STRUCTURES_TABLE, ID_, FAMILY, STRUCTURE_ID));
        queryBuilder.mainCondition(mainCondition);
        queryBuilder.addCondition(String.format(" AND %s.%s IS NULL",
                FAMILY, STRUCTURE_ID));
        queryBuilder.addCondition(String.format(" AND %s.%s != '%s'",
                tableName, CODE, BEDNET_DISTRIBUTION));
        return String.format(" SELECT %s.* , SUM(CASE WHEN status='%s' THEN 1 ELSE 0 END ) AS %s , COUNT(_id ) AS %s, " +
                        "GROUP_CONCAT(%s || \"-\" || %s ) AS %s , 'non_registered' as %s  FROM ( ",
                GROUPED_TASKS, COMPLETED.toString(), COMPLETED_TASK_COUNT, TASK_COUNT, CODE, BUSINESS_STATUS, GROUPED_STRUCTURE_TASK_CODE_AND_STATUS, FAMILY_MEMBER_NAMES) + queryBuilder +
                String.format(" ) AS %s GROUP BY %s ", GROUPED_TASKS, STRUCTURE_ID);
    }

    private String groupedRegisteredStructureTasksSelect(String mainCondition) {
        String tableName = TASK_TABLE;
        SmartRegisterQueryBuilder structureTasksQueryBuilder = new SmartRegisterQueryBuilder();
        String[] columns = ArrayUtils.add(mainColumns(tableName), String.format("%s.%s||' '||%s.%s as %s ", FAMILY_MEMBER, FIRST_NAME, FAMILY_MEMBER, LAST_NAME, FAMILY_MEMBER_NAMES));
        structureTasksQueryBuilder.selectInitiateMainTable(tableName, columns, ID_);
        structureTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                STRUCTURES_TABLE, tableName, STRUCTURE_ID, STRUCTURES_TABLE, ID_));
        structureTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s AND %s.%s IS NULL COLLATE NOCASE",
                FAMILY, STRUCTURES_TABLE, ID_, FAMILY, STRUCTURE_ID, FAMILY, DATE_REMOVED));
        structureTasksQueryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s AND %s.%s IS NULL  COLLATE NOCASE",
                FAMILY_MEMBER, FAMILY, BASE_ENTITY_ID, FAMILY_MEMBER, RELATIONAL_ID, FAMILY_MEMBER, DATE_REMOVED));
        structureTasksQueryBuilder.customJoin(String.format(" LEFT JOIN %s ON %s.%s = %s.%s ",
                SPRAYED_STRUCTURES, tableName, FOR, SPRAYED_STRUCTURES, ID));
        structureTasksQueryBuilder.mainCondition(mainCondition);

        return String.format(" SELECT %s.* , SUM(CASE WHEN status='%s' THEN 1 ELSE 0 END ) AS %s , COUNT(_id ) AS %s, " +
                        "GROUP_CONCAT(%s || \"-\" || %s ) AS %s , GROUP_CONCAT(%s) as %s  FROM ( ",
                GROUPED_TASKS, COMPLETED.toString(), COMPLETED_TASK_COUNT, TASK_COUNT, CODE, BUSINESS_STATUS, GROUPED_STRUCTURE_TASK_CODE_AND_STATUS, FAMILY_MEMBER_NAMES, FAMILY_MEMBER_NAMES) + structureTasksQueryBuilder +
                String.format(" ) AS %s GROUP BY %s ", GROUPED_TASKS, STRUCTURE_ID);

    }

    private String bccSelect() {
        return String.format("SELECT * FROM %s WHERE %s = ? AND %s = ? AND %s ='%s' AND %s NOT IN (%s)",
                TASK_TABLE, FOR, PLAN_ID, CODE, BCC, STATUS,
                TextUtils.join(",", Collections.nCopies(INACTIVE_TASK_STATUS.length, "?")));
    }

    private String indexCaseSelect() {
        return String.format("SELECT * FROM %s WHERE %s = ? AND %s = ? AND %s NOT IN (%s) AND %s = ? ",
                TASK_TABLE, GROUPID, PLAN_ID, STATUS,
                TextUtils.join(",", Collections.nCopies(INACTIVE_TASK_STATUS.length, "?")),
                CODE);
    }


    private String[] mainColumns(String tableName) {
        return new String[]{
                tableName + "." + ID_,
                tableName + "." + CODE,
                tableName + "." + FOR,
                tableName + "." + BUSINESS_STATUS,
                tableName + "." + STATUS,
                tableName + "." + REFERENCE_REASON,
                STRUCTURES_TABLE + "." + LATITUDE,
                STRUCTURES_TABLE + "." + LONGITUDE,
                STRUCTURES_TABLE + "." + NAME,
                SPRAYED_STRUCTURES + "." + STRUCTURE_NAME,
                SPRAYED_STRUCTURES + "." + FAMILY_NAME,
                SPRAYED_STRUCTURES + "." + SPRAY_STATUS,
                SPRAYED_STRUCTURES + "." + NOT_SRAYED_REASON,
                SPRAYED_STRUCTURES + "." + NOT_SRAYED_OTHER_REASON,
                STRUCTURES_TABLE + "." + ID_ + " AS " + STRUCTURE_ID,
                FAMILY + "." + FIRST_NAME,
                FAMILY + "." + HOUSE_NUMBER

        };
    }


    public void findTasks(Pair<String, String[]> mainCondition, Location lastLocation, Location operationalAreaCenter, String houseLabel) {
        if (mainCondition == null || mainCondition.second == null || mainCondition.second.length < 3 || mainCondition.second[0] == null) {
            getPresenter().onTasksFound(null, 0);
            return;
        }
        // Fetch grouped tasks
        List<TaskDetails> tasks = new ArrayList<>();
        appExecutors.diskIO().execute(() -> {
            structuresWithinBuffer = 0;
            if (Utils.isFocusInvestigationOrMDA()) { // perform task grouping

                tasks.addAll(queryTaskDetails(groupedRegisteredStructureTasksSelect(mainCondition.first),
                        mainCondition.second, lastLocation, operationalAreaCenter, houseLabel, true));


                tasks.addAll(queryTaskDetails(nonRegisteredStructureTasksSelect(mainCondition.first),
                        mainCondition.second, lastLocation, operationalAreaCenter, houseLabel, true));

            } else {

                tasks.addAll(queryTaskDetails(mainSelect(mainCondition.first), mainCondition.second,
                        lastLocation, operationalAreaCenter, houseLabel, false));

            }

            // Query BCC task
            tasks.addAll(queryTaskDetails(bccSelect(), mainCondition.second, lastLocation,
                    operationalAreaCenter, houseLabel, false));


            // Query Case Confirmation task
            String[] params = ArrayUtils.add(mainCondition.second, CASE_CONFIRMATION);
            tasks.addAll(queryTaskDetails(indexCaseSelect(), params, lastLocation,
                    operationalAreaCenter, houseLabel, false));

            Collections.sort(tasks);
            appExecutors.mainThread().execute(() -> {
                getPresenter().onTasksFound(tasks, structuresWithinBuffer);
            });

        });

    }

    private List<TaskDetails> queryTaskDetails(String query, String[] params, Location lastLocation,
                                               Location operationalAreaCenter, String houseLabel, boolean groupedTasks) {
        List<TaskDetails> tasks = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getDatabase().rawQuery(query, params);
            while (cursor != null && cursor.moveToNext()) {
                TaskDetails taskDetails = readTaskDetails(cursor, lastLocation, operationalAreaCenter, houseLabel, groupedTasks);
                //skip BCC and Case confirmation tasks in tracking tasks within buffer
                if (taskDetails.getDistanceFromUser() <= locationBuffer && taskDetails.getDistanceFromUser() >= 0) {
                    structuresWithinBuffer += 1;
                }
                tasks.add(taskDetails);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tasks;
    }


    private TaskDetails readTaskDetails(Cursor cursor, Location lastLocation, Location operationalAreaCenter, String houseLabel, boolean isGroupedTasks) {
        TaskDetails task = new TaskDetails(cursor.getString(cursor.getColumnIndex(ID_)));
        task.setTaskCode(cursor.getString(cursor.getColumnIndex(CODE)));
        task.setTaskEntity(cursor.getString(cursor.getColumnIndex(FOR)));
        task.setBusinessStatus(cursor.getString(cursor.getColumnIndex(BUSINESS_STATUS)));
        task.setTaskStatus(cursor.getString(cursor.getColumnIndex(STATUS)));
        if (isGroupedTasks) {
            task.setTaskCount(cursor.getInt(cursor.getColumnIndex(TASK_COUNT)));
            task.setCompleteTaskCount(cursor.getInt(cursor.getColumnIndex(COMPLETED_TASK_COUNT)));
            task.setGroupedTaskCodeStatus(cursor.getString(cursor.getColumnIndex(GROUPED_STRUCTURE_TASK_CODE_AND_STATUS)));
            task.setHouseNumber(cursor.getString(cursor.getColumnIndex(HOUSE_NUMBER)));
            task.setFamilyMemberNames(cursor.getString(cursor.getColumnIndex(FAMILY_MEMBER_NAMES)));
            task.setGroupedTaskCodes(cursor.getString(cursor.getColumnIndex(GROUPED_STRUCTURE_TASK_CODE_AND_STATUS)));
        }
        Location location = new Location((String) null);

        if (CASE_CONFIRMATION.equals(task.getTaskCode())) {
            task.setReasonReference(cursor.getString(cursor.getColumnIndex(REFERENCE_REASON)));
        } else if (!BCC.equals(task.getTaskCode())) {
            location.setLatitude(cursor.getDouble(cursor.getColumnIndex(LATITUDE)));
            location.setLongitude(cursor.getDouble(cursor.getColumnIndex(LONGITUDE)));
            task.setLocation(location);
            task.setStructureName(cursor.getString(cursor.getColumnIndex(NAME)));
            if (StringUtils.isBlank(task.getStructureName())) {
                task.setStructureName(cursor.getString(cursor.getColumnIndex(STRUCTURE_NAME)));
            }

            task.setFamilyName(cursor.getString(cursor.getColumnIndex(FIRST_NAME)));
            if (task.getFamilyName() == null) {
                task.setFamilyName(cursor.getString(cursor.getColumnIndex(FAMILY_NAME)));
            }

            if (task.getFamilyName() != null)
                task.setFamilyName(task.getFamilyName() + " " + houseLabel);

            task.setSprayStatus(cursor.getString(cursor.getColumnIndex(SPRAY_STATUS)));

            if (Constants.BusinessStatus.NOT_SPRAYED.equals(task.getBusinessStatus())) {
                String reason = cursor.getString(cursor.getColumnIndex(NOT_SRAYED_REASON));
                if (OTHER.equals(reason)) {
                    reason = cursor.getString(cursor.getColumnIndex(NOT_SRAYED_OTHER_REASON));
                }
                task.setTaskDetails(reason);
            }
        }
        task.setStructureId(cursor.getString(cursor.getColumnIndex(STRUCTURE_ID)));

        calculateDistance(task, location, lastLocation, operationalAreaCenter);

        return task;
    }

    private void calculateDistance(TaskDetails task, Location location, Location lastLocation, Location operationalAreaCenter) {
        if (BCC.equals(task.getTaskCode())) {
            //set distance to -2 to always display on top of register
            task.setDistanceFromUser(-2);
        } else if (CASE_CONFIRMATION.equals(task.getTaskCode()) && task.getTaskCount() == null) {
            //set distance to -1 to always display on top of register and below BCC
            task.setDistanceFromUser(-1);
        } else if (lastLocation != null) {
            task.setDistanceFromUser(location.distanceTo(lastLocation));
        } else {
            task.setDistanceFromUser(location.distanceTo(operationalAreaCenter));
            task.setDistanceFromCenter(true);
        }
    }


    public void calculateDistanceFromUser(List<TaskDetails> tasks, Location location) {
        if (tasks == null)
            return;
        appExecutors.diskIO().execute(() -> {
            int structuresWithinBuffer = 0;
            for (TaskDetails taskDetails : tasks) {
                if (!BCC.equals(taskDetails.getTaskCode()) && !CASE_CONFIRMATION.equals(taskDetails.getTaskCode())) {
                    taskDetails.setDistanceFromUser(taskDetails.getLocation().distanceTo(location));
                    taskDetails.setDistanceFromCenter(false);
                }
                if (taskDetails.getDistanceFromUser() <= locationBuffer) {
                    structuresWithinBuffer += 1;
                }
            }
            Collections.sort(tasks);
            int finalStructuresWithinBuffer = structuresWithinBuffer;
            appExecutors.mainThread().execute(() -> {
                getPresenter().onTasksFound(tasks, finalStructuresWithinBuffer);
            });
        });

    }


    public void getStructure(TaskDetails taskDetails) {
        appExecutors.diskIO().execute(() -> {
            org.smartregister.domain.Location structure;
            if (BCC.equals(taskDetails.getTaskCode()))
                structure = locationRepository.getLocationById(taskDetails.getTaskEntity());
            else
                structure = structureRepository.getLocationById(taskDetails.getStructureId());
            appExecutors.mainThread().execute(() -> {
                getPresenter().onStructureFound(structure, taskDetails);
            });
        });
    }

    private TaskRegisterFragmentContract.Presenter getPresenter() {
        return (TaskRegisterFragmentContract.Presenter) presenterCallBack;
    }

    public void getIndexCaseDetails(String structureId, String operationalArea, String indexCaseEventId) {
        appExecutors.diskIO().execute(() -> {
            JSONObject jsonEvent = null;
            if (StringUtils.isNotBlank(structureId) || StringUtils.isNotBlank(operationalArea)) {

                Cursor cursor = null;
                try {
                    String[] params;
                    if (structureId == null) {
                        params = new String[]{operationalArea, EventType.CASE_DETAILS_EVENT};
                    } else {
                        params = new String[]{structureId, operationalArea, EventType.CASE_DETAILS_EVENT};
                    }
                    String query = String.format("SELECT %s FROM %s WHERE %s IN (%s) AND %s = ?", event_column.json.name(), event.name(), event_column.baseEntityId.name(),
                            structureId == null ? "?" : "?,?", event_column.eventType.name());
                    cursor = getDatabase().rawQuery(query, params);
                    while (cursor.moveToNext()) {
                        String jsonEventStr = cursor.getString(0);

                        jsonEventStr = jsonEventStr.replaceAll("'", "");
                        JSONObject localJsonEvent = new JSONObject(jsonEventStr);

                        if (cursor.getCount() == 1 || localJsonEvent.optString(ID_).equals(indexCaseEventId)) {
                            jsonEvent = new JSONObject(jsonEventStr);
                            break;
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

            }
            JSONObject finalJsonEvent = jsonEvent;
            appExecutors.mainThread().execute(() -> {
                getPresenter().onIndexCaseFound(finalJsonEvent, finalJsonEvent != null
                        && operationalArea.equals(finalJsonEvent.optString(Properties.BASE_ENTITY_ID)));
            });
        });

    }

    @Override
    public void resetTaskInfo(Context context, TaskDetails taskDetails) {
        appExecutors.diskIO().execute(() -> {
            interactorUtils.resetTaskInfo(getDatabase(), taskDetails);
        });

        appExecutors.mainThread().execute(() -> {
            getPresenter().onTaskInfoReset();
        });
    }

    @Override
    public void handleLasteventFound(Event event) {
        getPresenter().onEventFound(event);
    }
}
