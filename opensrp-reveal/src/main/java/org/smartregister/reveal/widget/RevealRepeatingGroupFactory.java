package org.smartregister.reveal.widget;

import android.widget.TextView;

import com.vijay.jsonwizard.widgets.RepeatingGroupFactory;

import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.activity.RevealJsonFormActivity;
import org.smartregister.reveal.util.Country;

import static org.smartregister.reveal.util.Constants.JsonForm.ROOMS_ELIGIBLE;
import static org.smartregister.reveal.util.Constants.JsonForm.ROOMS_SPRAYED;
import static org.smartregister.util.JsonFormUtils.STEP1;

public class RevealRepeatingGroupFactory extends RepeatingGroupFactory {

    public static final String CONFIRMED_ROOMS_NOT_SPRAYED = "Confirmed # rooms not sprayed";
    public static final String CONFIRMED_ROOMS_NOT_SPRAYED_ERROR = "Please correct number of rooms not sprayed ";

    @Override
    protected void addOnDoneAction(TextView textView) {
        if(Country.SENEGAL.equals(BuildConfig.BUILD_COUNTRY)){
            Integer reasonsCount = Integer.parseInt(textView.getText().toString());
            if(CONFIRMED_ROOMS_NOT_SPRAYED.equals(textView.getHint().toString())){
                RevealJsonFormActivity activity = (RevealJsonFormActivity) textView.getContext();
                TextView roomSprayedTextView = (TextView) activity.getFormDataView(  STEP1 + ":" + ROOMS_SPRAYED);
                Integer roomsSprayedCount = Integer.parseInt(roomSprayedTextView.getText().toString());
                TextView roomsEligibleTextView = (TextView) activity.getFormDataView(STEP1 + ":" + ROOMS_ELIGIBLE);
                Integer roomsEligible = Integer.parseInt(roomsEligibleTextView.getText().toString());
                if (reasonsCount != (roomsEligible - roomsSprayedCount)){
                    textView.setError(CONFIRMED_ROOMS_NOT_SPRAYED_ERROR);
                    return;
                }
            }
        }
        super.addOnDoneAction(textView);
    }
}
