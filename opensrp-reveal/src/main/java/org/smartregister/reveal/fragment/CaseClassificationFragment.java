package org.smartregister.reveal.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.CaseClassificationContract;
import org.smartregister.reveal.util.Constants;
import org.smartregister.util.Utils;

public class CaseClassificationFragment extends Fragment implements CaseClassificationContract.View {

    private TextView familyNameTextView;
    private TextView villageTextView;
    private TextView quarterTextView;
    private TextView streetTextView;
    private TextView landmarkTextView;

    private TextView surnameTextView;
    private TextView firstNameTextView;
    private TextView middleNameTextView;
    private TextView dobTextView;

    private TextView caseNumberTextView;
    private TextView caseClassificationTextView;
    private TextView focusStatusTextView;
    private TextView focusReasonTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frament_case_classification, container, false);
        initializeViews(rootView);
        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.return_to_register);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        return rootView;
    }

    private void initializeViews(View rootView) {
        familyNameTextView = rootView.findViewById(R.id.family_name);
        villageTextView = rootView.findViewById(R.id.village_town);
        quarterTextView = rootView.findViewById(R.id.quarter_clan);
        streetTextView = rootView.findViewById(R.id.street);
        landmarkTextView = rootView.findViewById(R.id.landmark);

        surnameTextView = rootView.findViewById(R.id.surname);
        firstNameTextView = rootView.findViewById(R.id.first_name);
        middleNameTextView = rootView.findViewById(R.id.middle_name);
        dobTextView = rootView.findViewById(R.id.dob);

        caseNumberTextView = rootView.findViewById(R.id.case_number);
        caseClassificationTextView = rootView.findViewById(R.id.case_classification);
        focusStatusTextView = rootView.findViewById(R.id.focus_status);
        focusReasonTextView = rootView.findViewById(R.id.focus_reason);
    }

    @Override
    public void displayIndexCase(@NonNull JSONObject indexCase) {
        JSONObject details = indexCase.optJSONObject(Constants.DETAILS);
        if (details == null) {
            details = new JSONObject();
        }
        familyNameTextView.setText(getString(R.string.family_name_format, details.optString("family_name")));
        villageTextView.setText(getString(R.string.village_town_format, details.optString("village_town")));
        quarterTextView.setText(getString(R.string.quarter_clan_format, details.optString("quarter_clan")));
        streetTextView.setText(getString(R.string.street_format, details.optString("street")));
        landmarkTextView.setText(getString(R.string.landmark_format, details.optString("landmark")));

        surnameTextView.setText(getString(R.string.surname_format, details.optString("surname")));
        firstNameTextView.setText(getString(R.string.first_name_format, details.optString("first_name")));
        middleNameTextView.setText(getString(R.string.middle_name_format, details.optString("middle_name")));
        int age = details.optInt("age", -1);
        dobTextView.setText(getString(R.string.dob_format, age == -1 ? "" : Utils.getDob(age)));

        caseNumberTextView.setText(getString(R.string.case_number_format, details.optString("case_number")));
        caseClassificationTextView.setText(getString(R.string.case_classification_format, details.optString("case_classification")));
        focusStatusTextView.setText(getString(R.string.focus_status_format, details.optString("focus_status")));
        focusReasonTextView.setText(getString(R.string.focus_reason_format, details.optString("focus_reason")));

    }
}
