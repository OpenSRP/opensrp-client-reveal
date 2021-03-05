package org.smartregister.reveal.view;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.vijay.jsonwizard.customviews.TreeViewDialog;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.smartregister.CoreLibrary;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.interactor.BaseDrawerInteractor;
import org.smartregister.reveal.presenter.BaseDrawerPresenter;
import org.smartregister.reveal.util.AlertDialogUtils;
import org.smartregister.reveal.util.Constants.Tags;
import org.smartregister.reveal.util.Country;
import org.smartregister.util.NetworkUtils;
import org.smartregister.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.COPYDBNAME;
import static org.smartregister.reveal.util.Constants.DBNAME;

/**
 * Created by samuelgithengi on 3/21/19.
 */
public class DrawerMenuView implements View.OnClickListener, BaseDrawerContract.View {


    private TextView planTextView;
    private TextView operationalAreaTextView;
    private TextView districtTextView;
    private TextView facilityTextView;
    private TextView operatorTextView;
    private TextView p2pSyncTextView;

    private DrawerLayout mDrawerLayout;

    private BaseDrawerContract.Presenter presenter;

    private BaseDrawerContract.DrawerActivity activity;

    private BaseDrawerContract.Interactor interactor;

    public DrawerMenuView(BaseDrawerContract.DrawerActivity activity) {
        this.activity = activity;
        presenter = new BaseDrawerPresenter(this, activity);
        interactor = new BaseDrawerInteractor(presenter);
    }

    @Override
    public void initializeDrawerLayout() {

        mDrawerLayout = getContext().findViewById(R.id.drawer_layout);

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {//do nothing
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {//do nothing
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                presenter.onDrawerClosed();
            }

            @Override
            public void onDrawerStateChanged(int newState) {//do nothing
            }
        });

        NavigationView navigationView = getContext().findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        headerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                headerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int minimumOperatorMargin = getContext().getResources().getDimensionPixelSize(R.dimen.operator_top_margin);
                int screenHeightPixels = getContext().getResources().getDisplayMetrics().heightPixels;
                //if content of hamburger menu is bigger than screen; scroll content
                if (screenHeightPixels < headerView.getHeight() + minimumOperatorMargin) {
                    headerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    View operator = headerView.findViewById(R.id.operator_label);
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) operator.getLayoutParams();
                    params.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;
                    operator.setLayoutParams(params);
                } else {//content of hamburger menu fits on screen; set menu height to screen height
                    headerView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            screenHeightPixels - getContext().getResources().getDimensionPixelSize(R.dimen.hamburger_margin)));
                }
            }
        });

        try {
            String manifestVersion = getManifestVersion();
            String appVersion = getContext().getString(R.string.app_version, Utils.getVersion(getContext()));
            String appVersionText = appVersion + (manifestVersion == null ? "" : getContext().getString(R.string.manifest_version_parenthesis_placeholder, manifestVersion));
            ((TextView) headerView.findViewById(R.id.application_version))
                    .setText(appVersionText);
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e);
        }


        if (BuildConfig.BUILD_COUNTRY != Country.THAILAND && BuildConfig.BUILD_COUNTRY != Country.THAILAND_EN) {
            String buildDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(BuildConfig.BUILD_TIMESTAMP));
            ((TextView) headerView.findViewById(R.id.application_updated)).setText(getContext().getString(R.string.app_updated, buildDate));
        }

        planTextView = headerView.findViewById(R.id.plan_selector);
        operationalAreaTextView = headerView.findViewById(R.id.operational_area_selector);
        districtTextView = headerView.findViewById(R.id.district_label);
        facilityTextView = headerView.findViewById(R.id.facility_label);
        operatorTextView = headerView.findViewById(R.id.operator_label);
        p2pSyncTextView = headerView.findViewById(R.id.btn_navMenu_p2pSyncBtn);

        TextView offlineMapTextView = headerView.findViewById(R.id.btn_navMenu_offline_maps);

        TextView summaryFormsTextView = headerView.findViewById(R.id.btn_navMenu_summaryForms);

        if (operationalAreaTextView != null)
            operationalAreaTextView.setOnClickListener(this);

        if (planTextView != null)
            planTextView.setOnClickListener(this);

        if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA || BuildConfig.BUILD_COUNTRY == Country.SENEGAL) { // Enable P2P sync and other forms
            p2pSyncTextView.setVisibility(View.VISIBLE);
            p2pSyncTextView.setOnClickListener(this);

            summaryFormsTextView.setVisibility(View.VISIBLE);
            summaryFormsTextView.setOnClickListener(this);

            TextView filledForms = headerView.findViewById(R.id.btn_navMenu_filled_forms);
            filledForms.setVisibility(View.VISIBLE);
            filledForms.setOnClickListener(this);

        } else if(BuildConfig.BUILD_COUNTRY == Country.KENYA){
            summaryFormsTextView.setVisibility(View.VISIBLE);
            summaryFormsTextView.setOnClickListener(this);

            TextView filledForms = headerView.findViewById(R.id.btn_navMenu_filled_forms);
            filledForms.setVisibility(View.VISIBLE);
            filledForms.setOnClickListener(this);

        }

        if (BuildConfig.BUILD_COUNTRY != Country.NTD_SCHOOL) {
            offlineMapTextView.setVisibility(View.VISIBLE);
            offlineMapTextView.setOnClickListener(this);
        }

        headerView.findViewById(R.id.logout_button).setOnClickListener(this);
        headerView.findViewById(R.id.sync_button).setOnClickListener(this);
        headerView.findViewById(R.id.btn_navMenu_offline_maps).setOnClickListener(this);

        districtTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startStatsActivity();
                return true;
            }
        });

        operatorTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getContext(), R.string.export_db_notification, Toast.LENGTH_LONG).show();
                String currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.ENGLISH).format(new Date());
                Utils.copyDatabase(DBNAME, COPYDBNAME + "-" + currentTimeStamp + ".db", getContext());
                return false;
            }
        });
        //Check sync status and Update UI to show sync status
        checkSynced();
    }

    @Override
    public void setPlan(String campaign) {
        if (planTextView != null)
            planTextView.setText(campaign);
    }

    @Override
    public void setOperationalArea(String operationalArea) {
        operationalAreaTextView.setText(operationalArea);
    }

    @Override
    public String getPlan() {
        return planTextView != null ? planTextView.getText().toString() : "";
    }

    @Override
    public String getOperationalArea() {
        return operationalAreaTextView != null ? operationalAreaTextView.getText().toString() : null;
    }

    @Override
    public void setDistrict(String district) {
        org.smartregister.reveal.util.Utils.setTextViewText(districtTextView, R.string.district, district);
    }

    @Override
    public void setFacility(String facility, String facilityLevel) {
        org.smartregister.reveal.util.Utils.setTextViewText(facilityTextView,
                Tags.CANTON.equals(facilityLevel) ? R.string.canton : R.string.facility, facility);
    }

    @Override
    public void setOperator() {
        org.smartregister.reveal.util.Utils.setTextViewText(operatorTextView, R.string.operator,
                RevealApplication.getInstance().getContext().allSharedPreferences().fetchRegisteredANM());
    }

    @Override
    public void lockNavigationDrawerForSelection() {
        mDrawerLayout.openDrawer(GravityCompat.START);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);

    }

    @Override
    public void lockNavigationDrawerForSelection(int title, int message) {
        AlertDialogUtils.displayNotification(getContext(), title, message);
        mDrawerLayout.openDrawer(GravityCompat.START);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);

    }


    @Override
    public void unlockNavigationDrawer() {
        if (mDrawerLayout.getDrawerLockMode(GravityCompat.START) == DrawerLayout.LOCK_MODE_LOCKED_OPEN) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    @Override
    public void showOperationalAreaSelector(Pair<String, ArrayList<String>> locationHierarchy) {
        try {
            TreeViewDialog treeViewDialog = new TreeViewDialog(getContext(),
                    R.style.AppTheme_WideDialog,
                    new JSONArray(locationHierarchy.first), locationHierarchy.second, locationHierarchy.second);
            treeViewDialog.setCancelable(true);
            treeViewDialog.setCanceledOnTouchOutside(true);
            treeViewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    presenter.onOperationalAreaSelectorClicked(treeViewDialog.getName());
                }
            });
            treeViewDialog.show();
        } catch (JSONException e) {
            Timber.e(e);
        }

    }

    @Override
    public void showStructureSelector(Pair<String, ArrayList<String>> locationHierarchy) {
        try {
            TreeViewDialog treeViewDialog = new TreeViewDialog(getContext(),
                    R.style.AppTheme_WideDialog,
                    new JSONArray(locationHierarchy.first), locationHierarchy.second, locationHierarchy.second);
            treeViewDialog.setCancelable(true);
            treeViewDialog.setCanceledOnTouchOutside(true);
            treeViewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    presenter.onStructureSelectorClicked(treeViewDialog.getName());
                }
            });
            treeViewDialog.show();
        } catch (JSONException e) {
            Timber.e(e);
        }
    }


    @Override
    public void showPlanSelector(List<String> campaigns, String entireTreeString) {
        if (StringUtils.isBlank(entireTreeString)) {
            displayNotification(R.string.plans_download_on_progress_title, R.string.plans_download_on_progress);
            return;
        }
        try {
            TreeViewDialog treeViewDialog = new TreeViewDialog(getContext(),
                    R.style.AppTheme_WideDialog,
                    new JSONArray(entireTreeString), new ArrayList<>(campaigns), new ArrayList<>(campaigns));
            treeViewDialog.show();
            treeViewDialog.setCanceledOnTouchOutside(true);
            treeViewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    presenter.onPlanSelectorClicked(treeViewDialog.getValue(), treeViewDialog.getName());
                }
            });
            treeViewDialog.show();
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public void displayNotification(int title, int message, Object... formatArgs) {
        AlertDialogUtils.displayNotification(getContext(), title, message, formatArgs);
    }

    @Override
    public Activity getContext() {
        return activity.getActivity();
    }


    @Override
    public void openDrawerLayout() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }


    public void closeDrawerLayout() {
        if (presenter.isPlanAndOperationalAreaSelected()) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.operational_area_selector)
            if (BuildConfig.BUILD_COUNTRY != Country.NTD_SCHOOL) {
                presenter.onShowOperationalAreaSelector();
            } else {
                presenter.onShowStructureSelector();
            }
        else if (v.getId() == R.id.plan_selector)
            presenter.onShowPlanSelector();
        else if (v.getId() == R.id.logout_button)
            RevealApplication.getInstance().logoutCurrentUser();
        else if (v.getId() == R.id.btn_navMenu_p2pSyncBtn)
            startP2PActivity();
        else if (v.getId() == R.id.btn_navMenu_summaryForms)
            startOtherFormsActivity();
        else if (v.getId() == R.id.btn_navMenu_offline_maps)
            presenter.onShowOfflineMaps();
        else if (v.getId() == R.id.btn_navMenu_filled_forms)
            presenter.onShowFilledForms();
        else if (v.getId() == R.id.sync_button) {
            toggleProgressBarView(true);
            org.smartregister.reveal.util.Utils.startImmediateSync();
            closeDrawerLayout();
        }
    }

    @Override
    public BaseDrawerContract.Presenter getPresenter() {
        return presenter;
    }

    @Override
    public void onResume() {
        presenter.onViewResumed();
    }

    private void startP2PActivity() {
        getContext().startActivity(new Intent(getContext(), LocationPickerActivity.class));
    }

    @Override
    public void openOfflineMapsView() {
        Intent intent = new Intent(getContext(), OfflineMapsActivity.class);
        getContext().startActivity(intent);
    }

    @Override
    public void checkSynced() {
        interactor.checkSynced();
    }

    @Override
    public void toggleProgressBarView(boolean syncing) {
        ProgressBar progressBar = this.activity.getActivity().findViewById(R.id.sync_progress_bar);
        TextView progressLabel = this.activity.getActivity().findViewById(R.id.sync_progress_bar_label);
        TextView syncButton = this.activity.getActivity().findViewById(R.id.sync_button);
        TextView syncBadge = this.activity.getActivity().findViewById(R.id.sync_label);
        if (progressBar == null || syncBadge == null)
            return;
        if (syncing && NetworkUtils.isNetworkAvailable()) { //only hide the sync button when there is internet connection
            progressBar.setVisibility(View.VISIBLE);
            progressLabel.setVisibility(View.VISIBLE);
            syncButton.setVisibility(View.INVISIBLE);
            syncBadge.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
            progressLabel.setVisibility(View.INVISIBLE);
            syncButton.setVisibility(View.VISIBLE);
            syncBadge.setVisibility(View.VISIBLE);
        }
    }


    private void startOtherFormsActivity() {
        getContext().startActivity(new Intent(getContext(), SummaryFormsActivity.class));
    }

    private void startStatsActivity() {
        getContext().startActivity(new Intent(getContext(), StatsActivity.class));
    }

    @Nullable
    @Override
    public String getManifestVersion() {
        return CoreLibrary.getInstance().context().allSharedPreferences().fetchManifestVersion();
    }
}
