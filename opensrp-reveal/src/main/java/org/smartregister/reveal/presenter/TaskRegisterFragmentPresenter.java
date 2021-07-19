package org.smartregister.reveal.presenter;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.common.annotations.VisibleForTesting;
import com.mapbox.geojson.Feature;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.configurableviews.helper.ConfigurableViewsHelper;
import org.smartregister.configurableviews.model.View;
import org.smartregister.configurableviews.model.ViewConfiguration;
import org.smartregister.domain.Event;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.BaseFormFragmentContract;
import org.smartregister.reveal.contract.TaskRegisterFragmentContract;
import org.smartregister.reveal.interactor.BaseFormFragmentInteractor;
import org.smartregister.reveal.interactor.TaskRegisterFragmentInteractor;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import timber.log.Timber;

import static org.smartregister.domain.Task.INACTIVE_TASK_STATUS;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;

/**
 * Created by samuelgithengi on 3/11/19.
 */
public class TaskRegisterFragmentPresenter extends BaseFormFragmentPresenter implements TaskRegisterFragmentContract.Presenter {

    private WeakReference<TaskRegisterFragmentContract.View> view;

    private String viewConfigurationIdentifier;

    private ConfigurableViewsHelper viewsHelper;

    private Set<View> visibleColumns;

    private TaskRegisterFragmentInteractor interactor;

    private List<TaskDetails> tasks;
    private android.location.Location lastLocation;

    private boolean recalculateDistance;

    private PreferencesUtil prefsUtil;

    private boolean isActionClicked = true;

    private TaskFilterParams filterParams;

    private boolean isTasksFiltered;

    private ArrayList<TaskDetails> filteredTasks;

    private int withinBuffer;

    private boolean applyFilterOnTasksFound;

    private BaseFormFragmentContract.Interactor formInteractor;

    public TaskRegisterFragmentPresenter(TaskRegisterFragmentContract.View view, String viewConfigurationIdentifier) {
        this(view, viewConfigurationIdentifier, null);
        this.interactor = new TaskRegisterFragmentInteractor(this);
        formInteractor = new BaseFormFragmentInteractor(this);
    }

    @VisibleForTesting
    protected TaskRegisterFragmentPresenter(TaskRegisterFragmentContract.View view, String viewConfigurationIdentifier,
                                            TaskRegisterFragmentInteractor interactor) {
        super(view, view.getContext());
        this.view = new WeakReference<>(view);
        this.viewConfigurationIdentifier = viewConfigurationIdentifier;
        this.interactor = interactor;
        viewsHelper = ConfigurableViewsLibrary.getInstance().getConfigurableViewsHelper();
        prefsUtil = PreferencesUtil.getInstance();


    }


    @Override
    public void processViewConfigurations() {
        if (!StringUtils.isBlank(this.viewConfigurationIdentifier)) {
            ViewConfiguration viewConfiguration = viewsHelper.getViewConfiguration(this.viewConfigurationIdentifier);
            if (viewConfiguration != null) {
                visibleColumns = viewsHelper.getRegisterActiveColumns(this.viewConfigurationIdentifier);
            }
        }
    }

    @Override
    public void initializeQueries(String mainCondition) {

        if (getView().getAdapter() == null) {
            getView().initializeAdapter(visibleColumns);
        }
        lastLocation = getView().getLocationUtils().getLastLocation();
        if (lastLocation == null) {//if location client has not initialized use last location passed from map
            lastLocation = getView().getLastLocation();
        }

        if (!isTasksFiltered) {
            getView().showProgressView();
            interactor.findTasks(getMainCondition(), lastLocation, getOperationalAreaCenter(), getView().getContext().getString(R.string.house));
        }

    }

    private android.location.Location getOperationalAreaCenter() {
        Location operationalAreaLocation = Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea());
        if (operationalAreaLocation == null)
            return null;
        return mappingHelper.getCenter(gson.toJson(operationalAreaLocation.getGeometry()));
    }

    @Override
    public void startSync() {
        Utils.startImmediateSync();
    }

    @Override
    public void searchGlobally(String uniqueId) {//do nothing, tasks not searchable globally
    }

    /**
     * Gets the where clause for the task register, filters by operational area and campaign
     *
     * @return pair of filter clause and values for filter
     */
    private Pair<String, String[]> getMainCondition() {
        Location operationalArea = Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea());
        String whereClause = String.format("%s.%s = ? AND %s.%s = ? AND %s.%s NOT IN (%s)",
                Constants.DatabaseKeys.TASK_TABLE, Constants.DatabaseKeys.GROUPID, Constants.DatabaseKeys.TASK_TABLE, Constants.DatabaseKeys.PLAN_ID,
                Constants.DatabaseKeys.TASK_TABLE, Constants.DatabaseKeys.STATUS,
                TextUtils.join(",", Collections.nCopies(INACTIVE_TASK_STATUS.length, "?")));
        return new Pair<>(whereClause, ArrayUtils.addAll(new String[]{operationalArea == null ?
                null : operationalArea.getId(), prefsUtil.getCurrentPlanId()}, INACTIVE_TASK_STATUS));
    }

    @Override
    protected TaskRegisterFragmentContract.View getView() {
        return view.get();
    }

    @Override
    public void onTasksFound(List<TaskDetails> tasks, int structuresWithinBuffer) {
        if (recalculateDistance) {//there was a location update when tasks were being retrieved recalculate distance and order
            interactor.calculateDistanceFromUser(tasks, lastLocation);
            recalculateDistance = false;
        } else {
            this.tasks = tasks;
            if (tasks == null) {
                getView().displayNotification(R.string.fetching_structure_title,
                        R.string.fetch_location_and_structures_failed, prefsUtil.getCurrentOperationalArea());
                getView().setTaskDetails(new ArrayList<>());
            } else if (tasks.isEmpty()) {
                getView().displayNotification(R.string.fetching_structure_title, R.string.no_structures_found);
                getView().setTaskDetails(tasks);
            } else if (applyFilterOnTasksFound) {
                filterTasks(filterParams);
                getView().setSearchPhrase(filterParams.getSearchPhrase());
                applyFilterOnTasksFound = false;
            } else {
                getView().setTaskDetails(tasks);
            }
            getView().setTotalTasks(structuresWithinBuffer);
            getView().hideProgressDialog();
            getView().hideProgressView();
        }

    }


    @Override
    public void onLocationChanged(android.location.Location location) {
        if (!location.equals(lastLocation)) {
            if (lastLocation == null && tasks == null) {//tasks not yet retrieved from db
                recalculateDistance = true;
            } else if (lastLocation == null ||
                    location.distanceTo(lastLocation) >= Constants.REFRESH_MAP_MINIMUM_DISTANCE) {
                interactor.calculateDistanceFromUser(tasks, location);
            }
            lastLocation = location;
        }

    }

    @Override
    public void onDestroy() {
        getView().getLocationUtils().stopLocationClient();
    }

    @Override
    public void onDrawerClosed() {
        getView().showProgressDialog(R.string.fetching_structures_title, R.string.fetching_structures_message);
        interactor.findTasks(getMainCondition(), lastLocation, getOperationalAreaCenter(), getView().getContext().getString(R.string.house));
        getView().setInventionType(getInterventionLabel());
    }

    @Override
    public void onTaskSelected(TaskDetails details, boolean isActionClicked) {
        this.isActionClicked = isActionClicked;
        if (details != null) {
            setTaskDetails(details);
            boolean hasSingleGroupedTask = (BEDNET_DISTRIBUTION.equals(details.getTaskCode()) || BLOOD_SCREENING.equals(details.getTaskCode())) && details.getTaskCount() == 1;
            if (CASE_CONFIRMATION.equals(details.getTaskCode())) {
                interactor.getIndexCaseDetails(details.getStructureId(),
                        Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea()).getId(), details.getReasonReference());
            } else if(Task.TaskStatus.COMPLETED.name().equals(details.getTaskStatus())
                    &&
                    (BLOOD_SCREENING.equals(details.getTaskCode()) ||
                            BEDNET_DISTRIBUTION.equals(details.getTaskCode()) ||
                            REGISTER_FAMILY.equals(details.getTaskCode())) ||
                    hasSingleGroupedTask ||
                    (details.hasRegisteredFamily() && details.getTaskCount() != null && details.getTaskCount() > 1 // structures with grouped tasks should display the family profile
                            && !(REGISTER_FAMILY.equals(details.getTaskCode()) && Task.TaskStatus.READY.name().equals(details.getTaskStatus())))) { // skip if we have a READY family reg task
                setTaskDetails(details);
                interactor.fetchFamilyDetails(details.getStructureId());
            } else {
                if (details.getPendingTasksCount() > 1) {
                    getView().openTasksScreen(getTaskDetails());
                } else {
                    getView().showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
                    interactor.getStructure(details);
                }
            }
        }
    }

    @Override
    public int getInterventionLabel() {
        return Utils.getInterventionLabel();
    }

    /**
     * Called by interactor when the index event has been queried. If Event is not found an errror is displayed.
     * If task confirmation  is not competed and event was linked to a household and button was selected, then family profile is opened,
     * otherwise the index case details are displayed
     *
     * @param indexCase              the index case details event JSON
     * @param isLinkedToJurisdiction if index case was linked to FI, false if linked to structure
     */
    @Override
    public void onIndexCaseFound(JSONObject indexCase, boolean isLinkedToJurisdiction) {
        if (indexCase == null) {
            getView().displayError(R.string.classification_details, R.string.index_case_not_found);
        } else {
            if (isActionClicked && !isLinkedToJurisdiction
                    && getTaskDetails().getTaskStatus().equals(Task.TaskStatus.READY.name())) {
                interactor.fetchFamilyDetails(getTaskDetails().getStructureId());
            } else {
                getView().displayIndexCaseDetails(indexCase);
            }
        }
    }

    @Override
    public void searchTasks(String searchText) {
        Timber.d("searching task matching %s", searchText);
        if (StringUtils.isBlank(searchText)) {
            setTasks(getActiveTasks(), this.withinBuffer);
        } else {
            List<TaskDetails> filteredTasks = new ArrayList<>();
            int withinBuffer = 0;
            for (TaskDetails task : getActiveTasks()) {
                if (Utils.matchesSearchPhrase(task.getFamilyName(), searchText) ||
                        Utils.matchesSearchPhrase(task.getStructureName(), searchText) ||
                        Utils.matchesSearchPhrase(task.getHouseNumber(), searchText) ||
                        Utils.matchesSearchPhrase(task.getFamilyMemberNames(), searchText)) {
                    filteredTasks.add(task);
                    if (task.getDistanceFromUser() > 0 && task.getDistanceFromUser() <= Utils.getLocationBuffer())
                        withinBuffer++;
                }
            }
            setTasks(filteredTasks, withinBuffer);
        }
    }

    private void setTasks(List<TaskDetails> filteredTasks, int withinBuffer) {
        getView().setTaskDetails(filteredTasks);
        getView().setTotalTasks(withinBuffer);
    }

    @Override
    public void filterTasks(TaskFilterParams filterParams) {
        this.filterParams = filterParams;
        if (filterParams.getCheckedFilters() == null || filterParams.getCheckedFilters().isEmpty()) {
            applyEmptyFilter();
            return;
        }
        filteredTasks = new ArrayList<>();
        Set<String> filterStatus = filterParams.getCheckedFilters().get(Constants.Filter.STATUS);
        Set<String> filterTaskCode = filterParams.getCheckedFilters().get(Constants.Filter.CODE);
        Set<String> filterInterventionUnitTasks = Utils.getInterventionUnitCodes(filterParams.getCheckedFilters().get(Constants.Filter.INTERVENTION_UNIT));
        getView().setNumberOfFilters(filterParams.getCheckedFilters().size());
        Pattern pattern = Pattern.compile("~");
        withinBuffer = 0;
        for (TaskDetails taskDetails : tasks) {
            if (matchesTask(taskDetails, pattern, filterStatus, filterTaskCode, filterInterventionUnitTasks)) {
                filteredTasks.add(taskDetails);
                if (taskDetails.getDistanceFromUser() > 0 && taskDetails.getDistanceFromUser() <= Utils.getLocationBuffer())
                    withinBuffer++;
            }
        }
        if (StringUtils.isNotBlank(filterParams.getSortBy())) {
            sortTasks(filteredTasks, filterParams.getSortBy());
        }
        setTasks(filteredTasks, withinBuffer);
        isTasksFiltered = true;
        getView().setSearchPhrase("");
        getView().hideProgressDialog();
        getView().hideProgressView();
    }

    private void applyEmptyFilter() {
        isTasksFiltered = false;
        getView().clearFilter();
        filteredTasks = null;
        if (StringUtils.isNotBlank(filterParams.getSortBy())) {
            sortTasks(tasks, filterParams.getSortBy());
            getView().setTaskDetails(tasks);
        }
    }

    private boolean matchesTask(TaskDetails taskDetails, Pattern pattern, Set<String> filterStatus, Set<String> filterTaskCode, Set<String> filterInterventionUnitTasks) {
        boolean matches = true;
        if (filterStatus != null) {
            matches = StringUtils.isBlank(taskDetails.getAggregateBusinessStatus()) ? filterStatus.contains(taskDetails.getBusinessStatus()) : filterStatus.contains(taskDetails.getAggregateBusinessStatus());
        }
        if (matches && filterTaskCode != null) {
            if (taskDetails.getTaskCount() == null || taskDetails.getTaskCount() < 2) {
                matches = matchesTaskCodeFilterList(taskDetails.getTaskCode(), filterTaskCode, pattern);
            } else {
                matches = matchesTaskCodeFilterList(taskDetails.getGroupedTaskCodes(), filterTaskCode, pattern);
            }
        }
        if (matches && filterInterventionUnitTasks != null) {
            matches = matchesTaskCodeFilterList(taskDetails.getTaskCode(), filterInterventionUnitTasks, pattern);
        }
        return matches;
    }

    private void sortTasks(List<TaskDetails> filteredTasks, String sortBy) {
        int sortType = Arrays.asList(getView().getContext().getResources().getStringArray(R.array.task_sort_options)).indexOf(sortBy);
        if (sortType == 0) {// sort by distance default sort
            Collections.sort(filteredTasks);
        } else if (sortType == 1) {// sort by business status
            Collections.sort(filteredTasks, (task, task2) -> {
                String status = StringUtils.isBlank(task.getAggregateBusinessStatus()) ? task.getBusinessStatus() : task.getAggregateBusinessStatus();
                String status2 = StringUtils.isBlank(task2.getAggregateBusinessStatus()) ? task2.getBusinessStatus() : task2.getAggregateBusinessStatus();
                return status.compareTo(status2);
            });
        } else if (sortType == 2) {// sort by task type
            Collections.sort(filteredTasks, (task, task2) -> task.getTaskCode().compareTo(task2.getTaskCode()));
        }
    }

    @Override
    public void onFilterTasksClicked() {
        getView().openFilterActivity(filterParams);
    }

    @Override
    public void setTaskFilterParams(TaskFilterParams filterParams) {
        this.filterParams = filterParams;
        applyFilterOnTasksFound = true;
    }

    @Override
    public void onOpenMapClicked() {
        getView().startMapActivity(filterParams);
    }

    @Override
    public void resetTaskInfo(TaskDetails taskDetails) {
        interactor.resetTaskInfo(getView().getContext(), taskDetails);
    }

    @Override
    public void onTaskInfoReset() {
        // refresh task list
        getView().showProgressView();
        interactor.findTasks(getMainCondition(), lastLocation, getOperationalAreaCenter(), getView().getContext().getString(R.string.house));
    }

    @Override
    public void onEventFound(Event event) {
        if (!Constants.Intervention.REGISTER_FAMILY.equals(getTaskDetails().getTaskCode())) {
            String formName = getView().getJsonFormUtils().getFormName(null, getTaskDetails().getTaskCode());
            if (StringUtils.isBlank(formName)) {
                getView().displayError(R.string.opening_form_title, R.string.form_not_found);
            } else {
                JSONObject formJSON = getView().getJsonFormUtils().getFormJSON(getView().getContext(), formName, getTaskDetails(), getStructure());
                getView().getJsonFormUtils().populateForm(event, formJSON);
                getView().getJsonFormUtils().populateFormWithServerOptions(formName,formJSON);
                getView().startForm(formJSON);
            }
        }
        getView().hideProgressDialog();
    }

    private boolean matchesTaskCodeFilterList(String value, Set<String> filterList, Pattern pattern) {
        String[] array = pattern.split(value);
        return CollectionUtils.containsAny(Arrays.asList(array), filterList);
    }

    @Override
    public void onLocationValidated() {
        if (Constants.Intervention.REGISTER_FAMILY.equals(getTaskDetails().getTaskCode())) {
            if (Task.TaskStatus.READY.name().equals(getTaskDetails().getTaskStatus())) {
                getView().registerFamily(getTaskDetails());
            } else {
                interactor.fetchFamilyDetails(getTaskDetails().getStructureId());
            }
        }

        if ((Constants.Intervention.IRS.equals(getTaskDetails().getTaskCode()))
                && !Task.TaskStatus.READY.name().equals(getTaskDetails().getTaskStatus())) { // no event for READY tasks
            interactor.findLastEvent(getTaskDetails().getTaskEntity(), SPRAY_EVENT);
        } else if (Constants.Intervention.MOSQUITO_COLLECTION.equals(getTaskDetails().getTaskCode())) {
            interactor.findLastEvent(getTaskDetails().getTaskEntity(), MOSQUITO_COLLECTION_EVENT);
        } else if (Constants.Intervention.LARVAL_DIPPING.equals(getTaskDetails().getTaskCode())) {
            interactor.findLastEvent(getTaskDetails().getTaskEntity(), LARVAL_DIPPING_EVENT);
        } else {
            super.onLocationValidated();
        }
    }

    @Override
    public void onFormSaved(@NonNull String structureId, String taskID, @NonNull Task.TaskStatus taskStatus, @NonNull String businessStatus, String interventionType) {
        getView().hideProgressDialog();
    }

    @Override
    public void onStructureAdded(Feature feature, JSONArray featureCoordinates, double zoomlevel) {
        //not used
    }

    @Override
    public void onFormSaveFailure(String eventType) {
        getView().hideProgressDialog();
    }

    @Override
    public void onFamilyFound(CommonPersonObjectClient family) {
        if (family == null)
            getView().displayNotification(R.string.fetch_family_failed, R.string.failed_to_find_family);
        else
            getView().openFamilyProfile(family, getTaskDetails());
    }

    private List<TaskDetails> getActiveTasks() {
        List<TaskDetails> activeTasks = isTasksFiltered && filteredTasks != null ? filteredTasks : tasks;
        return activeTasks != null ? activeTasks : new ArrayList<>();
    }
}
