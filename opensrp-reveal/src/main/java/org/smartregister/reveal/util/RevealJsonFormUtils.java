package org.smartregister.reveal.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.core.util.Pair;

import com.mapbox.geojson.Feature;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.Obs;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.activity.RevealJsonFormActivity;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants.CONFIGURATION;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.util.JsonFormUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

import static com.vijay.jsonwizard.constants.JsonFormConstants.CHECK_BOX;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEYS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUES;
import static org.smartregister.AllConstants.JSON_FILE_EXTENSION;
import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.AllConstants.TEXT;
import static org.smartregister.reveal.util.Constants.BEDNET_DISTRIBUTION_EVENT;
import static org.smartregister.reveal.util.Constants.BEHAVIOUR_CHANGE_COMMUNICATION;
import static org.smartregister.reveal.util.Constants.BLOOD_SCREENING_EVENT;
import static org.smartregister.reveal.util.Constants.DETAILS;
import static org.smartregister.reveal.util.Constants.ENTITY_ID;
import static org.smartregister.reveal.util.Constants.EventType.CASE_CONFIRMATION_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.IRS_LITE_VERIFICATION;
import static org.smartregister.reveal.util.Constants.EventType.IRS_VERIFICATION;
import static org.smartregister.reveal.util.Constants.JSON_FORM_PARAM_JSON;
import static org.smartregister.reveal.util.Constants.JsonForm.COMPOUND_STRUCTURE;
import static org.smartregister.reveal.util.Constants.JsonForm.JSON_FORM_FOLDER;
import static org.smartregister.reveal.util.Constants.JsonForm.YES;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MACEPA_PROVINCES;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_GET_JSON;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Constants.Tags.HEALTH_CENTER;
import static org.smartregister.reveal.util.Constants.Tags.OPERATIONAL_AREA;
import static org.smartregister.reveal.util.Constants.Tags.ZONE;
import static org.smartregister.reveal.util.Utils.getPropertyValue;
import static org.smartregister.reveal.util.Utils.isZambiaIRSLite;


/**
 * Created by samuelgithengi on 3/22/19.
 */
public class RevealJsonFormUtils {

    private Set<String> nonEditablefields;
    private LocationHelper locationHelper = LocationHelper.getInstance();

    public RevealJsonFormUtils() {
        nonEditablefields = new HashSet<>(Arrays.asList(JsonForm.HOUSEHOLD_ACCESSIBLE,
                JsonForm.ABLE_TO_SPRAY_FIRST,JsonForm.CDD_SUPERVISION_TASK_COMPLETE));
    }

    public JSONObject getFormJSON(Context context, String formName, Feature feature, String sprayStatus, String familyHead) {

        String taskBusinessStatus = getPropertyValue(feature, Properties.TASK_BUSINESS_STATUS);
        String taskIdentifier = getPropertyValue(feature, Properties.TASK_IDENTIFIER);
        String taskStatus = getPropertyValue(feature, Properties.TASK_STATUS);

        String structureId = feature.id();
        String structureUUID = getPropertyValue(feature, Properties.LOCATION_UUID);
        String structureVersion = getPropertyValue(feature, Properties.LOCATION_VERSION);
        String structureType = getPropertyValue(feature, Properties.LOCATION_TYPE);

        String formString = getFormString(context, formName, structureType);
        try {
            JSONObject formJson = populateFormDetails(formString, structureId, structureId, taskIdentifier,
                    taskBusinessStatus, taskStatus, structureUUID,
                    structureVersion == null ? null : Integer.valueOf(structureVersion));

            populateFormFields(formJson, structureType, sprayStatus, familyHead);
            return formJson;
        } catch (Exception e) {
            Timber.e(e, "error launching form" + formName);
        }
        return null;
    }

    public JSONObject getFormJSON(Context context, String formName, BaseTaskDetails task, Location structure) {

        String taskBusinessStatus = "";
        String taskIdentifier = "";
        String taskStatus = "";
        String entityId = "";
        if (task != null) {
            taskBusinessStatus = task.getBusinessStatus();
            taskIdentifier = task.getTaskId();
            taskStatus = task.getTaskStatus();

            entityId = task.getTaskEntity();
        }

        String structureId = "";
        String structureUUID = "";
        int structureVersion = 0;
        String structureType = "";
        if (structure != null) {
            structureId = structure.getId();
            structureUUID = structure.getProperties().getUid();
            structureVersion = structure.getProperties().getVersion();
            structureType = structure.getProperties().getType();
        }

        String sprayStatus = null;
        String familyHead = null;

        if (task instanceof TaskDetails) {
            sprayStatus = ((TaskDetails) task).getSprayStatus();
            familyHead = ((TaskDetails) task).getFamilyName();
        }

        String formString = getFormString(context, formName, structureType);
        try {
            JSONObject formJson = populateFormDetails(formString, entityId, structureId, taskIdentifier,
                    taskBusinessStatus, taskStatus, structureUUID, structureVersion);
            populateFormFields(formJson, structureType, sprayStatus, familyHead);
            return formJson;
        } catch (JSONException e) {
            Timber.e(e, "error launching form" + formName);
        }
        return null;
    }

    public String getFormString(Context context, String formName, String structureType) {
        String formString = null;
        try {
            FormUtils formUtils = new FormUtils();
            String formattedFormName = formName.replace(JSON_FORM_FOLDER, "").replace(JSON_FILE_EXTENSION, "");
            JSONObject formStringObj = formUtils.getFormJsonFromRepositoryOrAssets(context, formattedFormName);
            if (formStringObj == null) {
                return null;
            }
            formString = formStringObj.toString();
            if ((JsonForm.SPRAY_FORM.equals(formName) || JsonForm.SPRAY_FORM_BOTSWANA.equals(formName)
                    || JsonForm.SPRAY_FORM_NAMIBIA.equals(formName))) {
                String structType = structureType;
                if (StringUtils.isBlank(structureType)) {
                    structType = Constants.StructureType.NON_RESIDENTIAL;
                }
                formString = formString.replace(JsonForm.STRUCTURE_PROPERTIES_TYPE, structType);
            }

        } catch (Exception e) {
            Timber.e(e);
        }
        return formString;
    }


    public JSONObject populateFormDetails(String formString, String entityId, String structureId, String taskIdentifier,
                                          String taskBusinessStatus, String taskStatus, String structureUUID,
                                          Integer structureVersion) throws JSONException {

        JSONObject formJson = new JSONObject(formString);
        formJson.put(ENTITY_ID, entityId);
        JSONObject formData = new JSONObject();
        formData.put(Properties.TASK_IDENTIFIER, taskIdentifier);
        formData.put(Properties.TASK_BUSINESS_STATUS, taskBusinessStatus);
        formData.put(Properties.TASK_STATUS, taskStatus);
        formData.put(Properties.LOCATION_ID, structureId);
        formData.put(Properties.LOCATION_UUID, structureUUID);
        formData.put(Properties.LOCATION_VERSION, structureVersion);
        formData.put(Properties.APP_VERSION_NAME, BuildConfig.VERSION_NAME);
        formData.put(Properties.FORM_VERSION, formJson.optString("form_version"));
        String planIdentifier = PreferencesUtil.getInstance().getCurrentPlanId();
        formData.put(Properties.PLAN_IDENTIFIER, planIdentifier);
        formJson.put(DETAILS, formData);
        return formJson;
    }


    private void populateFormFields(JSONObject formJson, String structureType, String sprayStatus, String familyHead) throws JSONException {

        JSONArray fields = org.smartregister.util.JsonFormUtils.fields(formJson);
        if (StringUtils.isNotBlank(structureType) || StringUtils.isNotBlank(sprayStatus) || StringUtils.isNotBlank(familyHead)) {
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                String key = field.getString(KEY);
                if (key.equalsIgnoreCase(JsonForm.STRUCTURE_TYPE))
                    field.put(org.smartregister.util.JsonFormUtils.VALUE, structureType);
                else if (key.equalsIgnoreCase(JsonForm.SPRAY_STATUS))
                    field.put(org.smartregister.util.JsonFormUtils.VALUE, sprayStatus);
                else if (key.equalsIgnoreCase(JsonForm.HEAD_OF_HOUSEHOLD))
                    field.put(org.smartregister.util.JsonFormUtils.VALUE, familyHead);
            }
        }

    }


    public void startJsonForm(JSONObject form, Activity context) {
        startJsonForm(form, context, REQUEST_CODE_GET_JSON);
    }

    public void startJsonForm(JSONObject form, Activity context, int requestCode) {
        Intent intent = new Intent(context, RevealJsonFormActivity.class);
        try {
            intent.putExtra(JSON_FORM_PARAM_JSON, form.toString());
            context.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public String getFormName(String encounterType, String taskCode) {
        String formName = null;
        if (SPRAY_EVENT.equals(encounterType) || Intervention.IRS.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA) {
                formName = JsonForm.SPRAY_FORM_NAMIBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.BOTSWANA) {
                formName = JsonForm.SPRAY_FORM_BOTSWANA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.SPRAY_FORM_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_SPRAY_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.SPRAY_FORM_REFAPP;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL) {
                formName = JsonForm.SPRAY_FORM_SENEGAL;
            } else {
                formName = JsonForm.SPRAY_FORM;
            }
        } else if (MOSQUITO_COLLECTION_EVENT.equals(encounterType)
                || Intervention.MOSQUITO_COLLECTION.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_MOSQUITO_COLLECTION_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN) {
                formName = JsonForm.THAILAND_EN_MOSQUITO_COLLECTION_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_MOSQUITO_COLLECTION_FORM;
            } else {
                formName = JsonForm.MOSQUITO_COLLECTION_FORM;
            }
        } else if (BEDNET_DISTRIBUTION_EVENT.equals(encounterType)
                || Intervention.BEDNET_DISTRIBUTION.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_BEDNET_DISTRIBUTION_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN) {
                formName = JsonForm.THAILAND_EN_BEDNET_DISTRIBUTION_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_BEDNET_DISTRIBUTION_FORM;
            } else {
                formName = JsonForm.BEDNET_DISTRIBUTION_FORM;
            }
        } else if (CASE_CONFIRMATION_EVENT.equals(encounterType)
                || Intervention.CASE_CONFIRMATION.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_CASE_CONFIRMATION_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_CASE_CONFIRMATION_FORM;
            } else {
                formName = JsonForm.CASE_CONFIRMATION_FORM;
            }
        } else if (BLOOD_SCREENING_EVENT.equals(encounterType)
                || Intervention.BLOOD_SCREENING.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_BLOOD_SCREENING_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN) {
                formName = JsonForm.THAILAND_EN_BLOOD_SCREENING_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_BLOOD_SCREENING_FORM;
            } else {
                formName = JsonForm.BLOOD_SCREENING_FORM;
            }
        } else if (LARVAL_DIPPING_EVENT.equals(encounterType) || Intervention.LARVAL_DIPPING.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_LARVAL_DIPPING_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN) {
                formName = JsonForm.THAILAND_EN_LARVAL_DIPPING_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_LARVAL_DIPPING_FORM;
            } else {
                formName = JsonForm.LARVAL_DIPPING_FORM;
            }
        } else if (BEHAVIOUR_CHANGE_COMMUNICATION.equals(encounterType) || Intervention.BCC.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_BEHAVIOUR_CHANGE_COMMUNICATION_FORM;
            } else {
                formName = JsonForm.BEHAVIOUR_CHANGE_COMMUNICATION_FORM;
            }
        } else if (REGISTER_STRUCTURE_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_ADD_STRUCTURE_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA) {
                formName = JsonForm.NAMIBIA_ADD_STRUCTURE_FORM;
            } else {
                formName = JsonForm.ADD_STRUCTURE_FORM;
            }
        } else if (Constants.EventType.PAOT_EVENT.equals(encounterType) || Intervention.PAOT.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
                formName = JsonForm.THAILAND_PAOT_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_PAOT_FORM;
            } else {
                formName = JsonForm.PAOT_FORM;
            }
        } else if (Intervention.MDA_ADHERENCE.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.ZAMBIA_MDA_ADHERENCE_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_MDA_ADHERENCE_FORM;
            }
        } else if (Intervention.MDA_DISPENSE.equals(taskCode)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.ZAMBIA_MDA_DISPENSE_FORM;
            } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
                formName = JsonForm.REFAPP_MDA_DISPENSE_FORM;
            }
        } else if (IRS_VERIFICATION.equals(encounterType) || Intervention.IRS_VERIFICATION.equals(taskCode) || IRS_LITE_VERIFICATION.equals(encounterType)) {
            if(isZambiaIRSLite()) {
                return JsonForm.IRS_LITE_VERIFICATION;
            }
            formName = JsonForm.ZAMBIA_IRS_VERIFICATION_FORM;
        } else if (Constants.EventType.DAILY_SUMMARY_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.DAILY_SUMMARY_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.DAILY_SUMMARY_SENEGAL;
            }
        } else if (Constants.EventType.IRS_FIELD_OFFICER_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.IRS_FIELD_OFFICER_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.IRS_FIELD_OFFICER_SENEGAL;
            }
        } else if (Constants.EventType.IRS_SA_DECISION_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.IRS_SA_DECISION_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.IRS_SA_DECISION_SENEGAL;
            }
        } else if (Constants.EventType.MOBILIZATION_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.MOBILIZATION_FORM_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.MOBILIZATION_FORM_SENEGAL;
            }
        } else if (Constants.EventType.TEAM_LEADER_DOS_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.TEAM_LEADER_DOS_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.TEAM_LEADER_DOS_SENEGAL;
            }
        } else if (Constants.EventType.VERIFICATION_EVENT.equals(encounterType)) {
            if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                formName = JsonForm.VERIFICATION_FORM_ZAMBIA;
            } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                formName = JsonForm.VERIFICATION_FORM_SENEGAL;
            }
        } else if (Constants.EventType.TABLET_ACCOUNTABILITY_EVENT.equals(encounterType)){
            formName = JsonForm.TABLET_ACCOUNTABILITY_FORM;
        }else if(Constants.EventType.CDD_SUPERVISOR_DAILY_SUMMARY.equals(encounterType) || Intervention.CDD_SUPERVISION.equals(taskCode)){
            return JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM;
        }
        return formName;
    }

    public String getFormName(String encounterType) {
        return getFormName(encounterType, null);
    }

    public void populatePAOTForm(MosquitoHarvestCardDetails cardDetails, JSONObject formJson) {
        if (formJson == null)
            return;
        try {
            populateField(formJson, JsonForm.PAOT_STATUS, cardDetails.getStatus(), VALUE);
            populateField(formJson, JsonForm.PAOT_COMMENTS, cardDetails.getComments(), VALUE);
            populateField(formJson, JsonForm.LAST_UPDATED_DATE, cardDetails.getStartDate(), VALUE);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public void populateField(JSONObject formJson, String key, String value, String fieldToPopulate) throws JSONException {
        JSONObject field = JsonFormUtils.getFieldJSONObject(JsonFormUtils.getMultiStepFormFields(formJson), key);
        if (field != null) {
            field.put(fieldToPopulate, value);
        }
    }

    public void populateSprayForm(CommonPersonObject commonPersonObject, JSONObject formJson) {
        if (commonPersonObject == null || commonPersonObject.getDetails() == null)
            return;
        JSONArray fields = JsonFormUtils.fields(formJson);
        for (int i = 0; i < fields.length(); i++) {
            try {
                JSONObject field = fields.getJSONObject(i);
                String key = field.getString(KEY);
                if (commonPersonObject.getDetails().containsKey(key)) {
                    String value = commonPersonObject.getDetails().get(key);
                    if (StringUtils.isNotBlank(value))
                        field.put(VALUE, value);
                    if (nonEditablefields.contains(key) && "Yes".equalsIgnoreCase(value)) {
                        field.put(JsonFormConstants.READ_ONLY, true);
                        field.remove(JsonFormConstants.RELEVANCE);
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }

        }
    }

    public void populateForm(Event event, JSONObject formJSON) {
        if (event == null)
            return;
        JSONArray fields = JsonFormUtils.fields(formJSON);
        for (int i = 0; i < fields.length(); i++) {
            try {
                JSONObject field = fields.getJSONObject(i);
                String key = field.getString(KEY);
                Obs obs = event.findObs(null, false, key);
                if (obs != null && obs.getValues() != null) {
                    if (CHECK_BOX.equals(field.getString(TYPE))) {
                        JSONArray options = field.getJSONArray(OPTIONS);
                        Map<String, String> optionsKeyValue = new HashMap<>();
                        for (int j = 0; j < options.length(); j++) {
                            JSONObject option = options.getJSONObject(j);
                            optionsKeyValue.put(option.getString(TEXT), option.getString(KEY));
                        }
                        JSONArray keys = new JSONArray();
                        for (Object value : obs.getValues()) {
                            keys.put(optionsKeyValue.get(value.toString()));
                        }
                        field.put(VALUE, keys);

                    } else {
                        if (!JsonFormConstants.REPEATING_GROUP.equals(field.optString(TYPE))) {
                            field.put(VALUE, obs.getValue());
                        }
                        if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA && nonEditablefields.contains(key)
                                && YES.equalsIgnoreCase(obs.getValue().toString())) {
                            field.put(JsonFormConstants.READ_ONLY, true);
                            field.remove(JsonFormConstants.RELEVANCE);
                        }
                    }
                    if(Country.KENYA.equals(BuildConfig.BUILD_COUNTRY) && nonEditablefields.contains(key)){
                        field.put(JsonFormConstants.READ_ONLY,true);
                    }

                    if(Country.SENEGAL.equals(BuildConfig.BUILD_COUNTRY) && key.equals(COMPOUND_STRUCTURE)){
                        populateCompoundStructureOptions(formJSON,Utils.getOperationalAreaLocation(PreferencesUtil.getInstance().getCurrentOperationalArea()));
                        JSONArray options = field.optJSONArray(OPTIONS);
                        for(int j=0;j < options.length();j++){
                            JSONObject option = (JSONObject) options.get(j);
                            JSONArray value = new JSONArray();
                            value.put(option);
                            if(option.get(KEY).equals(obs.getValue())){
                                field.put(VALUE,value);
                                break;
                            }
                        }
                    }
                }
                if (JsonFormConstants.REPEATING_GROUP.equals(field.optString(TYPE))) {
                    generateRepeatingGroupFields(field, event.getObs(), formJSON);
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }

    public void generateRepeatingGroupFields(JSONObject field, List<Obs> obs, JSONObject formJSON) {
        try {
            LinkedHashMap<String, HashMap<String, String>> repeatingGroupMap = Utils.buildRepeatingGroup(field, obs);
            List<HashMap<String, String>> repeatingGroupMapList = Utils.generateListMapOfRepeatingGrp(repeatingGroupMap);
            new RepeatingGroupGenerator(formJSON.optJSONObject(JsonFormConstants.STEP1),
                //    JsonFormConstants.STEP1,
                    field.optString(KEY),
                    new HashMap<>(),
                    Constants.JsonForm.REPEATING_GROUP_UNIQUE_ID,
                    repeatingGroupMapList).init();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Pair<JSONArray, JSONArray> populateServerOptions(Map<String, Object> serverConfigs, String settingsConfigKey, JSONObject field, String filterKey) {
        if (serverConfigs == null || field == null)
            return null;
        JSONArray serverConfig = (JSONArray) serverConfigs.get(settingsConfigKey);
        if (serverConfig != null && !serverConfig.isNull(0)) {
            JSONArray options = serverConfig.optJSONObject(0).optJSONArray(filterKey);
            if (options == null)
                return null;
            JSONArray codes = new JSONArray();
            JSONArray values = new JSONArray();
            for (int i = 0; i < options.length(); i++) {
                JSONObject operator = options.optJSONObject(i);
                if (operator == null)
                    continue;
                String code = operator.optString(CONFIGURATION.CODE, null);
                String name = operator.optString(CONFIGURATION.NAME);
                if (StringUtils.isBlank(code) || code.equalsIgnoreCase(name)) {
                    codes.put(name);
                    values.put(name);
                } else {
                    codes.put(code + ":" + name);
                    values.put(code + " - " + name);
                }
            }
            try {
                field.put(KEYS, codes);
                field.put(VALUES, values);
            } catch (JSONException e) {
                Timber.e(e, "Error populating %s Operators ", filterKey);
            }
            return new Pair<>(codes, values);
        }
        return null;
    }

    public static org.smartregister.clientandeventmodel.Event createTaskEvent(String baseEntityId, String locationId, Map<String, String> details, String eventType, String entityType) {
        org.smartregister.clientandeventmodel.Event taskEvent = (org.smartregister.clientandeventmodel.Event) new org.smartregister.clientandeventmodel.Event().withBaseEntityId(baseEntityId).withEventDate(new Date()).withEventType(eventType)
                .withLocationId(locationId).withEntityType(entityType).withFormSubmissionId(UUID.randomUUID().toString()).withDateCreated(new Date());
        return taskEvent;
    }

    public Map<String, JSONObject> getFields(JSONObject formJSON) {
        JSONArray fields = JsonFormUtils.fields(formJSON);
        Map<String, JSONObject> fieldsMap = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.optJSONObject(i);
            fieldsMap.put(field.optString(JsonFormUtils.KEY), field);
        }
        return fieldsMap;
    }

    public void populateFormWithServerOptions(String formName, JSONObject formJSON) {

            Map<String, JSONObject> fieldsMap = getFields(formJSON);
        switch (formName) {

            case JsonForm.IRS_SA_DECISION_ZAMBIA:
            case JsonForm.CB_SPRAY_AREA_ZAMBIA:
            case JsonForm.IRS_LITE_VERIFICATION:
            case JsonForm.MOBILIZATION_FORM_ZAMBIA:
            case JsonForm.IRS_SA_DECISION_SENEGAL:
            case JsonForm.CB_SPRAY_AREA_SENEGAL:
            case JsonForm.MOBILIZATION_FORM_SENEGAL:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.SUPERVISORS, fieldsMap.get(JsonForm.SUPERVISOR),
                        PreferencesUtil.getInstance().getCurrentDistrict());
                break;

            case JsonForm.IRS_FIELD_OFFICER_ZAMBIA:
            case JsonForm.IRS_FIELD_OFFICER_SENEGAL:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.FIELD_OFFICERS, fieldsMap.get(JsonForm.FIELD_OFFICER),
                        PreferencesUtil.getInstance().getCurrentDistrict());
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.HEALTH_FACILITIES, fieldsMap.get(JsonForm.HEALTH_FACILITY),
                        PreferencesUtil.getInstance().getCurrentDistrict());
                break;

            case JsonForm.DAILY_SUMMARY_ZAMBIA:
            case JsonForm.DAILY_SUMMARY_SENEGAL:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.TEAM_LEADERS, fieldsMap.get(JsonForm.TEAM_LEADER),
                        PreferencesUtil.getInstance().getCurrentDistrict());
                String dataCollector = RevealApplication.getInstance().getContext().allSharedPreferences().fetchRegisteredANM();
                if (StringUtils.isNotBlank(dataCollector)) {
                    populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                            CONFIGURATION.SPRAY_OPERATORS, fieldsMap.get(JsonForm.SPRAY_OPERATOR_CODE),
                            dataCollector);
                }

                if(Country.SENEGAL.equals(BuildConfig.BUILD_COUNTRY)){
                    populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                            CONFIGURATION.DISTRICT_MANAGERS, fieldsMap.get(JsonForm.DISTRICT_MANAGER),
                            PreferencesUtil.getInstance().getCurrentDistrict());
                    populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                            CONFIGURATION.SUPERVISORS, fieldsMap.get(JsonForm.SUPERVISOR),
                            PreferencesUtil.getInstance().getCurrentDistrict());
                }

                if (isZambiaIRSLite()) {
                    populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(OPERATIONAL_AREA));
                } else if (MACEPA_PROVINCES.contains(PreferencesUtil.getInstance().getCurrentProvince())) {
                    populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(HEALTH_CENTER));
                } else {
                    if(!Country.SENEGAL.equals(BuildConfig.BUILD_COUNTRY))
                        populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(OPERATIONAL_AREA, ZONE));
                }
                break;

            case JsonForm.TEAM_LEADER_DOS_ZAMBIA:
            case JsonForm.TEAM_LEADER_DOS_SENEGAL:

                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.DATA_COLLECTORS, fieldsMap.get(JsonForm.DATA_COLLECTOR),
                        PreferencesUtil.getInstance().getCurrentDistrict());

                dataCollector = JsonFormUtils.getString(fieldsMap.get(JsonForm.DATA_COLLECTOR), VALUE);
                if (StringUtils.isNotBlank(dataCollector)) {
                    populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                            CONFIGURATION.SPRAY_OPERATORS, fieldsMap.get(JsonForm.SPRAY_OPERATOR_CODE),
                            dataCollector.split(":")[0]);
                }

                if (isZambiaIRSLite()) {
                    populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(OPERATIONAL_AREA));
                } else if (MACEPA_PROVINCES.contains(PreferencesUtil.getInstance().getCurrentProvince())) {
                    populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(HEALTH_CENTER));
                } else {
                    populateUserAssignedLocations(formJSON, JsonForm.ZONE, Arrays.asList(OPERATIONAL_AREA, ZONE));
                }

                break;

            case JsonForm.VERIFICATION_FORM_ZAMBIA:
            case JsonForm.VERIFICATION_FORM_SENEGAL:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.FIELD_OFFICERS, fieldsMap.get(JsonForm.FIELD_OFFICER),
                        PreferencesUtil.getInstance().getCurrentDistrict());

            case JsonForm.SPRAY_FORM_ZAMBIA:
            case JsonForm.SPRAY_FORM_SENEGAL:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        Constants.CONFIGURATION.DATA_COLLECTORS, fieldsMap.get(JsonForm.DATA_COLLECTOR),
                        PreferencesUtil.getInstance().getCurrentDistrict());

                dataCollector = RevealApplication.getInstance().getContext().allSharedPreferences().fetchRegisteredANM();
                if (StringUtils.isNotBlank(dataCollector)) {
                    populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                            CONFIGURATION.SPRAY_OPERATORS, fieldsMap.get(JsonForm.SPRAY_OPERATOR_CODE),
                            dataCollector);
                }
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.DISTRICTS, fieldsMap.get(JsonForm.DISTRICT), PreferencesUtil.getInstance().getCurrentProvince());
                break;

            case JsonForm.TABLET_ACCOUNTABILITY_FORM:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.HEALTH_WORKER_SUPERVISORS, fieldsMap.get(JsonForm.HEALTH_WORKER_SUPERVISOR),
                        PreferencesUtil.getInstance().getCurrentOperationalArea());
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.COMMUNITY_DRUG_DISTRIBUTORS, fieldsMap.get(JsonForm.COMMUNITY_DRUG_DISTRIBUTOR_NAME),
                        PreferencesUtil.getInstance().getCurrentOperationalArea());
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(), CONFIGURATION.WARDS, fieldsMap.get(JsonForm.LOCATION), PreferencesUtil.getInstance().getCurrentOperationalArea());
                break;
            case JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM:
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.HEALTH_WORKER_SUPERVISORS, fieldsMap.get(JsonForm.HEALTH_WORKER_SUPERVISOR),
                        PreferencesUtil.getInstance().getCurrentOperationalArea());
                populateServerOptions(RevealApplication.getInstance().getServerConfigs(),
                        CONFIGURATION.COMMUNITY_DRUG_DISTRIBUTORS, fieldsMap.get(JsonForm.COMMUNITY_DRUG_DISTRIBUTOR_NAME),
                        PreferencesUtil.getInstance().getCurrentOperationalArea());
            default:
                break;
        }
    }

    private void populateUserAssignedLocations(JSONObject formJSON, String fieldKey, List<String> allowedTags) {
        JSONArray options = new JSONArray();
        List<String> defaultLocationHierarchy = locationHelper.generateDefaultLocationHierarchy(allowedTags);
        if (defaultLocationHierarchy == null) {
            return;
        }
        defaultLocationHierarchy.stream().forEach(options::put);
        JSONObject field = JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formJSON), fieldKey);

        try {
            field.put(KEYS, options);
            field.put(VALUES, options);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public void populateCompoundStructureOptions(JSONObject form, Location currentOperationalArea){
        SQLiteDatabase database = RevealApplication.getInstance().getRepository().getReadableDatabase();
        JSONObject option;
        JSONObject property;
        JSONArray options = new JSONArray();
        String locationId  = currentOperationalArea.getId();
        String query = String.format("SELECT %s,%s FROM %s WHERE %s IS NOT NULL AND %s IN (SELECT %s FROM %s WHERE %s = ? ) ORDER BY %s DESC",Constants.DatabaseKeys.ID,Constants.DatabaseKeys.COMPOUND_HEAD_NAME,Constants.Tables.SPRAYED_STRUCTURES,Constants.DatabaseKeys.COMPOUND_HEAD_NAME,Constants.DatabaseKeys.BASE_ENTITY_ID,Constants.DatabaseKeys.ID_,Constants.DatabaseKeys.STRUCTURES_TABLE,Constants.DatabaseKeys.PARENT_ID,Constants.DatabaseKeys.SPRAY_DATE);
        try(Cursor cursor = database.rawQuery(query,new String[]{locationId})){
            while (cursor.moveToNext()) {
                property = new JSONObject();
                property.put("presumed-id","err");
                property.put("confirmed-id","err");

                String structureId = cursor.getString(cursor.getColumnIndex(Constants.DatabaseKeys.ID));
                String compoundHeadName = cursor.getString(cursor.getColumnIndex(Constants.DatabaseKeys.COMPOUND_HEAD_NAME));

                option = new JSONObject();
                option.put(KEY,structureId);
                option.put(TEXT,compoundHeadName);
                option.put(JsonFormConstants.MultiSelectUtils.PROPERTY,property);
                options.put(option);
            }
        } catch (Exception e) {
            Timber.e(e, "Error find Sprayed Structures with compound head names ");
        }
        JSONObject compoundStructureField = JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(form),COMPOUND_STRUCTURE);
        try {
            compoundStructureField.put(OPTIONS, options);
        } catch (JSONException e) {
            Timber.e(e, "Error populating %s Options",COMPOUND_STRUCTURE);
        }
    }
}
