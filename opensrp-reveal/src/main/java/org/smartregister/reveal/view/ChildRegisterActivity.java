package org.smartregister.reveal.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.FormProcessor;
import org.smartregister.reveal.fragment.ChildFilterFragment;
import org.smartregister.reveal.fragment.ChildRegisterFragment;
import org.smartregister.view.activity.SecuredActivity;

public class ChildRegisterActivity extends SecuredActivity implements FormProcessor.Host {
    public static final String DISPLAY_FRAGMENT = "DISPLAY_FRAGMENT";
    private FormProcessor.Requester requester;

    public static void startFragment(Activity activity, String fragmentName, Bundle bundle, boolean clearStack) {
        Intent intent = new Intent(activity, ChildRegisterActivity.class);
        intent.putExtra(DISPLAY_FRAGMENT, fragmentName);

        if (bundle != null)
            intent.putExtras(bundle);
        if (clearStack)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.child_register_activity);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String fragmentName = bundle.getString(DISPLAY_FRAGMENT);
            Fragment fragment = getDestinationFragment(fragmentName);
            if (fragment != null) {
                fragment.setArguments(bundle);
                switchToFragment(fragment);
            }
        } else {
            switchToFragment(getDestinationFragment(ChildRegisterFragment.TAG));
        }
    }

    private void switchToFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .add(R.id.content, fragment)
                .commit();
    }

    @Nullable
    private Fragment getDestinationFragment(@Nullable String destinationFragment) {
        if (destinationFragment == null)
            return null;

        switch (destinationFragment) {
            case ChildFilterFragment
                    .TAG:
                return new ChildFilterFragment();
            default:
                return new ChildRegisterFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onCreation() {
        // do nothing
    }

    @Override
    protected void onResumption() {
        // do nothing
    }

    @Override
    public void startForm(JSONObject jsonObject, Form form, FormProcessor.Requester requester) {
        this.requester = requester;

        Intent intent = new Intent(this, JsonWizardFormActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
            if (jsonString != null && requester != null) {
                requester.onFormProcessingResult(jsonString);
                requester = null;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
