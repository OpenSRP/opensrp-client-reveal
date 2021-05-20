package org.smartregister.reveal.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONObject;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.OtherFormsfragmentContract;
import org.smartregister.reveal.presenter.OtherFormsFragmentPresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.LocationUtils;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.view.SummaryFormsActivity;

public class SummaryFormsFragment extends Fragment implements OtherFormsfragmentContract.View, View.OnClickListener {
    
    private OtherFormsFragmentPresenter presenter;

    private RevealJsonFormUtils jsonFormUtils;

    private ProgressDialog progressDialog;

    private LocationUtils locationUtils;

    private Button btnDailySummary;

    private Button btnTeamLeaderDos;

    private Button btnCbSprayArea;

    private Button btnIrsSaDecision;

    private Button btnMobilization;

    private Button btnIrsFieldOfficer;

    private Button btnVerificationForm;

    private Button btnTabletAccountabilityForm;

    public static SummaryFormsFragment newInstance(Bundle bundle) {

        SummaryFormsFragment fragment = new SummaryFormsFragment();
        if (bundle != null) {
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new OtherFormsFragmentPresenter(this);
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setCancelable(false);

        locationUtils =  new LocationUtils(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_summary_forms, container, false);
        initializeViews(rootView);
        return rootView;
    }

    private void initializeViews(View view)
    {
        btnDailySummary = view.findViewById(R.id.summary_daily_summary);
        btnTeamLeaderDos = view.findViewById(R.id.summary_team_leader_dos);
        btnCbSprayArea = view.findViewById(R.id.summary_cb_spray_area);
        btnIrsSaDecision = view.findViewById(R.id.summary_irs_sa_decision);
        btnMobilization = view.findViewById(R.id.summary_mobilization_form);
        btnIrsFieldOfficer = view.findViewById(R.id.summary_irs_field_officer);
        btnVerificationForm = view.findViewById(R.id.summary_verification_form);
        btnTabletAccountabilityForm = view.findViewById(R.id.summary_tablet_accountability_form);

        if(Country.KENYA.equals(BuildConfig.BUILD_COUNTRY)){
            btnDailySummary.setVisibility(View.GONE);
            view.findViewById(R.id.separator1).setVisibility(View.GONE);
            btnTeamLeaderDos.setVisibility(View.GONE);
            view.findViewById(R.id.separator2).setVisibility(View.GONE);
            btnCbSprayArea.setVisibility(View.GONE);
            view.findViewById(R.id.separator3).setVisibility(View.GONE);
            btnIrsSaDecision.setVisibility(View.GONE);
            view.findViewById(R.id.separator4).setVisibility(View.GONE);
            btnMobilization.setVisibility(View.GONE);
            view.findViewById(R.id.separator5).setVisibility(View.GONE);
            btnIrsFieldOfficer.setVisibility(View.GONE);
            view.findViewById(R.id.separator6).setVisibility(View.GONE);
            btnVerificationForm.setVisibility(View.GONE);
            view.findViewById(R.id.separator7).setVisibility(View.GONE);
            view.findViewById(R.id.separator8).setVisibility(View.GONE);
        }else if(Country.SENEGAL.equals(BuildConfig.BUILD_COUNTRY)){
            btnTeamLeaderDos.setVisibility(View.GONE);
            view.findViewById(R.id.separator2).setVisibility(View.GONE);
            btnCbSprayArea.setVisibility(View.GONE);
            view.findViewById(R.id.separator3).setVisibility(View.GONE);
            btnMobilization.setVisibility(View.GONE);
            view.findViewById(R.id.separator5).setVisibility(View.GONE);
            btnVerificationForm.setVisibility(View.GONE);
            view.findViewById(R.id.separator7).setVisibility(View.GONE);
            btnTabletAccountabilityForm.setVisibility(View.GONE);
            view.findViewById(R.id.separator8).setVisibility(View.GONE);
        }

        setClickListeners();
    }

    private void setClickListeners() {
        if(Country.KENYA.equals(BuildConfig.BUILD_COUNTRY)){
            btnTabletAccountabilityForm.setOnClickListener(this);
        } else {
            btnDailySummary.setOnClickListener(this);
            btnTeamLeaderDos.setOnClickListener(this);
            btnCbSprayArea.setOnClickListener(this);
            btnIrsSaDecision.setOnClickListener(this);
            btnMobilization.setOnClickListener(this);
            btnIrsFieldOfficer.setOnClickListener(this);
            btnVerificationForm.setOnClickListener(this);
        }

    }

    @Override
    public void displayToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public RevealJsonFormUtils getJsonFormUtils() {
        return jsonFormUtils;
    }

    @Override
    public void startForm(JSONObject formName) {
        ((SummaryFormsActivity) getActivity()).startFormActivity(formName);
    }

    @Override
    public void displayError(int title, int message) {
        new AlertDialog.Builder(getActivity()).setTitle(title).setMessage(message).create().show();
    }

    @Override
    public Location getUserCurrentLocation() {
        return locationUtils.getLastLocation();
    }

    @Override
    public void showProgressDialog(int title, int message) {
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
    public void requestUserLocation() {
        // Do nothing
    }

    public void setJsonFormUtils(RevealJsonFormUtils jsonFormUtils) {
        this.jsonFormUtils = jsonFormUtils;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.summary_daily_summary:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.DAILY_SUMMARY_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.DAILY_SUMMARY_SENEGAL);
                }
                break;
            case R.id.summary_team_leader_dos:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.TEAM_LEADER_DOS_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.TEAM_LEADER_DOS_SENEGAL);
                }
                break;
            case R.id.summary_cb_spray_area:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.CB_SPRAY_AREA_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.CB_SPRAY_AREA_SENEGAL);
                }
                break;
            case R.id.summary_irs_sa_decision:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.IRS_SA_DECISION_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.IRS_SA_DECISION_SENEGAL);
                }
                break;
            case R.id.summary_mobilization_form:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.MOBILIZATION_FORM_ZAMBIA);
                } else {
                    presenter.showBasicForm(Constants.JsonForm.MOBILIZATION_FORM_SENEGAL);
                }
                break;
            case R.id.summary_irs_field_officer:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.IRS_FIELD_OFFICER_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.IRS_FIELD_OFFICER_SENEGAL);
                }
                break;
            case R.id.summary_verification_form:
                if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
                    presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.VERIFICATION_FORM_ZAMBIA);
                } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
                    presenter.showBasicForm(Constants.JsonForm.VERIFICATION_FORM_SENEGAL);
                }
                break;
            case R.id.summary_tablet_accountability_form:
                presenter.showBasicForm(org.smartregister.reveal.util.Constants.JsonForm.TABLET_ACCOUNTABILITY_FORM);
            default:
                break;
        }
    }
}
