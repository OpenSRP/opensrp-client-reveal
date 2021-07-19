package org.smartregister.reveal.fragment;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.util.Utils;

import static org.junit.Assert.assertEquals;

public class CaseClassificationFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    private AppCompatActivity activity;

    private CaseClassificationFragment fragment;

    @Before
    public void setUp() {
        fragment = new CaseClassificationFragment();
        activity = Robolectric.buildActivity(AppCompatActivity.class).create().start().get();
        activity.setContentView(R.layout.activity_family_profile);
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "Case Details").commit();
    }

    @Test
    public void testDisplayIndexCaseDisplaysBlank() {
        fragment.displayIndexCase(new JSONObject());
        TextView familyNameTextView = Whitebox.getInternalState(fragment, "familyNameTextView");
        assertEquals("Family name:  ", familyNameTextView.getText());

        TextView focusReasonTextView = Whitebox.getInternalState(fragment, "focusReasonTextView");
        assertEquals("Focus Reason:  ", focusReasonTextView.getText());

    }


    @Test
    public void testDisplayIndexCase() throws JSONException {
        fragment.displayIndexCase(new JSONObject(TestingUtils.caseConfirmstionEventJSON));
        TextView familyNameTextView = Whitebox.getInternalState(fragment, "familyNameTextView");
        assertEquals("Family name: วีรศักดิ์", familyNameTextView.getText());


        TextView dobTextView = Whitebox.getInternalState(fragment, "dobTextView");
        assertEquals(activity.getString(R.string.dob_format, Utils.getDob(20)), dobTextView.getText());


        TextView caseNumberTextView = Whitebox.getInternalState(fragment, "caseNumberTextView");
        assertEquals("Case Number: 131412000001031181107101758977", caseNumberTextView.getText());

        TextView focusReasonTextView = Whitebox.getInternalState(fragment, "focusReasonTextView");
        assertEquals("Focus Reason: Investigation", focusReasonTextView.getText());
    }

}
