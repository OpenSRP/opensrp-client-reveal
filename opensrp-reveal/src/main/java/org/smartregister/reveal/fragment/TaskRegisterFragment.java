package org.smartregister.reveal.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Task;
import org.smartregister.family.fragment.NoMatchDialogFragment;
import org.smartregister.family.util.DBConstants;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.adapter.TaskRegisterAdapter;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.TaskRegisterFragmentContract;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.model.FilterConfiguration;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.presenter.TaskRegisterFragmentPresenter;
import org.smartregister.reveal.task.IndicatorsCalculatorTask;
import org.smartregister.reveal.util.AlertDialogUtils;
import org.smartregister.reveal.util.Constants.Properties;
import org.smartregister.reveal.util.Constants.TaskRegister;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.LocationUtils;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.view.DrawerMenuView;
import org.smartregister.reveal.view.FilterTasksActivity;
import org.smartregister.reveal.view.ListTasksActivity;
import org.smartregister.reveal.view.StructureTasksActivity;
import org.smartregister.reveal.view.TaskRegisterActivity;
import org.smartregister.view.activity.BaseRegisterActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import io.ona.kujaku.utils.Constants;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.smartregister.reveal.util.Constants.Action;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_SPRAYED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.NOT_VISITED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.PARTIALLY_SPRAYED;
import static org.smartregister.reveal.util.Constants.BusinessStatus.SPRAYED;
import static org.smartregister.reveal.util.Constants.Filter.FILTER_CONFIGURATION;
import static org.smartregister.reveal.util.Constants.Filter.FILTER_SORT_PARAMS;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.Intervention.TASK_RESET_INTERVENTIONS;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_FILTER_TASKS;

/**
 * Created by samuelgithengi on 3/11/19.
 */
public class TaskRegisterFragment extends BaseDrawerRegisterFragment implements TaskRegisterFragmentContract.View, BaseDrawerContract.DrawerActivity {

    private TaskRegisterAdapter taskAdapter;

    private RevealJsonFormUtils jsonFormUtils;
    private ProgressDialog progressDialog;
    private TextView interventionTypeTv;

    private LocationUtils locationUtils;

    private boolean hasRequestedLocation;

    private RefreshRegisterReceiver refreshRegisterReceiver = new RefreshRegisterReceiver();

    private CardView indicatorsCardView;

    private TextView filterTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerView = new DrawerMenuView(this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_task_register;
    }

    public void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns) {
        taskAdapter = new TaskRegisterAdapter(getActivity(), registerActionHandler);
        clientsView.setAdapter(taskAdapter);
    }

    @Override
    public void setupViews(View view) {
        super.setupViews(view);
        interventionTypeTv = view.findViewById(R.id.intervention_type);
        if (getActivity() != null) {
            interventionTypeTv.setText(
                    getActivity().getIntent().getStringExtra(TaskRegister.INTERVENTION_TYPE));
        }
        view.findViewById(R.id.txt_map_label).setOnClickListener(v -> getPresenter().onOpenMapClicked());
        drawerView.initializeDrawerLayout();
        view.findViewById(R.id.drawerMenu).setOnClickListener(v -> drawerView.openDrawerLayout());
        drawerView.onResume();

        initializeProgressIndicatorViews(view);

        filterTextView = view.findViewById(R.id.filter_text_view);
        filterTextView.setOnClickListener(v -> {
            getPresenter().onFilterTasksClicked();
        });

        TaskFilterParams filterParams = (TaskFilterParams) getActivity().getIntent().getSerializableExtra(FILTER_SORT_PARAMS);
        if (filterParams != null) {
            getPresenter().setTaskFilterParams(filterParams);
        }
    }

    @Override
    public void filter(String filterString, String joinTableString, String mainConditionString, boolean qrCode) {
        getSearchCancelView().setVisibility(isEmpty(filterString) ? View.INVISIBLE : View.VISIBLE);
        if (isEmpty(filterString)) {
            org.smartregister.util.Utils.hideKeyboard(getActivity());
        }
        getPresenter().searchTasks(filterString);
    }

    @Override
    public void startMapActivity(TaskFilterParams taskFilterParams) {
        Intent intent = new Intent(getContext(), ListTasksActivity.class);
        if (taskFilterParams != null) {
            taskFilterParams.setSearchPhrase(getSearchView().getText().toString());
            intent.putExtra(FILTER_SORT_PARAMS, taskFilterParams);
        } else if (StringUtils.isNotBlank(getSearchView().getText())) {
            intent.putExtra(FILTER_SORT_PARAMS, TaskFilterParams.builder().searchPhrase(searchView.getText().toString()).build());
        }
        getActivity().setResult(RESULT_OK, intent);
        getActivity().finish();
    }

    @Override
    public Location getLastLocation() {
        if (getActivity() != null && getActivity().getIntent().getExtras() != null) {
            return getActivity().getIntent().getExtras().getParcelable(TaskRegister.LAST_USER_LOCATION);
        } else {
            return null;
        }
    }

    @Override
    protected void initializePresenter() {
        presenter = new TaskRegisterFragmentPresenter(this, TaskRegister.VIEW_IDENTIFIER);
        locationUtils = new LocationUtils(getContext());
        locationUtils.requestLocationUpdates(getPresenter());
    }

    @Override
    public void setUniqueID(String qrCode) {
        if (this.getSearchView() != null) {
            this.getSearchView().setText(qrCode);
        }
    }

    @Override
    public void setAdvancedSearchFormData(HashMap<String, String> hashMap) {//not used
    }

    @Override
    protected String getMainCondition() {
        return null;//not used
    }

    @Override
    protected String getDefaultSortQuery() {
        return null;//not used
    }

    @Override
    protected void startRegistration() {////not used on reveal/ adding points done on map
    }

    @Override
    protected void onViewClicked(View view) {
        TaskDetails details = (TaskDetails) view.getTag(R.id.task_details);

        if (TASK_RESET_INTERVENTIONS.contains(details.getTaskCode())
                && Task.TaskStatus.COMPLETED.name().equals(details.getTaskStatus())) {
            displayTaskActionDialog(details, view);
        } else {
            getPresenter().onTaskSelected(details, view.getId() == R.id.task_action);
        }

    }

    public void displayTaskActionDialog(TaskDetails details, View view) {
        int viewDetailsStringResource =  PAOT.equals(details.getTaskCode()) ? R.string.view_paot_details : R.string.view_details;
        AlertDialogUtils.displayNotificationWithCallback(getContext(), R.string.select_task_action,
                R.string.choose_action, viewDetailsStringResource, R.string.undo, new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case BUTTON_POSITIVE:
                                getPresenter().onTaskSelected(details, view.getId() == R.id.task_action);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                displayResetTaskInfoDialog(details);
                                break;
                            default:
                                break;
                        }
                        dialog.dismiss();
                    }

                });
    }

    public void displayResetTaskInfoDialog(TaskDetails details) {
        AlertDialogUtils.displayNotificationWithCallback(getContext(), R.string.undo_task_title,
                R.string.undo_task_msg, R.string.confirm, R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE)
                            getPresenter().resetTaskInfo(details);
                        dialog.dismiss();
                    }
                });
    }

    @Override
    public void showNotFoundPopup(String opensrpId) {
        if (this.getActivity() != null) {
            NoMatchDialogFragment.launchDialog((BaseRegisterActivity) this.getActivity(), "dialog", opensrpId);
        }
    }

    private TaskRegisterFragmentPresenter getPresenter() {
        return (TaskRegisterFragmentPresenter) presenter;
    }

    @Override
    public void setTotalPatients() {
        //do nothing using @link setTotalTasks(structuresWithinBuffer)
    }


    @Override
    public void setTotalTasks(int structuresWithinBuffer) {
        if (isAdded() && headerTextDisplay != null) {
            headerTextDisplay.setText(getResources().getQuantityString(R.plurals.structures,
                    taskAdapter.getItemCount(), structuresWithinBuffer, Utils.getLocationBuffer(), taskAdapter.getItemCount()));

            filterRelativeLayout.setVisibility(View.GONE);
        }
    }

    public void setTaskDetails(List<TaskDetails> tasks) {
        taskAdapter.setTaskDetails(tasks);
        if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA || BuildConfig.BUILD_COUNTRY == Country.NAMIBIA
                || BuildConfig.BUILD_COUNTRY == Country.SENEGAL
                || (BuildConfig.BUILD_COUNTRY == Country.REFAPP && R.string.irs == Utils.getInterventionLabel())) {
            new IndicatorsCalculatorTask(getActivity(), tasks).execute();
        }
    }

    @Override
    public void displayNotification(int title, int message, Object... formatArgs) {
        setRefreshList(false);
        AlertDialogUtils.displayNotification(getContext(), title, message, formatArgs);
    }

    @Override
    public void startForm(JSONObject formName) {
        ((TaskRegisterActivity) getActivity()).startFormActivity(formName);
    }

    @Override
    public void displayError(int title, int message) {
        new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).create().show();
    }

    @Override
    public void onDestroy() {
        getPresenter().onDestroy();
        super.onDestroy();
    }

    @Override
    public void onDrawerClosed() {
        getPresenter().onDrawerClosed();

    }

    @Override
    public RevealJsonFormUtils getJsonFormUtils() {
        return jsonFormUtils;
    }

    @Override
    public Location getUserCurrentLocation() {
        return locationUtils.getLastLocation();
    }

    @Override
    public void requestUserLocation() {
        hasRequestedLocation = true;
        locationUtils.checkLocationSettingsAndStartLocationServices(getActivity(), getPresenter());
    }

    @Override
    public void displayToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public LocationUtils getLocationUtils() {
        return locationUtils;
    }

    @Override
    public void showProgressDialog(@StringRes int title, @StringRes int message) {
        if (progressDialog != null) {
            progressDialog.setTitle(title);
            progressDialog.setMessage(getString(message));
            progressDialog.show();
        }
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void setInventionType(int interventionLabel) {
        interventionTypeTv.setText(getString(interventionLabel));
    }

    @Override
    public void registerFamily(BaseTaskDetails taskDetails) {
        ((TaskRegisterActivity) getActivity()).startFamilyRegistration(taskDetails);
    }

    @Override
    public void openFamilyProfile(CommonPersonObjectClient family, BaseTaskDetails taskDetails) {
        Intent intent = new Intent(getContext(), org.smartregister.family.util.Utils.metadata().profileActivity);
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, family.getCaseId());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, org.smartregister.family.util.Utils.getValue(family.getColumnmaps(), DBConstants.KEY.FAMILY_HEAD, false));
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, org.smartregister.family.util.Utils.getValue(family.getColumnmaps(), DBConstants.KEY.PRIMARY_CAREGIVER, false));
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, org.smartregister.family.util.Utils.getValue(family.getColumnmaps(), DBConstants.KEY.FIRST_NAME, false));
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.GO_TO_DUE_PAGE, false);


        intent.putExtra(Properties.LOCATION_UUID, taskDetails.getStructureId());
        intent.putExtra(Properties.TASK_IDENTIFIER, taskDetails.getTaskId());
        intent.putExtra(Properties.TASK_BUSINESS_STATUS, taskDetails.getBusinessStatus());
        intent.putExtra(Properties.TASK_STATUS, taskDetails.getTaskStatus());

        startActivity(intent);

    }

    @Override
    public void openTasksScreen(BaseTaskDetails taskDetails) {
        Intent intent = new Intent(getContext(), StructureTasksActivity.class);
        intent.putExtra(Properties.LOCATION_UUID, taskDetails.getStructureId());
        intent.putExtra(Properties.TASK_IDENTIFIER, taskDetails.getTaskId());
        intent.putExtra(Properties.TASK_BUSINESS_STATUS, taskDetails.getBusinessStatus());
        intent.putExtra(Properties.TASK_STATUS, taskDetails.getTaskStatus());
        startActivity(intent);
    }

    @Override
    public void displayIndexCaseDetails(JSONObject indexCase) {
        ((TaskRegisterActivity) getActivity()).displayIndexCaseFragment(indexCase);
    }

    @Override
    public void setNumberOfFilters(int numberOfFilters) {
        filterTextView.setText(getString(R.string.filters, numberOfFilters));
        int padding = getResources().getDimensionPixelSize(R.dimen.filter_toggle_end_margin);
        filterTextView.setPadding(padding, 0, padding, 0);
    }

    @Override
    public void clearFilter() {
        filterTextView.setText(getString(R.string.filter));
        int padding = getResources().getDimensionPixelSize(R.dimen.filter_toggle_padding);
        filterTextView.setPadding(padding, 0, padding, 0);
    }

    @Override
    public TaskRegisterAdapter getAdapter() {
        return taskAdapter;
    }

    @Override
    public void openFilterActivity(TaskFilterParams filterParams) {
        Intent intent = new Intent(getContext(), FilterTasksActivity.class);
        intent.putExtra(FILTER_SORT_PARAMS, filterParams);
        FilterConfiguration.FilterConfigurationBuilder builder = FilterConfiguration.builder();
        if (BuildConfig.BUILD_COUNTRY.equals(Country.NAMIBIA)) {
            builder.taskCodeLayoutEnabled(false)
                    .interventionTypeLayoutEnabled(false)
                    .businessStatusList(Arrays.asList(NOT_VISITED, NOT_SPRAYED, PARTIALLY_SPRAYED, SPRAYED))
                    .sortOptions(R.array.task_sort_options_namibia);
        }
        intent.putExtra(FILTER_CONFIGURATION, builder.build());
        getActivity().startActivityForResult(intent, REQUEST_CODE_FILTER_TASKS);
    }

    @Override
    public void setSearchPhrase(String searchPhrase) {
        getSearchView().setText(searchPhrase);
    }

    public void setJsonFormUtils(RevealJsonFormUtils jsonFormUtils) {
        this.jsonFormUtils = jsonFormUtils;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.RequestCode.LOCATION_SETTINGS && hasRequestedLocation) {
            if (resultCode == RESULT_OK) {
                locationUtils.requestLocationUpdates(getPresenter());
                getPresenter().getLocationPresenter().waitForUserLocation();
            } else if (resultCode == RESULT_CANCELED) {
                getPresenter().getLocationPresenter().onGetUserLocationFailed();
            }
            hasRequestedLocation = false;
        } else if (requestCode == REQUEST_CODE_FILTER_TASKS && resultCode == RESULT_OK && data.hasExtra(FILTER_SORT_PARAMS)) {
            TaskFilterParams filterParams = (TaskFilterParams) data.getSerializableExtra(FILTER_SORT_PARAMS);
            getPresenter().filterTasks(filterParams);
        }
    }

    @Override
    public void onPause() {
        if (getContext() != null)
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(refreshRegisterReceiver);
        setViewVisibility(indicatorsCardView, false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(Action.STRUCTURE_TASK_SYNCED);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(refreshRegisterReceiver, filter);
        }
    }

    private class RefreshRegisterReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            getPresenter().initializeQueries(getMainCondition());
        }
    }

    private void initializeProgressIndicatorViews(View view) {

        LinearLayout progressIndicatorsGroupView = view.findViewById(R.id.progressIndicatorsGroupView);
        progressIndicatorsGroupView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openIndicatorsCardView();
            }
        });

        indicatorsCardView = view.findViewById(R.id.indicators_card_view);
        indicatorsCardView.findViewById(R.id.btn_collapse_indicators_card_view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setViewVisibility(indicatorsCardView, false);
            }
        });
    }

    private void openIndicatorsCardView() {

        setViewVisibility(indicatorsCardView, true);
    }


    private void setViewVisibility(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSyncStart() {
        super.onSyncStart();
        drawerView.toggleProgressBarView(true);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        super.onSyncComplete(fetchStatus);
        //Check sync status and Update UI to show sync status
        drawerView.checkSynced();
        drawerView.toggleProgressBarView(false);
    }

}
