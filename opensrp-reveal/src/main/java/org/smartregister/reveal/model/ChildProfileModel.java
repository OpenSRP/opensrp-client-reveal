package org.smartregister.reveal.model;

import android.content.Context;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.PlanDefinition;
import org.smartregister.domain.Task;
import org.smartregister.repository.PlanDefinitionRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.dao.EventDao;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.NativeFormProcessorHelper;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.util.JsonFormUtils;
import org.smartregister.util.NativeFormProcessor;
import org.smartregister.util.QueryComposer;
import org.smartregister.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;

public class ChildProfileModel extends AbstractDao implements ChildProfileContract.Model {

    private EventDao eventDao;
    private PreferencesUtil prefsUtil;

    @Nullable
    @Override
    public Child getChild(String baseEntityID) throws QueryComposer.InvalidQueryException {
        QueryComposer composer = new QueryComposer()
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.BASE_ENTITY_ID)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.FIRST_NAME)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.LAST_NAME)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.DOB)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.MIDDLE_NAME)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GENDER)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE)
                .withColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.UNIQUE_ID)
                .withColumn(
                        "(select business_status from " + Constants.DatabaseKeys.TASK_TABLE + " where for = " +
                                Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.BASE_ENTITY_ID + " and code = '" +
                                Constants.Intervention.MDA_DISPENSE + "' order by authored_on desc limit 1) as " + Constants.DatabaseKeys.STATUS
                )

                .withMainTable(Constants.DatabaseKeys.CHILD_TABLE);

        composer.withWhereClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.BASE_ENTITY_ID + " = '" + baseEntityID + "'");

        DataMap<Child> dataMap = cursor -> {
            Child child = new Child();
            child.setBaseEntityID(getCursorValue(cursor, Constants.DatabaseKeys.BASE_ENTITY_ID));
            child.setFirstName(getCursorValue(cursor, Constants.DatabaseKeys.FIRST_NAME));
            child.setLastName(getCursorValue(cursor, Constants.DatabaseKeys.LAST_NAME));
            child.setBirthDate(getCursorValueAsDate(cursor, Constants.DatabaseKeys.DOB, getDobDateFormat()));
            child.setMiddleName(getCursorValue(cursor, Constants.DatabaseKeys.MIDDLE_NAME));
            child.setGender(getCursorValue(cursor, Constants.DatabaseKeys.GENDER));
            child.setGrade(getCursorValue(cursor, Constants.DatabaseKeys.GRADE));
            child.setUniqueID(getCursorValue(cursor, Constants.DatabaseKeys.UNIQUE_ID));
            child.setTaskStatus(getCursorValue(cursor, Constants.DatabaseKeys.STATUS));
            return child;
        };

        return AbstractDao.readSingleValue(composer.generateQuery(), dataMap);
    }

    @Override
    public Task getCurrentTask(Context context, String baseEntityID) {
        String taskSQL = "select _id from task where for = '" + baseEntityID + "' and code = '" + Constants.Intervention.MDA_DISPENSE + "' order by  authored_on desc limit 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "_id");

        String taskId = AbstractDao.readSingleValue(taskSQL, dataMap);
        return getTaskRepository().getTaskByIdentifier(taskId);
    }

    public TaskRepository getTaskRepository() {
        return CoreLibrary.getInstance().context().getTaskRepository();
    }

    @Override
    public JSONObject getRegistrationEditForm(Context context, String baseEntityID) throws Exception {
        String jsonForm = readAssetContents(context, Constants.JsonForm.NTD_CHILD_REGISTRATION);
        JSONObject jsonObject = new JSONObject(jsonForm);
        jsonObject.put(Constants.Properties.BASE_ENTITY_ID, baseEntityID);
        jsonObject.put(ENCOUNTER_TYPE, Constants.EventType.UPDATE_CHILD_REGISTRATION);

        String sql = "select json from client where baseEntityId = '" + baseEntityID + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "json");
        String clientString = AbstractDao.readSingleValue(sql, dataMap);
        JSONObject clientJson = new JSONObject(clientString);

        // get key for values
        Map<String, Object> values = new HashMap<>();
        values.put("unique_id", getFormValue(clientJson.getJSONObject("identifiers"), "opensrp_id"));
        values.put("sactaNationalId", getFormValue(clientJson.getJSONObject("identifiers"), "national_id"));
        values.put("sactaRevealId", getFormValue(clientJson.getJSONObject("identifiers"), "reveal_id"));
        values.put("sactaGivenName", getFormValue(clientJson, "firstName"));
        values.put("sactaSurname", getFormValue(clientJson, "lastName"));
        values.put("sactaSex", getFormValue(clientJson, "gender"));
        values.put("sactaDob", convertToFormsDate(getFormValue(clientJson, "birthdate")));
        values.put("sactaDobUnk", getFormValue(clientJson, "birthdateApprox"));
        values.put("sactaCurrEnroll", getFormValue(clientJson.getJSONObject("attributes"), "school_enrolled"));
        values.put("sactaCurrSchName", getFormValue(clientJson.getJSONObject("attributes"), "school_name"));
        values.put("sactaGrade", getFormValue(clientJson.getJSONObject("attributes"), "grade"));
        values.put("sactaClass", getFormValue(clientJson.getJSONObject("attributes"), "grade_class"));


        NativeFormProcessorHelper.createInstance(jsonObject)
                .populateValues(values);

        String noID = getFormValue(clientJson.getJSONObject("attributes"), "has_no_id").toString();
        if (StringUtils.isNotBlank(noID)) {
            JSONObject field = JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(jsonObject), "sactaNoNationalId");
            field.getJSONArray("options").getJSONObject(0).put(JsonFormConstants.VALUE, true);
        }

        // sort out the check boxes
        return jsonObject;
    }

    public String readAssetContents(Context context, String path) {
        return Utils.readAssetContents(context, path);
    }

    @Override
    public JSONObject getEditMDAForm(Context context, String baseEntityID) throws Exception {
        String jsonForm = readAssetContents(context, Constants.JsonForm.NTD_MASS_DRUG_ADMINISTRATION);
        JSONObject jsonObject = new JSONObject(jsonForm);
        jsonObject.put(Constants.Properties.BASE_ENTITY_ID, baseEntityID);

        NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonObject);
        JSONObject processedForm = getEventDao().getLastEvent(baseEntityID, Constants.EventType.MDA_DISPENSE);

        if (processedForm != null) {
            try {
                if (eventIsWithinPlan(processedForm)) {
                    Map<String, Object> values = processor.getFormResults(processedForm);
                    processor.populateValues(values);
                }
            } catch (Exception exception) {
                Timber.e(exception);

                Map<String, Object> values = processor.getFormResults(processedForm);
                processor.populateValues(values);
            }
        }

        return jsonObject;
    }

    private EventDao getEventDao() {
        if (eventDao == null)
            eventDao = EventDao.getInstance();

        return eventDao;
    }

    public PreferencesUtil getPrefsUtil() {
        if (prefsUtil == null)
            prefsUtil = PreferencesUtil.getInstance();

        return prefsUtil;
    }

    public boolean eventIsWithinPlan(JSONObject processedForm) throws JSONException, ParseException {
        PlanDefinitionRepository planDefinitionRepository = RevealApplication.getInstance().getPlanDefinitionRepository();
        PlanDefinition planDefinition = planDefinitionRepository.findPlanDefinitionById(getPrefsUtil().getCurrentPlanId());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date eventDate = sdf.parse(processedForm.getString("eventDate").substring(0, 10));
        return planDefinition.getEffectivePeriod().getStart().toDate().getTime() <= eventDate.getTime();
    }

    private String convertToFormsDate(Object _value) throws ParseException {
        String value = _value.toString();

        if (StringUtils.isBlank(value))
            return "";

        Date original = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(value);
        return new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(original);
    }

    private Object getFormValue(JSONObject jsonObject, String key) throws JSONException {
        if (jsonObject.has(key))
            return jsonObject.getString(key);

        return "";
    }

    @Override
    public JSONObject getADRForm(Context context, String baseEntityID) throws JSONException {
        String jsonForm = Utils.readAssetContents(context, Constants.JsonForm.NTD_DRUG_ADVERSE_REACTION);
        JSONObject jsonObject = new JSONObject(jsonForm);
        jsonObject.put(Constants.Properties.BASE_ENTITY_ID, baseEntityID);

        NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonObject);
        JSONObject processedForm = getEventDao().getLastEvent(baseEntityID, Constants.EventType.MDA_ADVERSE_DRUG_REACTION);

        if (processedForm != null) {
            try {
                if (eventIsWithinPlan(processedForm)) {
                    Map<String, Object> values = processor.getFormResults(processedForm);
                    processor.populateValues(values);
                }
            } catch (Exception exception) {
                Timber.e(exception);

                Map<String, Object> values = processor.getFormResults(processedForm);
                processor.populateValues(values);
            }
        }

        return jsonObject;
    }
}
