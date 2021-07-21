package org.smartregister.reveal.fragment;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Location;
import org.smartregister.domain.ResponseErrorStatus;
import org.smartregister.domain.SyncProgress;
import org.smartregister.receiver.SyncProgressBroadcastReceiver;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.reporting.view.ProgressIndicatorView;
import org.smartregister.reveal.R;
import org.smartregister.reveal.adapter.ChildRegisterAdapter;
import org.smartregister.reveal.adapter.GroupedListableAdapter;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.ChildRegisterFragmentContract;
import org.smartregister.reveal.contract.FormProcessor;
import org.smartregister.reveal.dao.StructureDao;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.model.ChildModel;
import org.smartregister.reveal.presenter.ChildRegisterFragmentPresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.view.ChildProfileActivity;
import org.smartregister.reveal.view.ChildRegisterActivity;
import org.smartregister.reveal.view.DrawerMenuView;
import org.smartregister.reveal.viewholder.GroupedListableViewHolder;
import org.smartregister.util.Utils;
import org.smartregister.view.fragment.BaseListFragment;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;
import static org.smartregister.reveal.util.Utils.getSyncEntityString;

public class ChildRegisterFragment extends BaseListFragment<Child> implements ChildRegisterFragmentContract.View, BaseDrawerContract.DrawerActivity, SyncStatusBroadcastReceiver.SyncStatusListener,
        FormProcessor.Requester, SyncProgressBroadcastReceiver.SyncProgressListener {
    public static final String TAG = "ChildRegisterFragment";

    private BaseDrawerContract.View drawerView;
    private TextView tvTitle;
    @Nullable
    private HashMap<String, List<String>> filterAndSearch;
    private TextView searchTextView;
    private View progressIndicatorsGroupView;
    private View detailedReportCardView;
    private SyncProgressBroadcastReceiver syncProgressBroadcastReceiver = new SyncProgressBroadcastReceiver(this);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            Serializable serializable = bundle.getSerializable(Constants.ChildFilter.FILTER_PAYLOAD);
            if (serializable != null)
                filterAndSearch = (HashMap<String, List<String>>) serializable;
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void bindLayout() {
        super.bindLayout();
        drawerView = new DrawerMenuView(this);

        drawerView.initializeDrawerLayout();
        view.findViewById(R.id.drawerMenu).setOnClickListener(v -> drawerView.openDrawerLayout());

        drawerView.onResume();

        tvTitle = view.findViewById(R.id.intervention_type);
        tvTitle.setText(drawerView.getOperationalArea());

        TextView mapText = view.findViewById(R.id.txt_map_label);
        mapText.setText(R.string.label_add);
        mapText.setOnClickListener(v -> {
            if (getCurrentStructure() == null) {
                drawerView.openDrawerLayout();
            } else {
                startChildRegistrationForm();
            }
        });

        progressIndicatorsGroupView = view.findViewById(R.id.progressIndicatorsGroupView);
        detailedReportCardView = view.findViewById(R.id.indicators_card_view);

        progressIndicatorsGroupView.setOnClickListener(v -> toggleDetailedReport());
        detailedReportCardView.findViewById(R.id.btn_collapse_indicators_card_view).setOnClickListener(v -> toggleDetailedReport());

        searchTextView = view.findViewById(R.id.edt_search);
        searchTextView.setHint(R.string.search_students);
        searchTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                reloadFromSource();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // do nothing
            }
        });

        view.findViewById(R.id.filter_text_view).setOnClickListener(v -> openFilterFragment());
        if (StringUtils.isBlank(drawerView.getOperationalArea()))
            drawerView.openDrawerLayout();
    }

    @NonNull
    @Override
    protected Callable<List<Child>> onStartCallable(@Nullable Bundle bundle) {
        ChildModel model = presenter.getModel();
        return () -> model.searchAndFilter(filterAndSearch, "");
    }

    @Override
    protected int getRootLayout() {
        return R.layout.fragment_child_register;
    }

    @Override
    protected int getRecyclerViewID() {
        return R.id.recycler_view;
    }

    @Override
    protected int getProgressBarID() {
        return R.id.client_list_progress;
    }

    @Override
    public void onListItemClicked(Child child, int layoutID) {
        if (getCurrentStructure() == null) {
            drawerView.openDrawerLayout();
            return;
        }

        if (layoutID == R.id.linearLayoutAction || layoutID == R.id.btnDue) {
            startRecordMDAForm(child.getBaseEntityID());
        } else {
            ChildProfileActivity.startMe(getActivity(), child.getBaseEntityID());
        }
    }

    @Override
    public void onFetchError(Exception e) {
        Toast.makeText(getContext(), "An error occurred. " + e.getMessage(), Toast.LENGTH_SHORT).show();
        Timber.e(e);
    }

    @NonNull
    @Override
    public GroupedListableAdapter<Child, GroupedListableViewHolder<Child>> adapter() {
        return new ChildRegisterAdapter(list, this);
    }

    @NonNull
    @Override
    public ChildRegisterFragmentPresenter loadPresenter() {
        if (presenter == null) {
            presenter = new ChildRegisterFragmentPresenter()
                    .with(this)
                    .withModel(new ChildModel());
        }
        return (ChildRegisterFragmentPresenter) presenter;
    }

    @Override
    public void onDrawerClosed() {
        // to do -> re render the details
        String locationName = drawerView.getOperationalArea();
        tvTitle.setText(locationName);
        reloadFromSource();
    }

    @Override
    public void openFilterFragment() {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.ChildFilter.FILTER_PAYLOAD, filterAndSearch);
        ChildRegisterActivity.startFragment(getActivity(), ChildFilterFragment.TAG, bundle, false);
    }

    @Override
    public void startChildRegistrationForm() {
        loadPresenter().startChildRegistrationForm(getContext());
    }

    @Override
    public void startRecordMDAForm(String baseEntityID) {
        loadPresenter().startMDAForm(getContext(), baseEntityID);
    }

    @Override
    public void startJsonForm(JSONObject jsonObject, String formTitle) {
        Form form = new Form();
        form.setName(formTitle);
        form.setActionBarBackground(org.smartregister.family.R.color.family_actionbar);
        form.setNavigationBackground(org.smartregister.family.R.color.family_navigation);
        form.setHomeAsUpIndicator(org.smartregister.family.R.mipmap.ic_cross_white);
        form.setPreviousLabel(getResources().getString(org.smartregister.family.R.string.back));
        form.setWizard(false);

        getHostFormProcessor().startForm(jsonObject, form, this);
    }

    @Override
    public void reloadFromSource() {
        loadPresenter().search(filterAndSearch, searchTextView.getText().toString());
        loadPresenter().fetchReportStats();
        drawerView.checkSynced();
    }

    @Override
    public void onReportCountReloaded(Map<String, Integer> reportCounts) {
        ProgressIndicatorView progressIndicatorView = progressIndicatorsGroupView.findViewById(R.id.progressIndicatorViewTitle);

        Integer coverage = reportCounts.get(Constants.ChildRegister.MMA_COVERAGE);
        coverage = coverage == null ? 0 : coverage;

        progressIndicatorView.setProgress(coverage);
        progressIndicatorView.setTitle(getString(R.string.n_percent, coverage));

        TextView tvDoseRecordedAndDrugsGiven = detailedReportCardView.findViewById(R.id.tvDoseRecordedAndDrugsGiven);
        tvDoseRecordedAndDrugsGiven.setText(getMapValue(reportCounts, Constants.ChildRegister.MMA_VISITED_AND_ADMINISTERED));

        TextView tvChildrenNotAdministeredMedicine = detailedReportCardView.findViewById(R.id.tvChildrenNotAdministeredMedicine);
        tvChildrenNotAdministeredMedicine.setText(getMapValue(reportCounts, Constants.ChildRegister.MMA_VISITED_NOT_ADMINISTERED));

        TextView tvTotalChildrenRemaining = detailedReportCardView.findViewById(R.id.tvTotalChildrenRemaining);
        tvTotalChildrenRemaining.setText(getMapValue(reportCounts, Constants.ChildRegister.MMA_NOT_VISITED));

        TextView tvChildrenRemainingUntilTarget = detailedReportCardView.findViewById(R.id.tvChildrenRemainingUntilTarget);
        tvChildrenRemainingUntilTarget.setText(getMapValue(reportCounts, Constants.ChildRegister.MMA_TARGET_REMAINING));
    }

    private String getMapValue(Map<String, Integer> reportCounts, String key) {
        Integer value = reportCounts.get(key);
        return value == null ? "0" : Integer.toString(value);
    }

    @Override
    public void toggleDetailedReport() {
        detailedReportCardView.setVisibility(detailedReportCardView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    @Override
    public Location getCurrentStructure() {
        return org.smartregister.reveal.util.Utils.getStructureByName(PreferencesUtil.getInstance().getCurrentStructure());
    }

    @Override
    public void onResume() {
        if (!(getActivity() instanceof FormProcessor.Host))
            throw new IllegalStateException("Host activity must implement org.smartregister.reveal.contract.FormProcessor.Host");

        SyncStatusBroadcastReceiver.getInstance().addSyncStatusListener(this);
        if (drawerView != null) {
            drawerView.onResume();

            if (StringUtils.isBlank(drawerView.getOperationalArea()))
                drawerView.openDrawerLayout();
        }

        reloadFromSource();
        super.onResume();
    }

    @Override
    public void onPause() {
        SyncStatusBroadcastReceiver.getInstance().removeSyncStatusListener(this);
        super.onPause();
    }

    @Override
    public void onSyncProgress(SyncProgress syncProgress) {
        int progress = syncProgress.getPercentageSynced();
        String entity = getSyncEntityString(syncProgress.getSyncEntity());
        ProgressBar syncProgressBar = getActivity().findViewById(R.id.sync_progress_bar);
        TextView syncProgressBarLabel = getActivity().findViewById(R.id.sync_progress_bar_label);
        String labelText = String.format(getResources().getString(R.string.progressBarLabel), entity, progress);
        syncProgressBar.setProgress(progress);
        syncProgressBarLabel.setText(labelText);
    }

    @Override
    public void onSyncStart() {
        refreshSyncStatusViews(null);
        drawerView.toggleProgressBarView(true);
    }

    @Override
    public void onSyncInProgress(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
    }

    @Override
    public void onSyncComplete(FetchStatus fetchStatus) {
        refreshSyncStatusViews(fetchStatus);
        //Check sync status and Update UI to show sync status
        drawerView.checkSynced();
        drawerView.toggleProgressBarView(false);
    }

    @VisibleForTesting
    public void refreshSyncStatusViews(FetchStatus fetchStatus) {
        if (SyncStatusBroadcastReceiver.getInstance().isSyncing()) {
            Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.syncing));
            Timber.i(getString(org.smartregister.R.string.syncing));
        } else {
            if (fetchStatus != null) {

                if (fetchStatus.equals(FetchStatus.fetchedFailed)) {
                    if (fetchStatus.displayValue().equals(ResponseErrorStatus.malformed_url.name())) {
                        Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.sync_failed_malformed_url));
                        Timber.i(getString(org.smartregister.R.string.sync_failed_malformed_url));
                    } else if (fetchStatus.displayValue().equals(ResponseErrorStatus.timeout.name())) {
                        Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.sync_failed_timeout_error));
                        Timber.i(getString(org.smartregister.R.string.sync_failed_timeout_error));
                    } else {
                        Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.sync_failed));
                        Timber.i(getString(org.smartregister.R.string.sync_failed));
                    }

                } else if (fetchStatus.equals(FetchStatus.fetched)
                        || fetchStatus.equals(FetchStatus.nothingFetched)) {

                    StructureDao.resetLocationCache();

                    reloadFromSource();

                    Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.sync_complete));
                    Timber.i(getString(org.smartregister.R.string.sync_complete));

                } else if (fetchStatus.equals(FetchStatus.noConnection)) {

                    Utils.showShortToast(getActivity(), getString(org.smartregister.R.string.sync_failed_no_internet));
                    Timber.i(getString(org.smartregister.R.string.sync_failed_no_internet));
                }
            } else {
                Timber.i("Fetch Status NULL");
            }

        }
    }


    @Override
    public void onFormProcessingResult(String jsonForm) {

        try {
            JSONObject jsonFormString = new JSONObject(jsonForm);
            String title = jsonFormString.getString(ENCOUNTER_TYPE);

            if (title.equals(Constants.EventType.CHILD_REGISTRATION)) {
                loadPresenter().saveChild(jsonForm, getContext());
            } else if (title.equals(Constants.EventType.MDA_DISPENSE)) {
                loadPresenter().saveMDAForm(jsonForm, getContext());
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public FormProcessor.Host getHostFormProcessor() {
        return (FormProcessor.Host) getActivity();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        IntentFilter syncProgressFilter = new IntentFilter(AllConstants.SyncProgressConstants.ACTION_SYNC_PROGRESS);
        LocalBroadcastManager.getInstance(RevealApplication.getInstance().getApplicationContext()).registerReceiver(syncProgressBroadcastReceiver, syncProgressFilter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LocalBroadcastManager.getInstance(RevealApplication.getInstance().getApplicationContext()).unregisterReceiver(syncProgressBroadcastReceiver);
    }
}
