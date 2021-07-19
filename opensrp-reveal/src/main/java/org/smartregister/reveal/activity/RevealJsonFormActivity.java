package org.smartregister.reveal.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.activities.FormConfigurationJsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.UserLocationContract.UserLocationView;
import org.smartregister.reveal.fragment.RevealJsonFormFragment;
import io.ona.kujaku.utils.Constants;

/**
 * Created by samuelgithengi on 12/13/18.
 */
public class RevealJsonFormActivity extends FormConfigurationJsonFormActivity implements UserLocationView {

    private RevealJsonFormFragment formFragment;

    private boolean requestedLocation;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    @Override
    public void initializeFormFragment() {

        RevealJsonFormFragment revealJsonFormFragment = RevealJsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, revealJsonFormFragment).commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.RequestCode.LOCATION_SETTINGS && requestedLocation) {
            if (resultCode == RESULT_OK) {
                formFragment.getPresenter().getLocationUtils().requestLocationUpdates(formFragment.getPresenter().getLocationListener());
                formFragment.getPresenter().getLocationPresenter().waitForUserLocation();
            } else if (resultCode == RESULT_CANCELED) {
                formFragment.getPresenter().getLocationPresenter().onGetUserLocationFailed();
            }
            requestedLocation = false;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof RevealJsonFormFragment) {
            formFragment = (RevealJsonFormFragment) fragment;
        }
    }

    @Override
    public Location getUserCurrentLocation() {
        return formFragment.getPresenter().getLastLocation();
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
    public void requestUserLocation() {
        formFragment.getPresenter().getLocationUtils().checkLocationSettingsAndStartLocationServices(this, formFragment.getPresenter().getLocationListener());
        requestedLocation = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        formFragment.getPresenter().getLocationUtils().stopLocationClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        formFragment.getPresenter().getLocationUtils().destroy();
    }
}
