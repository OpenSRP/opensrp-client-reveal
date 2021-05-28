package org.smartregister.reveal.interactor;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.mapbox.geojson.Feature;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.cursoradapter.SmartRegisterQueryBuilder;
import org.smartregister.domain.Client;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Obs;
import org.smartregister.domain.Task;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.util.Constants.INTENT_KEY;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseContract;
import org.smartregister.reveal.contract.BaseContract.BasePresenter;
import org.smartregister.reveal.contract.StructureTasksContract;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.BusinessStatus;
import org.smartregister.reveal.util.Constants.EventType;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.Constants.StructureType;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.FamilyConstants.TABLE_NAME;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TaskUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.widget.GeoWidgetFactory;
import org.smartregister.util.DateTimeTypeConverter;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.util.PropertiesConverter;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

import static com.cocoahero.android.geojson.Geometry.JSON_COORDINATES;
import static org.smartregister.family.util.DBConstants.KEY.BASE_ENTITY_ID;
import static org.smartregister.family.util.DBConstants.KEY.DATE_REMOVED;
import static org.smartregister.family.util.Utils.metadata;
import static org.smartregister.reveal.util.Constants.BEDNET_DISTRIBUTION_EVENT;
import static org.smartregister.reveal.util.Constants.BEHAVIOUR_CHANGE_COMMUNICATION;
import static org.smartregister.reveal.util.Constants.BLOOD_SCREENING_EVENT;
import static org.smartregister.reveal.util.Constants.DETAILS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.FOR;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.ID_;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURES_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.TASK_TABLE;
import static org.smartregister.reveal.util.Constants.EventType.CASE_CONFIRMATION_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.CDD_SUPERVISOR_DAILY_SUMMARY;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.CDD_SUPERVISION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.JsonForm.COMPOUND_STRUCTURE;
import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;
import static org.smartregister.reveal.util.Constants.JsonForm.LOCATION_COMPONENT_ACTIVE;
import static org.smartregister.reveal.util.Constants.JsonForm.PHYSICAL_TYPE;
import static org.smartregister.reveal.util.Constants.JsonForm.STRUCTURE_NAME;
import static org.smartregister.reveal.util.Constants.JsonForm.STRUCTURE_TYPE;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.METADATA;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Constants.STRUCTURE;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;
import static org.smartregister.util.JsonFormUtils.ENTITY_ID;
import static org.smartregister.util.JsonFormUtils.KEY;
import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.VALUES;
import static org.smartregister.util.JsonFormUtils.getJSONObject;
import static org.smartregister.util.JsonFormUtils.getString;


/**
 * Created by samuelgithengi on 3/25/19.
 */
public class BaseInteractor implements BaseContract.BaseInteractor {

    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(DateTime.class, new DateTimeTypeConverter())
            .registerTypeAdapter(LocationProperty.class, new PropertiesConverter()).create();

    private RevealApplication revealApplication;

    protected TaskRepository taskRepository;

    protected StructureRepository structureRepository;

    protected BasePresenter presenterCallBack;

    protected String operationalAreaId;

    protected AppExecutors appExecutors;

    protected AllSharedPreferences sharedPreferences;

    protected EventClientRepository eventClientRepository;

    protected RevealClientProcessor clientProcessor;

    private TaskUtils taskUtils;

    private SQLiteDatabase database;

    private CommonRepository commonRepository;

    private PreferencesUtil prefsUtil;

    public BaseInteractor(BasePresenter presenterCallBack) {
        revealApplication = RevealApplication.getInstance();
        this.presenterCallBack = presenterCallBack;
        appExecutors = revealApplication.getAppExecutors();
        taskRepository = revealApplication.getTaskRepository();
        structureRepository = revealApplication.getStructureRepository();
        eventClientRepository = revealApplication.getContext().getEventClientRepository();
        clientProcessor = RevealClientProcessor.getInstance(revealApplication.getApplicationContext());
        sharedPreferences = revealApplication.getContext().allSharedPreferences();
        taskUtils = TaskUtils.getInstance();
        database = revealApplication.getRepository().getReadableDatabase();
        prefsUtil = PreferencesUtil.getInstance();
    }

    @VisibleForTesting
    public BaseInteractor(BasePresenter presenterCallBack, CommonRepository commonRepository) {
        this(presenterCallBack);
        this.commonRepository = commonRepository;
    }

    @Override
    public void saveJsonForm(String json) {
        String encounterType = null;
        try {
            JSONObject jsonForm = new JSONObject(json);
            encounterType = jsonForm.optString(ENCOUNTER_TYPE);
            boolean refreshMapOnEventSaved = true;
            switch (encounterType) {
                case REGISTER_STRUCTURE_EVENT:
                    saveRegisterStructureForm(jsonForm);
                    break;
                case EventType.MDA_DISPENSE:
                    taskUtils.generateMDAAdherenceTask(RevealApplication.getInstance().getApplicationContext(),
                            getString(jsonForm, ENTITY_ID), getJSONObject(jsonForm, DETAILS).optString(Properties.LOCATION_ID));

                case BLOOD_SCREENING_EVENT:
                case EventType.MDA_ADHERENCE:
                    saveMemberForm(jsonForm, encounterType, BLOOD_SCREENING);
                    break;

                case CASE_CONFIRMATION_EVENT:
                    saveCaseConfirmation(jsonForm, encounterType);
                    break;
                default:
                    saveLocationInterventionForm(jsonForm);
                    if (!encounterType.equals(BEDNET_DISTRIBUTION_EVENT) && !encounterType.equals(EventType.IRS_VERIFICATION)) {
                        refreshMapOnEventSaved = false;
                    }
                    break;
            }
            revealApplication.setRefreshMapOnEventSaved(refreshMapOnEventSaved);
        } catch (Exception e) {
            Timber.e(e);
            presenterCallBack.onFormSaveFailure(encounterType);
        }
    }

    @Override
    public void handleLasteventFound(org.smartregister.domain.Event event) {
        // handle in child class
    }

    private org.smartregister.domain.Event saveEvent(JSONObject jsonForm, String encounterType, String bindType) throws JSONException {
        String entityId = getString(jsonForm, ENTITY_ID);
        JSONArray fields = JsonFormUtils.fields(jsonForm);
        JSONObject metadata = getJSONObject(jsonForm, METADATA);
        Event event = JsonFormUtils.createEvent(fields, metadata, Utils.getFormTag(), entityId, encounterType, bindType);
        JSONObject eventJson = new JSONObject(gson.toJson(event));
        if(BuildConfig.BUILD_COUNTRY.equals(Country.SENEGAL)){
            JSONObject compoundStructureField = JsonFormUtils.getFieldJSONObject(fields,COMPOUND_STRUCTURE);
            JSONArray obsList = (JSONArray) eventJson.get("obs");
            for(int i =0; i < obsList.length();i++){
                JSONObject obs = (JSONObject) obsList.get(i);
                if(obs.get("formSubmissionField").equals(COMPOUND_STRUCTURE)){
                    JSONObject value = (JSONObject) new JSONArray(compoundStructureField.get(VALUE).toString()).get(0);
                    JSONArray values  = new JSONArray();
                    values.put(value.get(KEY));
                    obs.put(VALUES,values);
                    break;
                }

            }
        }
        eventJson.put(DETAILS, getJSONObject(jsonForm, DETAILS));
        eventClientRepository.addEvent(entityId, eventJson);
        return gson.fromJson(eventJson.toString(), org.smartregister.domain.Event.class);
    }

    private void saveLocationInterventionForm(JSONObject jsonForm) {
        String encounterType = null;
        String interventionType = null;
        try {
            encounterType = jsonForm.getString(ENCOUNTER_TYPE);
            if (encounterType.equals(SPRAY_EVENT) || encounterType.equals(EventType.IRS_LITE_VERIFICATION)) {
                interventionType = IRS;
            } else if (encounterType.equals(MOSQUITO_COLLECTION_EVENT)) {
                interventionType = MOSQUITO_COLLECTION;
            } else if (encounterType.equals(LARVAL_DIPPING_EVENT)) {
                interventionType = LARVAL_DIPPING;
            } else if (encounterType.equals(BEDNET_DISTRIBUTION_EVENT)) {
                interventionType = BEDNET_DISTRIBUTION;
            } else if (encounterType.equals(BEHAVIOUR_CHANGE_COMMUNICATION)) {
                interventionType = BCC;
            } else if (encounterType.equals(EventType.PAOT_EVENT)) {
                interventionType = PAOT;
            } else if (encounterType.equals(EventType.MDA_DISPENSE)) {
                interventionType = Intervention.MDA_DISPENSE;
            } else if (encounterType.equals(EventType.MDA_ADHERENCE)) {
                interventionType = Intervention.MDA_ADHERENCE;
            } else if (encounterType.equals(EventType.IRS_VERIFICATION)) {
                interventionType = Intervention.IRS_VERIFICATION;
            } else if (encounterType.equals(EventType.DAILY_SUMMARY_EVENT)) {
                jsonForm.put(ENTITY_ID, UUID.randomUUID().toString());
            }else if (CDD_SUPERVISOR_DAILY_SUMMARY.equals(encounterType)){
                interventionType = CDD_SUPERVISION;
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        final String finalInterventionType = interventionType;
        final String finalEncounterType = encounterType;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if(finalEncounterType.equals(EventType.TABLET_ACCOUNTABILITY_EVENT)){
                        String locationName = JsonFormUtils.getFieldValue(jsonForm.toString(),JsonForm.LOCATION);
                        jsonForm.put(ENTITY_ID, getStructureIdByName(locationName));
                    }
                    org.smartregister.domain.Event event = saveEvent(jsonForm, finalEncounterType, STRUCTURE);
                    clientProcessor.processClient(Collections.singletonList(new EventClient(event, null)), true);
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            String businessStatus = clientProcessor.calculateBusinessStatus(event);
                            String taskID = event.getDetails() == null ? null : event.getDetails().get(Properties.TASK_IDENTIFIER);
                            presenterCallBack.onFormSaved(event.getBaseEntityId(), taskID, Task.TaskStatus.COMPLETED, businessStatus, finalInterventionType);
                        }
                    });
                } catch (JSONException e) {
                    Timber.e(e, "Error saving saving Form ");
                    presenterCallBack.onFormSaveFailure(finalEncounterType);
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void saveRegisterStructureForm(JSONObject jsonForm) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    jsonForm.put(ENTITY_ID, UUID.randomUUID().toString());
                    JSONObject eventDetails = new JSONObject();
                    eventDetails.put(Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
                    eventDetails.put(Properties.LOCATION_PARENT, operationalAreaId);
                    String planIdentifier = PreferencesUtil.getInstance().getCurrentPlanId();
                    eventDetails.put(Properties.PLAN_IDENTIFIER, planIdentifier);
                    jsonForm.put(DETAILS, eventDetails);
                    org.smartregister.domain.Event event = saveEvent(jsonForm, REGISTER_STRUCTURE_EVENT, STRUCTURE);
                    com.cocoahero.android.geojson.Feature feature = new com.cocoahero.android.geojson.Feature(new JSONObject(event.findObs(null, false, "structure").getValue().toString()));
                    Date now = new Date();
                    Location structure = new Location();
                    structure.setId(event.getBaseEntityId());
                    structure.setType(feature.getType());
                    org.smartregister.domain.Geometry geometry = new org.smartregister.domain.Geometry();
                    geometry.setType(org.smartregister.domain.Geometry.GeometryType.valueOf(feature.getGeometry().getType().toUpperCase()));
                    JsonArray coordinates = new JsonArray();
                    JSONArray featureCoordinates = feature.getGeometry().toJSON().getJSONArray(JSON_COORDINATES);
                    coordinates.add(Double.parseDouble(featureCoordinates.get(0).toString()));
                    coordinates.add(Double.parseDouble(featureCoordinates.get(1).toString()));
                    geometry.setCoordinates(coordinates);
                    structure.setGeometry(geometry);
                    LocationProperty properties = new LocationProperty();
                    String structureType = event.findObs(null, false, STRUCTURE_TYPE).getValue().toString();
                    properties.setType(structureType);
                    properties.setEffectiveStartDate(now);
                    properties.setParentId(operationalAreaId);
                    properties.setStatus(LocationProperty.PropertyStatus.PENDING_REVIEW);
                    properties.setUid(UUID.randomUUID().toString());
                    Obs structureNameObs = event.findObs(null, false, STRUCTURE_NAME);
                    if (structureNameObs != null && structureNameObs.getValue() != null) {
                        properties.setName(structureNameObs.getValue().toString());
                    }
                    Obs physicalTypeObs = event.findObs(null, false, PHYSICAL_TYPE);
                    if (physicalTypeObs != null && physicalTypeObs.getValue() != null) {
                        Map<String, String> customProperties = new HashMap<>();
                        customProperties.put(PHYSICAL_TYPE, physicalTypeObs.getValue().toString());
                        properties.setCustomProperties(customProperties);
                    }
                    structure.setProperties(properties);
                    structure.setSyncStatus(BaseRepository.TYPE_Created);
                    structureRepository.addOrUpdate(structure);
                    revealApplication.setSynced(false);
                    Context applicationContext = revealApplication.getApplicationContext();
                    Task task = null;
                    if (StructureType.RESIDENTIAL.equals(structureType) && Utils.isFocusInvestigationOrMDA()) {
                        task = taskUtils.generateRegisterFamilyTask(applicationContext, structure.getId());
                    } else {
                        if (StructureType.RESIDENTIAL.equals(structureType)) {
                            task = taskUtils.generateTask(applicationContext, structure.getId(), structure.getId(),
                                    BusinessStatus.NOT_VISITED, Intervention.IRS, R.string.irs_task_description);
                        } else if (StructureType.MOSQUITO_COLLECTION_POINT.equals(structureType)) {
                            task = taskUtils.generateTask(applicationContext, structure.getId(), structure.getId(),
                                    BusinessStatus.NOT_VISITED, Intervention.MOSQUITO_COLLECTION, R.string.mosquito_collection_task_description);
                        } else if (StructureType.LARVAL_BREEDING_SITE.equals(structureType)) {
                            task = taskUtils.generateTask(applicationContext, structure.getId(), structure.getId(),
                                    BusinessStatus.NOT_VISITED, Intervention.LARVAL_DIPPING, R.string.larval_dipping_task_description);
                        } else if (StructureType.POTENTIAL_AREA_OF_TRANSMISSION.equals(structureType)) {
                            task = taskUtils.generateTask(applicationContext, structure.getId(), structure.getId(),
                                    BusinessStatus.NOT_VISITED, PAOT, R.string.poat_task_description);
                        }
                    }
                    clientProcessor.processClient(Collections.singletonList(new EventClient(event, null)), true);
                    Task finalTask = task;
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, String> taskProperties = new HashMap<>();
                            if (finalTask != null) {

                                taskProperties.put(Properties.TASK_IDENTIFIER, finalTask.getIdentifier());
                                taskProperties.put(Properties.TASK_BUSINESS_STATUS, finalTask.getBusinessStatus());
                                taskProperties.put(Properties.TASK_STATUS, finalTask.getStatus().name());
                                taskProperties.put(Properties.TASK_CODE, finalTask.getCode());
                            }
                            taskProperties.put(Properties.LOCATION_UUID, structure.getProperties().getUid());
                            taskProperties.put(Properties.LOCATION_VERSION, structure.getProperties().getVersion() + "");
                            taskProperties.put(Properties.LOCATION_TYPE, structure.getProperties().getType());
                            structure.getProperties().setCustomProperties(taskProperties);


                            Obs myLocationActiveObs = event.findObs(null, false, LOCATION_COMPONENT_ACTIVE);

                            boolean myLocationActive = myLocationActiveObs != null && Boolean.valueOf(myLocationActiveObs.getValue().toString());
                            revealApplication.setMyLocationComponentEnabled(myLocationActive);


                            Obs zoomObs = event.findObs(null, false, GeoWidgetFactory.ZOOM_LEVEL);
                            double zoomLevel = Double.parseDouble(zoomObs.getValue().toString());

                            presenterCallBack.onStructureAdded(Feature.fromJson(gson.toJson(structure)), featureCoordinates, zoomLevel);
                        }
                    });
                } catch (JSONException e) {
                    Timber.e(e, "Error saving new Structure");
                    presenterCallBack.onFormSaveFailure(REGISTER_STRUCTURE_EVENT);
                }
            }
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void saveMemberForm(JSONObject jsonForm, String eventType, String intervention) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    org.smartregister.domain.Event event = saveEvent(jsonForm, eventType, FAMILY_MEMBER);
                    Client client = eventClientRepository.fetchClientByBaseEntityId(event.getBaseEntityId());
                    clientProcessor.processClient(Collections.singletonList(new EventClient(event, client)), true);
                    appExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            String businessStatus = clientProcessor.calculateBusinessStatus(event);
                            String taskID = event.getDetails().get(Properties.TASK_IDENTIFIER);
                            presenterCallBack.onFormSaved(event.getBaseEntityId(), taskID, Task.TaskStatus.COMPLETED, businessStatus, intervention);
                        }
                    });
                } catch (Exception e) {
                    Timber.e("Error saving member event form");
                }
            }
        };
        appExecutors.diskIO().execute(runnable);
    }

    private void saveCaseConfirmation(JSONObject jsonForm, String eventType) {
        appExecutors.diskIO().execute(() -> {
            try {
                String baseEntityId = JsonFormUtils.getFieldValue(JsonFormUtils.fields(jsonForm), JsonForm.FAMILY_MEMBER);
                jsonForm.put(ENTITY_ID, baseEntityId);
                org.smartregister.domain.Event event = saveEvent(jsonForm, eventType, CASE_CONFIRMATION);
                Client client = eventClientRepository.fetchClientByBaseEntityId(event.getBaseEntityId());
                String taskID = event.getDetails().get(Properties.TASK_IDENTIFIER);
                String businessStatus = clientProcessor.calculateBusinessStatus(event);
                Task task = taskRepository.getTaskByIdentifier(taskID);
                task.setForEntity(baseEntityId);
                task.setBusinessStatus(businessStatus);
                task.setStatus(Task.TaskStatus.COMPLETED);
                task.setSyncStatus(BaseRepository.TYPE_Created);
                taskRepository.addOrUpdate(task);
                Set<Task> removedTasks = new HashSet<>();
                for (Task bloodScreeningTask : taskRepository.getTasksByEntityAndCode(prefsUtil.getCurrentPlanId(),
                        Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea()).getId(), baseEntityId, BLOOD_SCREENING)) {
                    bloodScreeningTask.setStatus(Task.TaskStatus.CANCELLED);
                    bloodScreeningTask.setSyncStatus(BaseRepository.TYPE_Created);
                    taskRepository.addOrUpdate(bloodScreeningTask);
                    removedTasks.add(bloodScreeningTask);
                }
                revealApplication.setSynced(false);
                clientProcessor.processClient(Collections.singletonList(new EventClient(event, client)), true);
                appExecutors.mainThread().execute(() -> {
                    ((StructureTasksContract.Presenter) presenterCallBack).onIndexConfirmationFormSaved(taskID, Task.TaskStatus.COMPLETED, businessStatus, removedTasks);
                });
            } catch (Exception e) {
                Timber.e("Error saving case confirmation data");
            }
        });
    }

    protected String getMemberTasksSelect(String mainCondition, String[] memberColumns) {
        SmartRegisterQueryBuilder queryBuilder = new SmartRegisterQueryBuilder();
        queryBuilder.selectInitiateMainTable(STRUCTURES_TABLE, memberColumns, ID_);
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                FAMILY_MEMBER, FAMILY_MEMBER, STRUCTURE_ID, STRUCTURES_TABLE, ID_));
        queryBuilder.customJoin(String.format(" JOIN %s ON %s.%s = %s.%s ",
                TASK_TABLE, TASK_TABLE, FOR, FAMILY_MEMBER, BASE_ENTITY_ID));
        return queryBuilder.mainCondition(mainCondition);
    }

    public void fetchFamilyDetails(String structureId) {
        appExecutors.diskIO().execute(() -> {
            Cursor cursor = null;
            CommonPersonObjectClient family = null;
            try {
                cursor = database.rawQuery(String.format("SELECT %s FROM %S WHERE %s = ? AND %s IS NULL",
                        INTENT_KEY.BASE_ENTITY_ID, TABLE_NAME.FAMILY, STRUCTURE_ID, DATE_REMOVED), new String[]{structureId});
                if (cursor.moveToNext()) {
                    String baseEntityId = cursor.getString(0);
                    setCommonRepository();
                    final CommonPersonObject personObject = commonRepository.findByBaseEntityId(baseEntityId);
                    family = new CommonPersonObjectClient(personObject.getCaseId(),
                            personObject.getDetails(), "");
                    family.setColumnmaps(personObject.getColumnmaps());
                }
            } catch (Exception e) {
                Timber.e(e);
            } finally {
                if (cursor != null)
                    cursor.close();
            }

            CommonPersonObjectClient finalFamily = family;
            appExecutors.mainThread().execute(() -> {
                presenterCallBack.onFamilyFound(finalFamily);
            });
        });
    }

    public SQLiteDatabase getDatabase() {
        return database;
    }

    public void setCommonRepository() {
        if (commonRepository == null) {
            commonRepository = revealApplication.getContext().commonrepository(metadata().familyRegister.tableName);

        }
    }

    @Override
    public void findLastEvent(String eventBaseEntityId, String eventType) {

        appExecutors.diskIO().execute(() -> {
            String events = String.format("select %s from %s where %s = ? and %s =? order by %s desc limit 1",
                    EventClientRepository.event_column.json, EventClientRepository.Table.event.name(), EventClientRepository.event_column.baseEntityId, EventClientRepository.event_column.eventType, EventClientRepository.event_column.updatedAt);

            try (Cursor cursor = getDatabase().rawQuery(events, new String[]{eventBaseEntityId, eventType});) {

                if (cursor.moveToFirst()) {
                    String eventJSON = cursor.getString(0);
                    handleLasteventFound(eventClientRepository.convert(eventJSON, org.smartregister.domain.Event.class));

                } else {
                    handleLasteventFound(null);
                }
            } catch (SQLException e) {
                Timber.e(e);
            }
        });

    }
    private String getStructureIdByName(String locationName){
        String structureId = null;
        String query = String.format("select %s from  %s where %s = ? limit 1", ID_, STRUCTURES_TABLE, Constants.DatabaseKeys.NAME);
        try(Cursor cursor = getDatabase().rawQuery(query,new String[]{locationName})){
            if(cursor.moveToFirst()){
                structureId = cursor.getString(0);
            }
        }catch (SQLException e){
            Timber.e(e);
        }
        return structureId;
    }

}
