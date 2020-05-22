package org.smartregister.reveal.view;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.presenter.ChildProfilePresenter;
import org.smartregister.reveal.util.Constants;
import org.smartregister.util.GenericInteractor;
import org.smartregister.view.activity.BaseProfileActivity;

import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;

public class ChildProfileActivity extends BaseProfileActivity implements ChildProfileContract.View {

    protected ProgressBar progressBar;
    protected AtomicInteger incompleteRequests = new AtomicInteger(0);
    private String childBaseEntityID;
    private TextView tvNames;
    private TextView tvNumber;
    private TextView tvGender;
    private TextView tvAge;

    public static void startMe(Activity activity, String childBaseEntityID) {
        Intent intent = new Intent(activity, ChildProfileActivity.class);
        intent.putExtra(Constants.Properties.BASE_ENTITY_ID, childBaseEntityID);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        setContentView(R.layout.activity_child_profile);
        Toolbar toolbar = findViewById(R.id.collapsing_toolbar);
        toolbar.findViewById(R.id.toolbar_title).setOnClickListener(v -> onBackPressed());
        setSupportActionBar(toolbar);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            childBaseEntityID = bundle.getString(Constants.Properties.BASE_ENTITY_ID);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            final Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
            upArrow.setColorFilter(getResources().getColor(R.color.text_blue), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        appBarLayout = findViewById(R.id.collapsing_toolbar_appbarlayout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setOutlineProvider(null);
        }

        initializePresenter();
        setupViews();
        fetchProfileData();
    }

    @Override
    protected void setupViews() {
        tvNames = findViewById(R.id.tvNames);
        tvNumber = findViewById(R.id.tvNumber);
        tvGender = findViewById(R.id.tvGender);
        tvAge = findViewById(R.id.tvAge);
        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    protected void initializePresenter() {
        presenter = new ChildProfilePresenter(this)
                .usingInteractor(new GenericInteractor());
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.edit_child) {
            startEditForm();
            return true;
        } else if (i == R.id.record_adverse_drugs) {
            startADRForm();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.child_profile_menu, menu);
        return true;
    }

    @Override
    protected ViewPager setupViewPager(ViewPager viewPager) {
        return null;
    }

    @Override
    protected void fetchProfileData() {
        getPresenter().fetchProfileData(childBaseEntityID);
    }


    @Override
    public void onFetchResult(Child child) {
        String names = StringUtils.isBlank(child.getGrade()) ? child.getFullName() : child.getFullName() + ", " + child.getGrade();
        tvNames.setText(names);
        tvNumber.setText(child.getUniqueID());
        tvGender.setText(child.getGender());
        tvAge.setText(getString(R.string.child_age, child.getAge()));
    }

    @Override
    public void setLoadingState(boolean state) {
        int result = state ? incompleteRequests.incrementAndGet() : incompleteRequests.decrementAndGet();
        progressBar.setVisibility(result > 0 ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public ChildProfileContract.Presenter getPresenter() {
        return (ChildProfileContract.Presenter) presenter;
    }

    @Override
    public void onError(Exception e) {
        Toast.makeText(getBaseContext(), R.string.an_error_occured, Toast.LENGTH_SHORT).show();
        Timber.e(e);
        finish();
    }

    @Override
    public void startEditForm() {
        getPresenter().startChildRegistrationForm(getBaseContext(), childBaseEntityID);
    }

    @Override
    public void startADRForm() {
        getPresenter().startADRForm(getBaseContext(), childBaseEntityID);
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

        Intent intent = new Intent(this, FamilyWizardFormActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void reloadFromSource() {
        fetchProfileData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String title = jsonObject.getString(ENCOUNTER_TYPE);

                if (title.equals(Constants.EventType.UPDATE_CHILD_REGISTRATION)) {
                    getPresenter().updateChild(jsonObject, getApplicationContext());
                } else if (title.equals(Constants.EventType.MDA_ADVERSE_DRUG_REACTION)) {
                    getPresenter().saveADRForm(jsonObject, getApplicationContext());
                }

            } catch (JSONException e) {
                Timber.e(e);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}