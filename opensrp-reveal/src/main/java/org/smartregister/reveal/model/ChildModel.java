package org.smartregister.reveal.model;

import android.content.Context;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.repository.TaskRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.ChildRegisterFragmentContract;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.util.QueryComposer;
import org.smartregister.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ChildModel extends AbstractDao implements ChildRegisterFragmentContract.Model {

    private RevealJsonFormUtils revealJsonFormUtils = new RevealJsonFormUtils();

    @Override
    public List<Child> searchAndFilter(@Nullable HashMap<String, List<String>> sortAndFilter, @Nullable String searchText) {
        QueryComposer composer = getDefaultComposer();
        if (StringUtils.isNotBlank(searchText)) {
            String[] searchTexts = searchText.split(" ");
            for (String search : searchTexts) {
                String text = search.trim();

                String searchPhrase = " LIKE '%" + text + "%'";

                QueryComposer.Disjoint disjoint = new QueryComposer.Disjoint();
                disjoint.addDisjointClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.FIRST_NAME + searchPhrase);
                disjoint.addDisjointClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.LAST_NAME + searchPhrase);
                disjoint.addDisjointClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.UNIQUE_ID + searchPhrase);
                disjoint.addDisjointClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.MIDDLE_NAME + searchPhrase);

                composer.withWhereClause(disjoint);
            }
        }

        String currentArea = getCurrentLocationID();
        if (StringUtils.isNotBlank(currentArea))
            composer.withWhereClause(Constants.DatabaseKeys.LOCATION + " = '" + currentArea + "'");


        extractSort(composer, sortAndFilter);
        extractFilter(composer, sortAndFilter);

        try {
            return AbstractDao.readData(composer.generateQuery(), getChildDataMap());
        } catch (QueryComposer.InvalidQueryException e) {
            Timber.e(e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getCurrentLocationID() {
        Location location = org.smartregister.reveal.util.Utils.getStructureByName(PreferencesUtil.getInstance().getCurrentStructure());
        return location == null ? "" : location.getId();
    }

    @Override
    public Map<String, Integer> getReportCounts() {
        Map<String, Integer> result = new HashMap<>();

        int registeredChildren = getTotal("select count(*) cnt from ec_child where DATE(dob) > DATE('now','-19 years') and is_closed = 0 and location = '" + getCurrentLocationID() + "'");

        int administeredDrugs = getTotal("select count(*) cnt from ec_child where DATE(dob) > DATE('now','-19 years') and is_closed = 0 and location = '" + getCurrentLocationID() + "' " +
                "and ec_child.base_entity_id in (select t.for from task  t where t.code = '" + Constants.Intervention.MDA_DISPENSE + "' and t.business_status like '%" + Constants.BusinessStatus.VISITED_DRUG_ADMINISTERED + "%') ");

        int childrenNotVisited = getTotal("select count(*) cnt from ec_child where DATE(dob) > DATE('now','-19 years') and is_closed = 0 and location = '" + getCurrentLocationID() + "' " +
                "and ec_child.base_entity_id in (select t.for from task  t where t.code = '" + Constants.Intervention.MDA_DISPENSE + "' and t.business_status like '%" + Constants.BusinessStatus.NOT_VISITED + "%') ");

        int visitedNotAdministered = getTotal("select count(*) cnt from ec_child where DATE(dob) > DATE('now','-19 years') and is_closed = 0 and location = '" + getCurrentLocationID() + "' " +
                "and ec_child.base_entity_id in (select t.for from task  t where t.code = '" + Constants.Intervention.MDA_DISPENSE + "' and t.business_status like '%" + Constants.BusinessStatus.VISITED_DRUG_NOT_ADMINISTERED + "%') ");

        result.put(Constants.ChildRegister.MMA_COVERAGE, registeredChildren == 0 ? 0 : (int) Math.round(((administeredDrugs * 100.0) / registeredChildren)));
        result.put(Constants.ChildRegister.MMA_TARGET_REMAINING, (int) Math.round(((registeredChildren * 0.9) - administeredDrugs)));
        result.put(Constants.ChildRegister.MMA_CHILDREN_REGISTERED, registeredChildren);
        result.put(Constants.ChildRegister.MMA_VISITED_AND_ADMINISTERED, administeredDrugs);
        result.put(Constants.ChildRegister.MMA_NOT_VISITED, childrenNotVisited);
        result.put(Constants.ChildRegister.MMA_VISITED_NOT_ADMINISTERED, visitedNotAdministered);

        return result;
    }

    private int getTotal(String sql) {
        DataMap<Integer> countMap = cursor -> getCursorIntValue(cursor, "cnt");
        Integer registeredChildren = AbstractDao.readSingleValue(sql, countMap);
        return registeredChildren == null ? 0 : registeredChildren;
    }

    @Override
    public JSONObject getMDAForm(Context context, String baseEntityID) throws JSONException {
        // read form and inject base id
        String jsonForm = readAssetContents(context, Constants.JsonForm.NTD_MASS_DRUG_ADMINISTRATION);
        JSONObject jsonObject = new JSONObject(jsonForm);
        jsonObject.put(Constants.Properties.BASE_ENTITY_ID, baseEntityID);
        return jsonObject;
    }

    @Override
    public JSONObject getRegistrationForm(Context context) throws JSONException {
        String jsonForm = readAssetContents(context, Constants.JsonForm.NTD_CHILD_REGISTRATION);
        JSONObject jsonObject = new JSONObject(jsonForm);

        // inject unique id
        String uniqueID = getUniqueIdRepository().getNextUniqueId().getOpenmrsId();
        if (StringUtils.isBlank(uniqueID))
            throw new IllegalStateException("No local unique ID");

        revealJsonFormUtils.populateField(jsonObject, Constants.DatabaseKeys.UNIQUE_ID, uniqueID, JsonFormConstants.VALUE);
        return jsonObject;
    }

    public UniqueIdRepository getUniqueIdRepository() {
        return RevealApplication.getInstance().getContext().getUniqueIdRepository();
    }

    public String readAssetContents(Context context, String path) {
        return Utils.readAssetContents(context, path);
    }

    @Override
    public Task getCurrentTask(Context context, String baseEntityID) {
        String taskSQL = "select _id from task where for = '" + baseEntityID + "' and code = '" + Constants.Intervention.MDA_DISPENSE + "' order by  authored_on desc limit 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "_id");

        String taskId = AbstractDao.readSingleValue(taskSQL, dataMap);
        return getTaskRepository().getTaskByIdentifier(taskId);
    }

    public TaskRepository getTaskRepository() {
        return RevealApplication.getInstance().getContext().getTaskRepository();
    }

    private void extractSort(QueryComposer composer, @Nullable HashMap<String, List<String>> sortAndFilter) {
        if (sortAndFilter != null) {
            List<String> params = sortAndFilter.get(Constants.ChildFilter.SORT);
            if (params != null && params.size() > 0) {
                for (String s : params) {
                    composer.withSortColumn(s);
                }
            }
        } else {
            composer.withSortColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE + " ASC");
            composer.withSortColumn(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.LAST_NAME + " DESC");
        }
    }

    private void extractFilter(QueryComposer composer, @Nullable HashMap<String, List<String>> sortAndFilter) {
        if (sortAndFilter != null) {
            List<String> paramsGrade = sortAndFilter.get(Constants.ChildFilter.FILTER_GRADE);
            if (paramsGrade != null && paramsGrade.size() > 0) {
                composer.withWhereClause(Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.GRADE + " in ( " +
                        toCSV(paramsGrade) + ")");
            }

            List<String> paramsAge = sortAndFilter.get(Constants.ChildFilter.FILTER_AGE);
            if (paramsAge != null && paramsAge.size() > 0) {

                String agePhrase = " cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', " + Constants.DatabaseKeys.CHILD_TABLE + "." + Constants.DatabaseKeys.DOB + ") as int) ";

                QueryComposer.Disjoint disjoint = new QueryComposer.Disjoint();

                for (String param : paramsAge) {
                    if (param.contains(":")) {
                        String[] bounds = param.split(":");
                        disjoint.addDisjointClause(agePhrase + " between " + bounds[0] + " and " + bounds[1]);

                    } else if (param.equalsIgnoreCase("Adult")) {
                        disjoint.addDisjointClause(agePhrase + " > 18 ");
                    }
                }

                composer.withWhereClause(disjoint);
            }
        }
    }

    private String toCSV(List<String> results) {
        StringBuilder builder = new StringBuilder();
        int size = results.size();
        while (size > 0) {

            builder.append("'").append(results.get(size - 1)).append("'");

            if (size > 1)
                builder.append(" , ");
            size--;
        }
        return builder.toString();
    }

    private QueryComposer getDefaultComposer() {
        return new QueryComposer()
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
    }

    private DataMap<Child> getChildDataMap() {
        return cursor -> {
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
            if (child.getGrade() == null)
                child.setGrade("");

            return child;
        };
    }
}
