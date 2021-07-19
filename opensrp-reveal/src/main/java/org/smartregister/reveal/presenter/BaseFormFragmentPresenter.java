package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.domain.Location;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.BaseFormFragmentContract;
import org.smartregister.reveal.interactor.BaseFormFragmentInteractor;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.repository.RevealMappingHelper;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.Intervention;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.reveal.util.PasswordDialogUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.DateTimeTypeConverter;
import org.smartregister.util.JsonFormUtils;

import java.lang.ref.WeakReference;

import io.ona.kujaku.listeners.BaseLocationListener;
import timber.log.Timber;

import static org.smartregister.family.util.Constants.JSON_FORM_KEY.OPTIONS;
import static org.smartregister.reveal.util.Constants.DateFormat.EVENT_DATE_FORMAT_Z;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MDA_ADHERENCE;
import static org.smartregister.reveal.util.Constants.Intervention.MDA_DISPENSE;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;

/**
 * Created by samuelgithengi on 4/18/19.
 */
public class BaseFormFragmentPresenter extends BaseLocationListener implements BaseFormFragmentContract.Presenter {

    private final WeakReference<BaseFormFragmentContract.View> view;
    private AlertDialog passwordDialog;

    private ValidateUserLocationPresenter locationPresenter;

    protected RevealMappingHelper mappingHelper;

    private Location structure;

    private BaseTaskDetails taskDetails;

    private Context context;

    private BaseFormFragmentInteractor interactor;

    private PreferencesUtil prefsUtil;

    protected Gson gson = new GsonBuilder().setDateFormat(EVENT_DATE_FORMAT_Z)
            .registerTypeAdapter(DateTime.class, new DateTimeTypeConverter()).create();

    private RevealJsonFormUtils jsonFormUtils = new RevealJsonFormUtils();

    protected BaseFormFragmentPresenter(BaseFormFragmentContract.View view, Context context) {
        this.context = context;
        this.view = new WeakReference<>(view);
        passwordDialog = PasswordDialogUtils.initPasswordDialog(context, this);
        locationPresenter = new ValidateUserLocationPresenter(view, this);
        mappingHelper = new RevealMappingHelper();
        interactor = new BaseFormFragmentInteractor(this);
        prefsUtil = PreferencesUtil.getInstance();
    }

    protected boolean validateFarStructures() {
        return Utils.validateFarStructures();
    }

    private void validateUserLocation() {
        android.location.Location location = getView().getUserCurrentLocation();
        if (location == null) {
            locationPresenter.requestUserLocation();
        } else {
            locationPresenter.onGetUserLocation(location);
        }
    }

    @Override
    public void onPasswordVerified() {
        onLocationValidated();
    }

    @Override
    public void onLocationValidated() {
        if (!Intervention.REGISTER_FAMILY.equals(getTaskDetails().getTaskCode())) {
            String formName = getView().getJsonFormUtils().getFormName(null, taskDetails.getTaskCode());
            if (StringUtils.isBlank(formName)) {
                getView().displayError(R.string.opening_form_title, R.string.form_not_found);
            } else {
                JSONObject formJSON = getView().getJsonFormUtils().getFormJSON(context, formName, taskDetails, structure);
                if (Intervention.BEDNET_DISTRIBUTION.equals(taskDetails.getTaskCode())) {
                    interactor.findNumberOfMembers(taskDetails.getTaskEntity(), formJSON);
                    return;
                } else if (CASE_CONFIRMATION.equals(taskDetails.getTaskCode())) {
                    interactor.findMemberDetails(taskDetails.getStructureId(), formJSON);
                    return;
                } else if (MDA_DISPENSE.equals(taskDetails.getTaskCode()) || MDA_ADHERENCE.equals(taskDetails.getTaskCode())) {
                    jsonFormUtils.populateServerOptions(RevealApplication.getInstance().getServerConfigs(), Constants.CONFIGURATION.MDA_CATCHMENT_AREAS, jsonFormUtils.getFields(formJSON).get(JsonForm.CATCHMENT_AREA), prefsUtil.getCurrentDistrict());
                    getView().startForm(formJSON);
                } else {
                    getView().startForm(formJSON);
                }
            }
        }
        getView().hideProgressDialog();
    }

    public void showBasicForm(String formName) {
        JSONObject formJSON = getView().getJsonFormUtils().getFormJSON(context, formName, null, null);
        jsonFormUtils.populateFormWithServerOptions(formName, formJSON);
        getView().startForm(formJSON);
    }

    @Override
    public LatLng getTargetCoordinates() {
        android.location.Location center = mappingHelper.getCenter(gson.toJson(structure.getGeometry()));
        return new LatLng(center.getLatitude(), center.getLongitude());
    }

    @Override
    public void requestUserPassword() {
        if (passwordDialog != null) {
            passwordDialog.show();
        }
    }

    @Override
    public ValidateUserLocationPresenter getLocationPresenter() {
        return locationPresenter;
    }

    protected BaseFormFragmentContract.View getView() {
        return view.get();
    }

    @Override
    public void onStructureFound(Location structure, BaseTaskDetails details) {
        this.structure = structure;
        this.taskDetails = details;
        if (IRS.equals(details.getTaskCode()) || MOSQUITO_COLLECTION.equals(details.getTaskCode()) ||
                LARVAL_DIPPING.equals(details.getTaskCode()) || REGISTER_FAMILY.equals(details.getTaskCode()) ||
                BEDNET_DISTRIBUTION.equals(details.getTaskCode()) || CASE_CONFIRMATION.equals(details.getTaskCode()) ||
                BLOOD_SCREENING.equals(details.getTaskCode())) {
            if (validateFarStructures()) {
                validateUserLocation();
            } else {
                onLocationValidated();
            }
        } else {
            onLocationValidated();
        }
    }

    @Override
    public void onFetchedMembersCount(Pair<Integer, Integer> numberOfMembers, JSONObject formJSON) {
        try {
            String jsonStr = formJSON.toString().replace(JsonForm.NUMBER_OF_FAMILY_MEMBERS, numberOfMembers.first + "");
            jsonStr = jsonStr.replace(JsonForm.NUMBER_OF_FAMILY_MEMBERS_SLEEPING_OUTDOORS, numberOfMembers.second + "");
            getView().startForm(new JSONObject(jsonStr));
        } catch (JSONException e) {
            Timber.e(e, "Error updating Number of members");
            getView().startForm(formJSON);
        }
        getView().hideProgressDialog();
    }

    @Override
    public void onFetchedFamilyMembers(JSONArray familyMembers, JSONObject formJSON) {
        JSONObject familyMemberField = JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formJSON), JsonForm.FAMILY_MEMBER);
        try {
            familyMemberField.put(OPTIONS, familyMembers);
        } catch (JSONException e) {
            Timber.e(e, "Error updating family members");
        }
        getView().startForm(formJSON);
    }

    @Override
    public void onFetchedSprayDetails(CommonPersonObject commonPersonObject, JSONObject formJSON) {
        getView().getJsonFormUtils().populateSprayForm(commonPersonObject, formJSON);
        getView().startForm(formJSON);
    }

    public BaseTaskDetails getTaskDetails() {
        return taskDetails;
    }

    public void setTaskDetails(BaseTaskDetails taskDetails) {
        this.taskDetails = taskDetails;
    }

    public Location getStructure() {
        return structure;
    }

    public void onGetUserLocation(android.location.Location location){
        //empty
    };
}
