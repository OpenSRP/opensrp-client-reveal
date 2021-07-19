package org.smartregister.reveal.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;
import org.smartregister.family.adapter.ViewPagerAdapter;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.OtherFormsContract;
import org.smartregister.reveal.fragment.SummaryFormsFragment;
import org.smartregister.reveal.presenter.OtherFormsPresenter;
import org.smartregister.reveal.util.RevealJsonFormUtils;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.JSON_FORM_PARAM_JSON;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_GET_JSON;

public class SummaryFormsActivity extends AppCompatActivity implements OtherFormsContract.View {

    private OtherFormsPresenter presenter;
    private RevealJsonFormUtils jsonFormUtils;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_summary_forms);
        presenter = new OtherFormsPresenter(this);

        Toolbar toolbar = (Toolbar) this.findViewById(R.id.summary_toolbar);
        toolbar.setTitle(R.string.return_to_register);
        this.setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        setupViews();
    }

    protected void setupViews() {
        TabLayout tabLayout = findViewById(R.id.tabs);
        ViewPager viewPager = findViewById(R.id.viewpager);
        tabLayout.setupWithViewPager(setupViewPager(viewPager));
    }

    protected ViewPager setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        jsonFormUtils = new RevealJsonFormUtils();

        SummaryFormsFragment otherFormsFragment = SummaryFormsFragment.newInstance(this.getIntent().getExtras());
        otherFormsFragment.setJsonFormUtils(jsonFormUtils);
        adapter.addFragment(otherFormsFragment, this.getString(R.string.summary_forms).toUpperCase());


        viewPager.setAdapter(adapter);

        return viewPager;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_GET_JSON && resultCode == RESULT_OK && data.hasExtra(JSON_FORM_PARAM_JSON)) {
            String json = data.getStringExtra(JSON_FORM_PARAM_JSON);
            Timber.d( json);
            getPresenter().saveJsonForm(json);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonObject) {
        jsonFormUtils.startJsonForm(jsonObject, this);
    }

    @Override
    public void saveJsonForm(String json) {
        // do nothing
    }

    @Override
    public void showProgressDialog(int titleIdentifier) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle(titleIdentifier);
        progressDialog.setMessage(getString(R.string.please_wait_message));
        if (!isFinishing())
            progressDialog.show();
    }

    @Override
    public void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private OtherFormsContract.Presenter getPresenter() {
        return (OtherFormsContract.Presenter) presenter;
    }
}
