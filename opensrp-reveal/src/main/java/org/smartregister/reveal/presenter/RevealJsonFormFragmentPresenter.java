package org.smartregister.reveal.presenter;

import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.customviews.MaterialSpinner;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.utils.ValidationStatus;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.reveal.R;
import org.smartregister.reveal.activity.RevealJsonFormActivity;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.PasswordRequestCallback;
import org.smartregister.reveal.contract.UserLocationContract.UserLocationCallback;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.LocationUtils;
import org.smartregister.reveal.util.PasswordDialogUtils;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.view.RevealMapView;
import org.smartregister.reveal.widget.GeoWidgetFactory;
import org.smartregister.reveal.widget.RevealToasterNotesFactory;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import io.ona.kujaku.listeners.BaseLocationListener;

/**
 * Created by samuelgithengi on 1/30/19.
 */
public class RevealJsonFormFragmentPresenter extends JsonFormFragmentPresenter implements PasswordRequestCallback, UserLocationCallback {

    private JsonFormFragment formFragment;

    private AlertDialog passwordDialog;

    private RevealJsonFormActivity jsonFormView;

    private RevealMapView mapView;

    private ValidateUserLocationPresenter locationPresenter;

    private LocationUtils locationUtils;

    private Location lastLocation;

    private BaseLocationListener locationListener;

    private RevealJsonFormUtils jsonFormUtils;

    public RevealJsonFormFragmentPresenter(JsonFormFragment formFragment, JsonFormInteractor jsonFormInteractor) {
        super(formFragment, jsonFormInteractor);
        this.formFragment = formFragment;
        passwordDialog = PasswordDialogUtils.initPasswordDialog(formFragment.getActivity(), this);
        jsonFormView = (RevealJsonFormActivity) formFragment.getActivity();
        locationPresenter = new ValidateUserLocationPresenter(jsonFormView, this);
        locationUtils = new LocationUtils(jsonFormView);
        locationListener = new BaseLocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastLocation = location;
            }
        };
        locationUtils.requestLocationUpdates(locationListener);
        jsonFormUtils = new RevealJsonFormUtils();
    }

    @Override
    public void validateAndWriteValues() {
        super.validateAndWriteValues();
        boolean showError = true;
        for (View childAt : formFragment.getJsonApi().getFormDataViews()) {
            if (childAt instanceof RevealMapView) {
                RevealMapView mapView = (RevealMapView) childAt;
                ValidationStatus validationStatus = GeoWidgetFactory.validate(formFragment, mapView, this);
                String key = (String) childAt.getTag(com.vijay.jsonwizard.R.id.key);
                String mStepName = this.getView().getArguments().getString("stepName");
                String fieldKey = mStepName + " (" + mStepDetails.optString("title") + ") :" + key;
                if (!validationStatus.isValid()) {
                    getInvalidFields().put(fieldKey, validationStatus);
                    showError = !validationStatus.getErrorMessage().equalsIgnoreCase(getView().getContext().getResources().getString(R.string.register_outside_boundary_warning));
                } else {
                    getInvalidFields().remove(fieldKey);
                    if (isFormValid() && validateFarStructures()) {
                        validateUserLocation(mapView);
                        return;
                    }
                }
                this.mapView = mapView;
                break;//exit loop, assumption; there will be only 1 map per form.
            } else if (childAt instanceof TextView && !(childAt instanceof MaterialEditText)) {
                ValidationStatus validationStatus = RevealToasterNotesFactory.validate(formFragment, (TextView) childAt);
                String address = (String) childAt.getTag(com.vijay.jsonwizard.R.id.address);
                if (!validationStatus.isValid()) {
                    getInvalidFields().put(address, validationStatus);
                } else {
                    getInvalidFields().remove(address);
                }
            }
        }
        if (isFormValid()) {// if form is valid and did not have a map, if it had a map view it will be handled above
            onLocationValidated();

        } else {//if form is invalid whether having a map or not
            if (showErrorsOnSubmit()) {
                launchErrorDialog();
                getView().showToast(getView().getContext().getResources().getString(R.string.json_form_error_msg, this.getInvalidFields().size()));
            } else if (showError) {
                getView().showSnackBar(getView().getContext().getResources().getString(R.string.json_form_error_msg, this.getInvalidFields().size()));
            }
        }
    }

    @VisibleForTesting
    protected boolean validateFarStructures() {
        return Utils.validateFarStructures();
    }


    private void validateUserLocation(RevealMapView mapView) {
        this.mapView = mapView;
        Location location = jsonFormView.getUserCurrentLocation();
        if (location != null) {
            locationPresenter.onGetUserLocation(location);
        } else {
            locationPresenter.requestUserLocation();
        }
    }

    @Override
    public void onSaveClick(LinearLayout mainView) {
        validateAndWriteValues();
    }

    @Override
    public void onLocationValidated() {
        jsonFormView.hideProgressDialog();
        Intent returnIntent = new Intent();
        getView().onFormFinish();
        returnIntent.putExtra("json", getView().getCurrentJsonState());
        Object skipValidation = formFragment.getMainView().getTag(com.vijay.jsonwizard.R.id.skip_validation);
        returnIntent.putExtra(JsonFormConstants.SKIP_VALIDATION, Boolean.valueOf(skipValidation == null ? Boolean.FALSE.toString() : skipValidation.toString()));
        getView().finishWithResult(returnIntent);

    }

    @Override
    public LatLng getTargetCoordinates() {
        return mapView.getMapboxMap().getCameraPosition().target;
    }

    @Override
    public void requestUserPassword() {
        if (passwordDialog != null) {
            passwordDialog.show();
        }
    }

    @Override
    public void onPasswordVerified() {
        onLocationValidated();
    }

    @Override
    public ValidateUserLocationPresenter getLocationPresenter() {
        return locationPresenter;
    }

    public LocationUtils getLocationUtils() {
        return locationUtils;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public BaseLocationListener getLocationListener() {
        return locationListener;
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        super.onItemSelected(parent, view, position, id);
        String key = (String) parent.getTag(R.id.key);
        Map<String, JSONObject> fields = jsonFormUtils.getFields(jsonFormView.getmJSONObject());
        cascadeSelect(key, JsonForm.DISTRICT, Constants.CONFIGURATION.HEALTH_FACILITIES, fields.get(JsonForm.HFC_BELONG));
        cascadeSelect(key, JsonForm.DISTRICT, Constants.CONFIGURATION.HEALTH_FACILITIES, fields.get(JsonForm.HFC_SEEK));
        cascadeSelect(key, JsonForm.DATA_COLLECTOR, Constants.CONFIGURATION.SPRAY_OPERATORS, fields.get(JsonForm.SPRAY_OPERATOR_CODE));
        cascadeSelect(key, JsonForm.HFC_BELONG, Constants.CONFIGURATION.COMMUNITY_HEALTH_WORKERS, fields.get(JsonForm.CHW_NAME));
        cascadeSelect(key, JsonForm.CATCHMENT_AREA, Constants.CONFIGURATION.MDA_CORDINATORS, fields.get(JsonForm.COORDINATOR_NAME));
        cascadeSelect(key, JsonForm.CATCHMENT_AREA, Constants.CONFIGURATION.MDA_ENUMERATORS, fields.get(JsonForm.DATA_COLLECTOR));
        cascadeSelect(key, JsonForm.CATCHMENT_AREA, Constants.CONFIGURATION.MDA_COMMUNITY_HEALTH_WORKERS, fields.get(JsonForm.CHW_NAME));
        cascadeSelect(key, JsonForm.CATCHMENT_AREA, Constants.CONFIGURATION.MDA_ADHERENCE_OFFICERS, fields.get(JsonForm.ADHERENCE_NAME));
        cascadeSelect(key,JsonForm.LOCATION,Constants.CONFIGURATION.HEALTH_WORKER_SUPERVISORS,fields.get(JsonForm.HEALTH_WORKER_SUPERVISOR));
        cascadeSelect(key,JsonForm.HEALTH_WORKER_SUPERVISOR,Constants.CONFIGURATION.COMMUNITY_DRUG_DISTRIBUTORS,fields.get(JsonForm.COMMUNITY_DRUG_DISTRIBUTOR_NAME));
    }

    private void cascadeSelect(String key, String parentWidget, String configurationKey, JSONObject childWidget) {
        if (parentWidget.equals(key)) {
            String value = JsonFormUtils.getFieldValue(getView().getCurrentJsonState(), key);
            if (!TextUtils.isEmpty(value)) {
                Pair<JSONArray, JSONArray> options = jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs()
                        , configurationKey, childWidget, value.split(":")[0]);
                if (options != null) {
                    List<String> newAdapterValues = new Gson().fromJson(options.second.toString(), new TypeToken<List<String>>() {
                    }.getType());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getView().getContext(), R.layout.native_form_simple_list_item_1, newAdapterValues);
                    MaterialSpinner spinner = (MaterialSpinner) jsonFormView.getFormDataView(JsonFormConstants.STEP1 + ":" + childWidget.optString(JsonFormUtils.KEY));
                    if (spinner != null) {
                        Object selected;
                        if (spinner.getAdapter().getCount() == spinner.getSelectedItemPosition()) {
                            selected = spinner.getAdapter().getItem(spinner.getSelectedItemPosition() - 1);
                        } else {
                            selected = spinner.getSelectedItem();
                        }
                        spinner.setAdapter(adapter);
                        spinner.setOnItemSelectedListener(formFragment.getCommonListener());
                        spinner.setTag(R.id.keys, options.first);
                        if (selected != null && newAdapterValues.contains(selected.toString())) {
                            spinner.setSelection(newAdapterValues.indexOf(selected.toString()));
                        }
                    }
                }
            }
        }
    }

    public void onGetUserLocation(Location location) {
        //empty
    }

}
