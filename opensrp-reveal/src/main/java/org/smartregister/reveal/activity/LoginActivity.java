package org.smartregister.reveal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.presenter.LoginPresenter;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.view.ChildRegisterActivity;
import org.smartregister.reveal.view.ListTasksActivity;
import org.smartregister.task.SaveTeamLocationsTask;
import org.smartregister.view.activity.BaseLoginActivity;
import org.smartregister.view.contract.BaseLoginContract;

public class LoginActivity extends BaseLoginActivity implements BaseLoginContract.View {

    private ImageView mainLogo;

    @Override
    protected int getContentView() {
        return R.layout.activity_login;
    }

    @Override
    protected void initializePresenter() {
        mLoginPresenter = new LoginPresenter(this);
    }

    @Override
    public void goToHome(boolean remote) {
        if (remote) {
            org.smartregister.util.Utils.startAsyncTask(new SaveTeamLocationsTask(), null);
        }
        RevealApplication.getInstance().getContext().anmLocationController().evict();
        Class<?> startActivity = ListTasksActivity.class;

        if (BuildConfig.BUILD_COUNTRY == Country.NTD_SCHOOL)
            startActivity = ChildRegisterActivity.class;

        Intent intent = new Intent(this, startActivity);
        startActivity(intent);

        finish();

        RevealApplication.getInstance().processServerConfigs();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoginPresenter.processViewCustomizations();
        if (!mLoginPresenter.isUserLoggedOut()) {
            goToHome(false);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLogo = findViewById(R.id.login_logo);
        setUpLogos();
    }

    private void setUpLogos() {
        if (BuildConfig.BUILD_COUNTRY == Country.THAILAND || BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN) {
            ImageView partnerLogo = findViewById(R.id.partner_logo);
            partnerLogo.setVisibility(View.VISIBLE);
            mainLogo.setBackgroundResource(R.drawable.ic_dvbd_logo);
        } else {
            mainLogo.setBackgroundResource(R.drawable.ic_logo_login);
        }
    }
}
