package org.smartregister.reveal.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;

import org.codehaus.plexus.util.StringUtils;
import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.reveal.TestRevealApplication;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.shadow.ContextShadowHelper;
import org.smartregister.reveal.view.ChildProfileActivity;

import java.util.Date;

import static org.robolectric.Shadows.shadowOf;

/**
 * @author ronald
 */

@RunWith(RobolectricTestRunner.class)
@Config(application = TestRevealApplication.class, sdk = 22, shadows = {ContextShadowHelper.class})
public class ChildProfileActivityTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ChildProfileActivity activity;
    private String childBaseID = "childBaseID";
    private ActivityController<ChildProfileActivity> controller;

    @Mock
    private ChildProfileContract.Presenter presenter;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(ChildProfileActivity.class).create().start();
        activity = controller.get();


        Context context = Context.getInstance();
        CoreLibrary.init(context);

        //Auto login by default
        String password = "pwd";
        context.session().start(context.session().lengthInMilliseconds());
        //context.configuration().getDrishtiApplication().setPassword(password.toCharArray());
        context.session().setPassword(password.getBytes());

        MockitoAnnotations.initMocks(this);
        Intent testIntent = new Intent();
        testIntent.putExtra(org.smartregister.reveal.util.Constants.Properties.BASE_ENTITY_ID, childBaseID);
        controller = Robolectric.buildActivity(ChildProfileActivity.class, testIntent).create().start();

        activity = controller.get();
        ReflectionHelpers.setField(activity, "presenter", presenter);
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
            controller.pause().stop().destroy(); //destroy controller if we can
        } catch (Exception e) {
            e.printStackTrace();
        }

        //logout
        Context context = Context.getInstance();
        context.session().expire();

        System.gc();
    }

    @Test
    public void testViewsCreated() {
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "tvNames"));
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "tvNumber"));
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "tvGender"));
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "tvAge"));
        Assert.assertNotNull(ReflectionHelpers.getField(activity, "progressBar"));
    }

    @Test
    public void testOnFetchResult() {
        Child child = new Child();
        child.setGrade("Grade");
        child.setFirstName("Fname");
        child.setLastName("Lname");
        child.setGender("Male");
        child.setBirthDate(new Date());
        child.setUniqueID("uid");


        activity.onFetchResult(child);
        TextView tvNames = ReflectionHelpers.getField(activity, "tvNames");
        String names = tvNames.getText().toString();
        Assert.assertTrue(names.contains(child.getFirstName()));
        Assert.assertTrue(names.contains(child.getLastName()));

        TextView tvNumber = ReflectionHelpers.getField(activity, "tvNumber");
        child.getUniqueID().equals(tvNumber.getText().toString());

        TextView tvGender = ReflectionHelpers.getField(activity, "tvGender");
        child.getGender().equals(tvGender.getText().toString());

        TextView tvAge = ReflectionHelpers.getField(activity, "tvAge");
        Assert.assertTrue(StringUtils.isNotBlank(tvAge.getText().toString()));
    }

    @Test
    public void testSetLoadingState() {
        ProgressBar progressBar = ReflectionHelpers.getField(activity, "progressBar");
        progressBar.setVisibility(View.INVISIBLE);

        activity.setLoadingState(true);
        Assert.assertEquals(progressBar.getVisibility(), View.VISIBLE);
        activity.setLoadingState(true);
        Assert.assertEquals(progressBar.getVisibility(), View.VISIBLE);
        activity.setLoadingState(false);
        Assert.assertEquals(progressBar.getVisibility(), View.VISIBLE);
        activity.setLoadingState(false);
        activity.setLoadingState(false);
        activity.setLoadingState(false);
        Assert.assertEquals(progressBar.getVisibility(), View.INVISIBLE);
    }

    @Test
    public void testStartEditForm() {
        activity.startEditForm();
        Mockito.verify(presenter).startChildRegistrationForm(Mockito.any(), Mockito.any());
    }

    @Test
    public void testStartADRForm() {
        activity.startADRForm();
        Mockito.verify(presenter).startADRForm(Mockito.any(), Mockito.any());
    }

    @Test
    public void testStartJsonForm() {
        activity.startJsonForm(new JSONObject(), "Title");

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        Assert.assertThat(shadowIntent.getIntentClass().getName(),
                CoreMatchers.equalTo(JsonWizardFormActivity.class.getName()));
    }

    @Test
    public void testReloadFromSource() {
        activity.reloadFromSource();
        Mockito.verify(presenter).fetchProfileData(childBaseID);
    }

    @Test
    public void testOnActivityResult() throws JSONException {
        Intent data = new Intent();
        JSONObject jsonObject = new JSONObject("{\"encounter_type\": \"Update Child Registration\"}");

        data.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());
        activity.onActivityResult(JsonFormUtils.REQUEST_CODE_GET_JSON, Activity.RESULT_OK, data);
        Mockito.verify(presenter).updateChild(Mockito.any(), Mockito.any());


        jsonObject = new JSONObject("{\"encounter_type\": \"mma_adr\"}");

        data.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());
        activity.onActivityResult(JsonFormUtils.REQUEST_CODE_GET_JSON, Activity.RESULT_OK, data);
        Mockito.verify(presenter).saveADRForm(Mockito.any(), Mockito.any());
    }
}
