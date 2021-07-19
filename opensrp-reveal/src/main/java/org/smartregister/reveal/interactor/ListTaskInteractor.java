package org.smartregister.reveal.interactor;

import com.mapbox.geojson.Feature;

import net.sqlcipher.Cursor;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.ListTaskContract;
import org.smartregister.reveal.model.CardDetails;
import org.smartregister.reveal.model.FamilyCardDetails;
import org.smartregister.reveal.model.IRSVerificationCardDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.SprayCardDetails;
import org.smartregister.reveal.model.StructureDetails;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.presenter.ListTaskPresenter;
import org.smartregister.reveal.util.CardDetailsUtil;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.GeoJSON;
import org.smartregister.reveal.util.FamilyConstants;
import org.smartregister.reveal.util.FamilyJsonFormUtils;
import org.smartregister.reveal.util.GeoJsonUtils;
import org.smartregister.reveal.util.IndicatorUtils;
import org.smartregister.reveal.util.InteractorUtils;
import org.smartregister.reveal.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static org.smartregister.domain.LocationProperty.PropertyStatus.INACTIVE;
import static org.smartregister.family.util.Constants.KEY.FAMILY_HEAD_NAME;
import static org.smartregister.family.util.DBConstants.KEY.DATE_REMOVED;
import static org.smartregister.family.util.DBConstants.KEY.RELATIONAL_ID;
import static org.smartregister.reveal.util.Constants.BusinessStatus.COMPLETE;
import static org.smartregister.reveal.util.Constants.BusinessStatus.IN_PROGRESS;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_ELIGIBLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.AUTHORED_ON;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BUSINESS_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CARD_SPRAY;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CHALK_SPRAY;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.CODE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ELIGIBLE_STRUCTURE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FIRST_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LAST_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LAST_UPDATED_DATE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.OWNER;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PAOT_COMMENTS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PAOT_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PLAN_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.REPORT_SPRAY;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STICKER_SPRAY;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TRUE_STRUCTURE;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.IRS_VERIFICATION;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;
import static org.smartregister.reveal.util.Constants.Properties.TASK_CODE;
import static org.smartregister.reveal.util.Constants.Properties.TASK_IDENTIFIER;
import static org.smartregister.reveal.util.Constants.Tables.IRS_VERIFICATION_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.LARVAL_DIPPINGS_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.MOSQUITO_COLLECTIONS_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.PAOT_TABLE;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;
import static org.smartregister.reveal.util.Utils.getInterventionLabel;
import static org.smartregister.reveal.util.Utils.getPropertyValue;

/**
 * Created by samuelgithengi on 11/27/18.
 */
public class ListTaskInteractor extends BaseInteractor {

    private CommonRepository commonRepository;
    private InteractorUtils interactorUtils;
    private StructureRepository structureRepository;
    private TaskRepository taskRepository;
    private RevealApplication revealApplication;

    public ListTaskInteractor(ListTaskContract.Presenter presenter) {
        super(presenter);
        commonRepository = RevealApplication.getInstance().getContext().commonrepository(SPRAYED_STRUCTURES);
        structureRepository = RevealApplication.getInstance().getContext().getStructureRepository();
        taskRepository = RevealApplication.getInstance().getTaskRepository();
        interactorUtils = new InteractorUtils(taskRepository, eventClientRepository);
        revealApplication = RevealApplication.getInstance();
    }

    public void fetchInterventionDetails(String interventionType, String featureId, boolean isForForm) {
        String sql = "SELECT status, start_date, end_date FROM %s WHERE id=?";
        if (IRS.equals(interventionType)) {
            sql = "SELECT spray_status, not_sprayed_reason, not_sprayed_other_reason, property_type, spray_date," +
                    " spray_operator, family_head_name FROM sprayed_structures WHERE id=?";
        } else if (MOSQUITO_COLLECTION.equals(interventionType)) {
            sql = String.format(sql, MOSQUITO_COLLECTIONS_TABLE);
        } else if (LARVAL_DIPPING.equals(interventionType)) {
            sql = String.format(sql, LARVAL_DIPPINGS_TABLE);
        } else if (PAOT.equals(interventionType)) {
            sql = String.format("SELECT %s, %s, %s  FROM %s WHERE %s=? ", PAOT_STATUS,
                    PAOT_COMMENTS, LAST_UPDATED_DATE, PAOT_TABLE, BASE_ENTITY_ID);
        } else if (IRS_VERIFICATION.equals(interventionType)) {
            sql = String.format("SELECT %s, %s, %s, %s, %s, %s FROM %s WHERE id= ?",
                    TRUE_STRUCTURE, ELIGIBLE_STRUCTURE, REPORT_SPRAY, CHALK_SPRAY, STICKER_SPRAY, CARD_SPRAY, IRS_VERIFICATION_TABLE);
        } else if (REGISTER_FAMILY.equals(interventionType)) {
            sql = String.format("SELECT %s, %s, %s FROM %s WHERE %s = ?",
                    BUSINESS_STATUS, AUTHORED_ON, OWNER, TASK_TABLE, FOR);
        }

        final String SQL = sql;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Cursor cursor = getDatabase().rawQuery(SQL, new String[]{featureId});

                CardDetails cardDetails = null;
                try {
                    if (cursor.moveToFirst()) {
                        cardDetails = createCardDetails(cursor, interventionType);
                        cardDetails.setInterventionType(interventionType);
                    }
                } catch (Exception e) {
                    Timber.e(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                // run on ui thread
                final CardDetails CARD_DETAILS = cardDetails;
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (isForForm) {
                            getSprayDetails(interventionType, featureId, CARD_DETAILS);
                            ((ListTaskPresenter) presenterCallBack).onInterventionFormDetailsFetched(CARD_DETAILS);
                        } else {
                            ((ListTaskPresenter) presenterCallBack).onCardDetailsFetched(CARD_DETAILS);
                        }
                    }
                });
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void getSprayDetails(String interventionType, String structureId, CardDetails cardDetails) {
        if (!IRS.equals(interventionType))
            return;
        CommonPersonObject commonPersonObject = interactorUtils.fetchSprayDetails(interventionType, structureId,
                eventClientRepository, commonRepository);
        ((SprayCardDetails) cardDetails).setCommonPersonObject(commonPersonObject);
    }

    private CardDetails createCardDetails(Cursor cursor, String interventionType) {
        CardDetails cardDetails = null;
        if (MOSQUITO_COLLECTION.equals(interventionType) || LARVAL_DIPPING.equals(interventionType)) {
            cardDetails = createMosquitoHarvestCardDetails(cursor, interventionType);
        } else if (IRS.equals(interventionType)) {
            cardDetails = createSprayCardDetails(cursor);
        } else if (PAOT.equals(interventionType)) {
            cardDetails = createPaotCardDetails(cursor, interventionType);
        } else if (IRS_VERIFICATION.equals(interventionType)) {
            cardDetails = createIRSverificationCardDetails(cursor);
        } else if (REGISTER_FAMILY.equals(interventionType)) {
            cardDetails = createFamilyCardDetails(cursor);
        }

        return cardDetails;
    }

    private SprayCardDetails createSprayCardDetails(Cursor cursor) {
        String reason = cursor.getString(cursor.getColumnIndex("not_sprayed_reason"));
        if ("other".equals(reason)) {
            reason = cursor.getString(cursor.getColumnIndex("not_sprayed_other_reason"));
        }
        return new SprayCardDetails(
                CardDetailsUtil.getTranslatedBusinessStatus(cursor.getString(cursor.getColumnIndex("spray_status"))),
                cursor.getString(cursor.getColumnIndex("property_type")),
                cursor.getString(cursor.getColumnIndex("spray_date")),
                cursor.getString(cursor.getColumnIndex("spray_operator")),
                cursor.getString(cursor.getColumnIndex("family_head_name")),
                reason
        );
    }

    private MosquitoHarvestCardDetails createMosquitoHarvestCardDetails(Cursor cursor, String interventionType) {
        return new MosquitoHarvestCardDetails(
                CardDetailsUtil.getTranslatedBusinessStatus(cursor.getString(cursor.getColumnIndex("status"))),
                cursor.getString(cursor.getColumnIndex("start_date")),
                cursor.getString(cursor.getColumnIndex("end_date")),
                interventionType
        );
    }

    private MosquitoHarvestCardDetails createPaotCardDetails(Cursor cursor, String interventionType) {
        MosquitoHarvestCardDetails paotCardDetails = new MosquitoHarvestCardDetails(
                cursor.getString(cursor.getColumnIndex(PAOT_STATUS)),
                cursor.getString(cursor.getColumnIndex(LAST_UPDATED_DATE)),
                null,
                interventionType
        );
        paotCardDetails.setComments(cursor.getString(cursor.getColumnIndex(PAOT_COMMENTS)));
        return paotCardDetails;
    }

    private IRSVerificationCardDetails createIRSverificationCardDetails(Cursor cursor) {
        IRSVerificationCardDetails irsVerificationCardDetails = new IRSVerificationCardDetails(
                COMPLETE,
                cursor.getString(cursor.getColumnIndex(TRUE_STRUCTURE)),
                cursor.getString(cursor.getColumnIndex(ELIGIBLE_STRUCTURE)),
                cursor.getString(cursor.getColumnIndex(REPORT_SPRAY)),
                cursor.getString(cursor.getColumnIndex(CHALK_SPRAY)),
                cursor.getString(cursor.getColumnIndex(STICKER_SPRAY)),
                cursor.getString(cursor.getColumnIndex(CARD_SPRAY))
        );
        return irsVerificationCardDetails;
    }

    private FamilyCardDetails createFamilyCardDetails(Cursor cursor) {
        return new FamilyCardDetails(
                CardDetailsUtil.getTranslatedBusinessStatus(cursor.getString(cursor.getColumnIndex("business_status"))),
                cursor.getString(cursor.getColumnIndex("authored_on")),
                cursor.getString(cursor.getColumnIndex("owner"))

        );
    }

    public void fetchLocations(String plan, String operationalArea) {
        fetchLocations(plan, operationalArea, null, null);
    }

    public void fetchLocations(String plan, String operationalArea, String point, Boolean locationComponentActive) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                JSONObject featureCollection = null;

                Location operationalAreaLocation = Utils.getOperationalAreaLocation(operationalArea);
                List<TaskDetails> taskDetailsList = null;

                try {
                    featureCollection = createFeatureCollection();
                    if (operationalAreaLocation != null) {
                        Map<String, Set<Task>> tasks = taskRepository.getTasksByPlanAndGroup(plan, operationalAreaLocation.getId());
                        List<Location> structures = structureRepository.getLocationsByParentId(operationalAreaLocation.getId());
                        Map<String, StructureDetails> structureNames = getStructureName(operationalAreaLocation.getId());
                        taskDetailsList = IndicatorUtils.processTaskDetails(tasks);
                        String indexCase = null;
                        if (getInterventionLabel() == R.string.focus_investigation)
                            indexCase = getIndexCaseStructure(plan);
                        String features = GeoJsonUtils.getGeoJsonFromStructuresAndTasks(structures, tasks, indexCase, structureNames);
                        featureCollection.put(GeoJSON.FEATURES, new JSONArray(features));

                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
                JSONObject finalFeatureCollection = featureCollection;
                List<TaskDetails> finalTaskDetailsList = taskDetailsList;
                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (operationalAreaLocation != null) {
                            operationalAreaId = operationalAreaLocation.getId();
                            Feature operationalAreaFeature = Feature.fromJson(gson.toJson(operationalAreaLocation));
                            if (locationComponentActive != null) {
                                getPresenter().onStructuresFetched(finalFeatureCollection, operationalAreaFeature, finalTaskDetailsList, point, locationComponentActive);
                            } else {
                                getPresenter().onStructuresFetched(finalFeatureCollection, operationalAreaFeature, finalTaskDetailsList);
                            }
                        } else {
                            getPresenter().onStructuresFetched(finalFeatureCollection, null, null);
                        }
                    }
                });

            }

        };


        appExecutors.diskIO().execute(runnable);
    }


    protected String getStructureNamesSelect(String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(STRUCTURES_TABLE, new String[]{
                String.format("COALESCE(%s.%s,%s,%s,%s)", FAMILY, FIRST_NAME, STRUCTURE_NAME, NAME, FAMILY_HEAD_NAME),
                String.format("group_concat(%s.%s||' '||%s.%s)", FAMILY_MEMBER, FIRST_NAME, FAMILY_MEMBER, LAST_NAME)}, ID_);
        queryBuilder.customJoin(String.format("LEFT JOIN %s ON %s.%s = %s.%s AND %s.%s IS NULL collate nocase ",
                FAMILY, STRUCTURES_TABLE, ID_, FAMILY, STRUCTURE_ID, FAMILY, DATE_REMOVED));
        queryBuilder.customJoin(String.format("LEFT JOIN %s ON %s.%s = %s.%s AND %s.%s IS NULL collate nocase ",
                FAMILY_MEMBER, FAMILY, BASE_ENTITY_ID, FAMILY_MEMBER, RELATIONAL_ID, FAMILY_MEMBER, DATE_REMOVED));
        queryBuilder.customJoin(String.format("LEFT JOIN %s ON %s.%s = %s.%s collate nocase ",
                SPRAYED_STRUCTURES, STRUCTURES_TABLE, ID_, SPRAYED_STRUCTURES, ID));
        return queryBuilder.mainCondition(mainCondition);
    }

    private Map<String, StructureDetails> getStructureName(String parentId) {
        Cursor cursor = null;
        Map<String, StructureDetails> structureNames = new HashMap<>();
        try {
            String query = getStructureNamesSelect(String.format("%s=?",
                    Constants.DatabaseKeys.PARENT_ID)).concat(String.format(" GROUP BY %s.%s", STRUCTURES_TABLE, ID_));
            Timber.d(query);
            cursor = getDatabase().rawQuery(query, new String[]{parentId});
            while (cursor.moveToNext()) {
                structureNames.put(cursor.getString(0), new StructureDetails(cursor.getString(1), cursor.getString(2)));
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return structureNames;
    }

    private String getIndexCaseStructure(String planId) {
        Cursor cursor = null;
        String structureId = null;
        try {
            String query = getMemberTasksSelect(String.format("%s=? AND %s=? ",
                    PLAN_ID, CODE), new String[]{});
            Timber.d(query);
            cursor = getDatabase().rawQuery(query, new String[]{planId, CASE_CONFIRMATION});
            if (cursor.moveToNext()) {
                structureId = cursor.getString(0);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return structureId;
    }

    private JSONObject createFeatureCollection() throws JSONException {
        JSONObject featureCollection = new JSONObject();
        featureCollection.put(GeoJSON.TYPE, GeoJSON.FEATURE_COLLECTION);
        return featureCollection;
    }

    private ListTaskContract.Presenter getPresenter() {
        return (ListTaskContract.Presenter) presenterCallBack;
    }

    public void markStructureAsInactive(Feature feature) {

        try {
            Location structure = structureRepository.getLocationById(feature.id());
            structure.getProperties().setStatus(INACTIVE);
            structureRepository.addOrUpdate(structure);

            String taskIdentifier = feature.getStringProperty(TASK_IDENTIFIER);
            taskRepository.cancelTaskByIdentifier(taskIdentifier);

            revealApplication.setSynced(false);
        } catch (Exception e) {
            Timber.e(e);
        }

        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                ((ListTaskPresenter) presenterCallBack).onStructureMarkedInactive();
            }
        });

    }

    public void markStructureAsIneligible(Feature feature, String reasonUnligible) {

        String taskIdentifier = getPropertyValue(feature, TASK_IDENTIFIER);
        String code = getPropertyValue(feature, TASK_CODE);

        if (REGISTER_FAMILY.equals(code)) {

            Task task = taskRepository.getTaskByIdentifier(taskIdentifier);
            Map<String, String> details = new HashMap<>();
            details.put(TASK_IDENTIFIER, taskIdentifier);
            details.put(Constants.Properties.TASK_BUSINESS_STATUS, task.getBusinessStatus());
            details.put(Constants.Properties.TASK_STATUS, task.getStatus().name());
            details.put(Constants.Properties.LOCATION_ID, feature.id());
            details.put(Constants.Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
            details.put(Constants.Properties.PLAN_IDENTIFIER, task.getPlanIdentifier());
            task.setBusinessStatus(NOT_ELIGIBLE);
            task.setStatus(Task.TaskStatus.COMPLETED);
            task.setLastModified(new DateTime());
            taskRepository.addOrUpdate(task);
            revealApplication.setSynced(false);
            Event event = FamilyJsonFormUtils.createFamilyEvent(task.getForEntity(), feature.id(), details, FamilyConstants.EventType.FAMILY_REGISTRATION_INELIGIBLE);
            event.addObs(new Obs().withValue(reasonUnligible).withFieldCode("eligible").withFieldType("formsubmissionField"));
            event.addObs(new Obs().withValue(task.getBusinessStatus()).withFieldCode("whyNotEligible").withFieldType("formsubmissionField"));
            try {
                eventClientRepository.addEvent(feature.id(), new JSONObject(gson.toJson(event)));
            } catch (JSONException e) {
                Timber.e(e);
            }

        }

        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                ((ListTaskPresenter) presenterCallBack).onStructureMarkedIneligible();
            }
        });
    }

    private String[] getStructureColumns() {
        return new String[]{
                TASK_TABLE + "." + ID_,
                TASK_TABLE + "." + CODE,
                TASK_TABLE + "." + FOR,
                TASK_TABLE + "." + BUSINESS_STATUS,
                TASK_TABLE + "." + STATUS,
                TASK_TABLE + "." + STRUCTURE_ID
        };
    }

    public String getTaskSelect(String mainCondition) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(TASK_TABLE, getStructureColumns(), ID_);
        return queryBuilder.mainCondition(mainCondition);
    }

    public void resetInterventionTaskInfo(String interventionType, String featureId) {
        String sql = String.format(getTaskSelect("%s = ? and %s = ?"),
                FOR, CODE);

        final String SQL = sql;

        appExecutors.diskIO().execute(() -> {
            StructureTaskDetails taskDetails = null;
            Cursor cursor = null;
            boolean taskInfoResetSuccessful = false;
            try {
                cursor = getDatabase().rawQuery(SQL, new String[]{featureId, interventionType});
                if (cursor.moveToNext()) {
                    taskDetails = readTaskDetails(cursor);
                }

            } catch (Exception e) {
                Timber.e(e, "Error querying tasks details for %s", featureId);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            // Reset task info
            taskInfoResetSuccessful = interactorUtils.resetTaskInfo(getDatabase(), taskDetails);


            boolean finalTaskInfoResetSuccessful = taskInfoResetSuccessful;
            appExecutors.mainThread().execute(() -> {
                getPresenter().onInterventionTaskInfoReset(finalTaskInfoResetSuccessful);
            });
        });


    }

    public StructureTaskDetails readTaskDetails(Cursor cursor) {
        StructureTaskDetails task = new StructureTaskDetails(cursor.getString(cursor.getColumnIndex(ID_)));
        task.setTaskCode(cursor.getString(cursor.getColumnIndex(CODE)));
        task.setTaskEntity(cursor.getString(cursor.getColumnIndex(FOR)));
        task.setBusinessStatus(cursor.getString(cursor.getColumnIndex(BUSINESS_STATUS)));
        task.setTaskStatus(cursor.getString(cursor.getColumnIndex(STATUS)));
        task.setStructureId(cursor.getString(cursor.getColumnIndex(STRUCTURE_ID)));
        return task;
    }

    @Override
    public void handleLasteventFound(org.smartregister.domain.Event event) {
        getPresenter().onEventFound(event);
    }

    public void editCDDTaskCompleteStatus(Feature feature, boolean isTaskComplete) {

        String taskIdentifier = getPropertyValue(feature, TASK_IDENTIFIER);

        Task task = taskRepository.getTaskByIdentifier(taskIdentifier);
        Map<String, String> details = new HashMap<>();
        details.put(TASK_IDENTIFIER, taskIdentifier);
        details.put(Constants.Properties.TASK_BUSINESS_STATUS, task.getBusinessStatus());
        details.put(Constants.Properties.TASK_STATUS, task.getStatus().name());
        details.put(Constants.Properties.LOCATION_ID, feature.id());
        details.put(Constants.Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
        String businessStatus = isTaskComplete ? COMPLETE : IN_PROGRESS;
        task.setBusinessStatus(businessStatus);
        task.setStatus(Task.TaskStatus.COMPLETED);
        task.setLastModified(new DateTime());
        taskRepository.addOrUpdate(task);
        revealApplication.setSynced(false);
        appExecutors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                ((ListTaskPresenter) presenterCallBack).onCDDTaskCompleteStatusEdited(businessStatus);
            }
        });
    }

}
