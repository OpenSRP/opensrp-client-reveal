package org.smartregister.reveal.view;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.TaskRegisterContract;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.TaskRegister;
import org.smartregister.reveal.util.RevealJsonFormUtils;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;
import org.smartregister.view.viewpager.OpenSRPViewPager;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.reveal.util.Constants.JSON_FORM_PARAM_JSON;
import static org.smartregister.reveal.util.Constants.RequestCode.REQUEST_CODE_GET_JSON;
import static org.smartregister.reveal.util.FamilyConstants.Intent.START_REGISTRATION;

/**
 * Created by samuelgithengi on 1/24/20.
 */
public class TaskRegisterActivityTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private TaskRegisterActivity taskRegisterActivity;

    @Mock
    private RevealJsonFormUtils jsonFormUtils;

    @Mock
    private TaskRegisterContract.Presenter presenter;

    @Mock
    private BaseRegisterFragment mBaseFragment;

    @Mock
    private OpenSRPViewPager mPager;

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        taskRegisterActivity = Robolectric.buildActivity(TaskRegisterActivity.class).create().resume().get();
    }


    @Test
    public void testOnCreate() {
        assertNotNull(Robolectric.buildActivity(TaskRegisterActivity.class).create().get());
    }

    @Test
    public void testInitializePresenter() {
        taskRegisterActivity.initializePresenter();
        assertNotNull(Whitebox.getInternalState(taskRegisterActivity, "presenter"));

    }


    @Test
    public void testGetRegisterFragment() {
        BaseRegisterFragment fragment = taskRegisterActivity.getRegisterFragment();
        assertNotNull(fragment);
    }


    @Test
    public void testGetOtherFragments() {
        Fragment[] fragments = taskRegisterActivity.getOtherFragments();
        assertEquals(1, fragments.length);
        assertNotNull(fragments[0]);
    }

    @Test
    public void testStartFormActivity() {
        JSONObject jsonObject = new JSONObject();
        Whitebox.setInternalState(taskRegisterActivity, "jsonFormUtils", jsonFormUtils);
        taskRegisterActivity.startFormActivity(jsonObject);
        verify(jsonFormUtils).startJsonForm(jsonObject, taskRegisterActivity);
    }

    @Test
    public void testOnActivityResultExtendedSavesForm() {
        taskRegisterActivity = spy(taskRegisterActivity);
        when(taskRegisterActivity.getPresenter()).thenReturn(presenter);
        Intent intent = new Intent();
        intent.putExtra(JSON_FORM_PARAM_JSON, "{formjson}");
        taskRegisterActivity.onActivityResultExtended(REQUEST_CODE_GET_JSON, Activity.RESULT_OK, intent);
        verify(presenter).saveJsonForm("{formjson}");
    }


    @Test
    public void testOnActivityResultCascadesToBaseFragment() {
        Whitebox.setInternalState(taskRegisterActivity, "mBaseFragment", mBaseFragment);
        Intent intent = new Intent();
        taskRegisterActivity.onActivityResultExtended(REQUEST_CODE_GET_JSON, Activity.RESULT_CANCELED, intent);
        verify(mBaseFragment).onActivityResult(REQUEST_CODE_GET_JSON, Activity.RESULT_CANCELED, intent);
    }


    @Test
    public void testGetViewIdentifiers() {
        assertEquals(1, taskRegisterActivity.getViewIdentifiers().size());
        assertEquals(TaskRegister.VIEW_IDENTIFIER, taskRegisterActivity.getViewIdentifiers().get(0));
    }

    @Test
    public void testRegisterBottomNavigationHidesBottonNav() {
        assertEquals(View.GONE, taskRegisterActivity.findViewById(R.id.bottom_navigation).getVisibility());
    }

    @Test
    public void testDisplayIndexCaseFragment() {
        taskRegisterActivity.getSupportFragmentManager().beginTransaction().add(0, taskRegisterActivity.getOtherFragments()[0]).commit();
        Whitebox.setInternalState(taskRegisterActivity, "mPager", mPager);
        JSONObject indexcase = new JSONObject();
        taskRegisterActivity.displayIndexCaseFragment(indexcase);
        verify(mPager).setCurrentItem(1, false);

    }

    @Test
    public void startFamilyRegistration() {
        TaskDetails taskDetails = TestingUtils.getTaskDetails();
        taskRegisterActivity.startFamilyRegistration(taskDetails);
        Intent intent = shadowOf(taskRegisterActivity).getNextStartedActivity();
        assertEquals(FamilyRegisterActivity.class, shadowOf(intent).getIntentClass());
        assertTrue(intent.getBooleanExtra(START_REGISTRATION, false));
        assertEquals(taskDetails.getStructureId(), intent.getStringExtra(Constants.Properties.LOCATION_UUID));
        assertEquals(taskDetails.getTaskId(), intent.getStringExtra(Constants.Properties.TASK_IDENTIFIER));
        assertEquals(taskDetails.getBusinessStatus(), intent.getStringExtra(Constants.Properties.TASK_BUSINESS_STATUS));
        assertEquals(taskDetails.getTaskStatus(), intent.getStringExtra(Constants.Properties.TASK_STATUS));

    }

}
