package org.smartregister.reveal.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.activities.JsonWizardFormActivity;
import com.vijay.jsonwizard.domain.Form;

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
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.reveal.TestRevealApplication;
import org.smartregister.reveal.contract.FormProcessor;
import org.smartregister.reveal.shadow.ContextShadowHelper;
import org.smartregister.reveal.shadow.DrawerMenuViewShadow;
import org.smartregister.reveal.shadow.SyncStatusBroadcastReceiverShadowHelper;
import org.smartregister.reveal.view.ChildRegisterActivity;

import static org.robolectric.Shadows.shadowOf;

/**
 * @author ronald
 */

@RunWith(RobolectricTestRunner.class)
@Config(application = TestRevealApplication.class, sdk = 22, shadows = {ContextShadowHelper.class, DrawerMenuViewShadow.class, SyncStatusBroadcastReceiverShadowHelper.class})
public class ChildRegisterActivityTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ChildRegisterActivity activity;
    private ActivityController<ChildRegisterActivity> controller;

    @Mock
    private FormProcessor.Requester requester;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(ChildRegisterActivity.class).create().start();
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
        controller = Robolectric.buildActivity(ChildRegisterActivity.class, testIntent).create().start();

        activity = controller.get();
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
    public void testStartForm() {
        Form form = new Form();
        activity.startForm(new JSONObject(), form, requester);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        Assert.assertThat(shadowIntent.getIntentClass().getName(),
                CoreMatchers.equalTo(JsonWizardFormActivity.class.getName()));
    }

    @Test
    public void testOnActivityResult() throws JSONException {

        Whitebox.setInternalState(activity, "requester", requester);

        Intent data = new Intent();
        data.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, new JSONObject("{}").toString());

        activity.onActivityResult(JsonFormUtils.REQUEST_CODE_GET_JSON, Activity.RESULT_OK, data);
        Mockito.verify(requester).onFormProcessingResult(Mockito.anyString());
    }

}
