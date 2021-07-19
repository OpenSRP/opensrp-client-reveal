package org.smartregister.reveal.fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;

import org.smartregister.reveal.R;
import org.smartregister.reveal.interactor.RevealJsonFormInteractor;
import org.smartregister.reveal.presenter.RevealJsonFormFragmentPresenter;
import org.smartregister.reveal.util.Constants;

/**
 * Created by samuelgithengi on 12/13/18.
 */
public class RevealJsonFormFragment extends JsonFormFragment {

    private RevealJsonFormFragmentPresenter presenter;

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        presenter = new RevealJsonFormFragmentPresenter(this, new RevealJsonFormInteractor());
        return presenter;
    }

    public static RevealJsonFormFragment getFormFragment(String stepName) {
        RevealJsonFormFragment jsonFormFragment = new RevealJsonFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString(JsonFormConstants.JSON_FORM_KEY.STEPNAME, stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMargins(view);
    }

    private void setupMargins(View view) {
        if (getArguments() != null) {
            String stepName = getArguments().getString(JsonFormConstants.STEPNAME);
            if (getStep(stepName).optBoolean(Constants.JsonForm.NO_PADDING)) {
                view.findViewById(R.id.main_layout).setPadding(0, 0, 0, 0);
            }
        }
    }

    public RevealJsonFormFragmentPresenter getPresenter() {
        return presenter;
    }

}
