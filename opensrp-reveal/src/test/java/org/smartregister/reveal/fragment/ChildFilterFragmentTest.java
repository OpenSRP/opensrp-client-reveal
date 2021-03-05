package org.smartregister.reveal.fragment;

import android.content.Intent;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.ChildFilterFragmentContract;
import org.smartregister.reveal.shadow.DrawerMenuViewShadow;
import org.smartregister.reveal.shadow.SyncStatusBroadcastReceiverShadowHelper;
import org.smartregister.reveal.view.ChildRegisterActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Arrays;
import timber.log.Timber;

import static org.robolectric.Shadows.shadowOf;

/**
 * @author ronald
 */
@Config(shadows = {DrawerMenuViewShadow.class, SyncStatusBroadcastReceiverShadowHelper.class})
public class ChildFilterFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    private ChildRegisterActivity activity;

    private ChildFilterFragment fragment;

    @Mock
    private ChildFilterFragmentContract.Presenter presenter;

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        fragment = new ChildFilterFragment();
        Whitebox.setInternalState(fragment, "presenter", presenter);
        activity = Robolectric.buildActivity(ChildRegisterActivity.class).create().start().get();
        activity.getSupportFragmentManager().beginTransaction().add(0, fragment).commit();
    }

    @After
    public void tearDown() {
        try {
            activity.finish();
        } catch (Exception e) {
            Timber.e(e);
        }

        System.gc();
    }

    @Test
    public void testBindLayoutAvailsAllLayouts() {
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "radioGradeName"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "radioGradeAge"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "radioAge"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "view"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "progressBar"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "linearLayoutGrades"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "linearLayoutAges"));
    }

    @Test
    public void testGetFilterValues() {

        RadioButton radioGradeName = ReflectionHelpers.getField(fragment, "radioGradeName");
        radioGradeName.setChecked(true);
        Whitebox.setInternalState(fragment, "radioGradeName", radioGradeName);

        RadioButton radioGradeAge = ReflectionHelpers.getField(fragment, "radioGradeAge");
        radioGradeAge.setChecked(true);
        Whitebox.setInternalState(fragment, "radioGradeAge", radioGradeAge);

        RadioButton radioAge = ReflectionHelpers.getField(fragment, "radioAge");
        radioAge.setChecked(true);
        Whitebox.setInternalState(fragment, "radioAge", radioAge);

        LinearLayout linearLayoutGrades = ReflectionHelpers.getField(fragment, "linearLayoutGrades");
        CheckBox checkBox = new CheckBox(RuntimeEnvironment.application);
        checkBox.setText("1A");
        checkBox.setChecked(true);
        linearLayoutGrades.addView(checkBox);
        Whitebox.setInternalState(fragment, "linearLayoutGrades", linearLayoutGrades);

        LinearLayout linearLayoutAges = ReflectionHelpers.getField(fragment, "linearLayoutAges");
        CheckBox checkBoxAge = new CheckBox(RuntimeEnvironment.application);
        checkBoxAge.setText("Sample");
        checkBoxAge.setChecked(true);
        linearLayoutAges.addView(checkBoxAge);
        Whitebox.setInternalState(fragment, "linearLayoutAges", linearLayoutAges);

        HashMap<String, List<String>> result = fragment.getFilterValues();
        Assert.assertEquals(result.size(), 3);
    }

    @Test
    public void testClearFilters() {

        RadioButton radioGradeName = Mockito.mock(RadioButton.class);
        ReflectionHelpers.setField(fragment, "radioGradeName", radioGradeName);

        fragment.clearFilters();
        Mockito.verify(radioGradeName).setChecked(true);
    }

    @Test
    public void testOnGradesFetched() {
        List<String> grades = Arrays.asList(new String[]{"1A", "2B", "3C", "4D"});
        fragment.onGradesFetched(grades);
        LinearLayout linearLayoutGrades = ReflectionHelpers.getField(fragment, "linearLayoutGrades");
        Assert.assertEquals(linearLayoutGrades.getChildCount(), 4);
    }

    @Test
    public void testExecuteFilter() {
        fragment.executeFilter();

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        Assert.assertThat(shadowIntent.getIntentClass().getName(),
                CoreMatchers.equalTo(ChildRegisterActivity.class.getName()));
    }
}
