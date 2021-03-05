package org.smartregister.reveal.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.hamcrest.CoreMatchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Location;
import org.smartregister.domain.ResponseErrorStatus;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.reporting.view.ProgressIndicatorView;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.BaseDrawerContract;
import org.smartregister.reveal.contract.FormProcessor;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.presenter.ChildRegisterFragmentPresenter;
import org.smartregister.reveal.shadow.DrawerMenuViewShadow;
import org.smartregister.reveal.shadow.SyncStatusBroadcastReceiverShadowHelper;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.view.ChildProfileActivity;
import org.smartregister.reveal.view.ChildRegisterActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.reveal.util.Constants.JsonForm.ENCOUNTER_TYPE;


/**
 * @author ronald
 */
@Config(shadows = {DrawerMenuViewShadow.class, SyncStatusBroadcastReceiverShadowHelper.class})
public class ChildRegisterFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    private ChildRegisterActivity activity;

    private ChildRegisterFragment fragment;

    @Mock
    private ChildRegisterFragmentPresenter presenter;

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        fragment = new ChildRegisterFragment();
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
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "drawerView"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "view"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "tvTitle"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "progressIndicatorsGroupView"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "detailedReportCardView"));
        Assert.assertNotNull(ReflectionHelpers.getField(fragment, "searchTextView"));
    }


    @Test
    public void testOnListItemClickedWithNoStructureSet() {
        fragment = Mockito.spy(fragment);

        // click when no structure is present
        BaseDrawerContract.View drawerView = Mockito.mock(BaseDrawerContract.View.class);
        Whitebox.setInternalState(fragment, drawerView);
        fragment.onListItemClicked(null, -1);
        Mockito.verify(drawerView).openDrawerLayout();
    }

    @Test
    public void testOnListItemClickedOpensMDAForm() {
        fragment = Mockito.spy(fragment);

        // prepare data
        Location location = Mockito.mock(Location.class);
        Mockito.doReturn(location).when(fragment).getCurrentStructure();

        Child child = new Child();
        child.setBaseEntityID("12345");

        // starts form
        fragment.onListItemClicked(child, R.id.linearLayoutAction);
        Mockito.verify(fragment).startRecordMDAForm("12345");
    }

    @Test
    public void testOnListItemClickedOpensProfile() {
        fragment = Mockito.spy(fragment);

        // prepare data
        Location location = Mockito.mock(Location.class);
        Mockito.doReturn(location).when(fragment).getCurrentStructure();

        Child child = new Child();
        child.setBaseEntityID("12345");

        // opens profile
        fragment.onListItemClicked(child, -1);

        ShadowActivity shadowActivity = shadowOf(activity);
        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        Assert.assertThat(shadowIntent.getIntentClass().getName(),
                CoreMatchers.equalTo(ChildProfileActivity.class.getName()));
    }

    @Test
    public void testOnFetchErrorDisplaysError() {
        Exception exception = Mockito.mock(Exception.class);
        fragment.onFetchError(exception);
        Mockito.verify(exception).getMessage();
    }

    @Test
    public void testOnDrawerClosedSetsTitle() {
        TextView tvTitle = Mockito.mock(TextView.class);
        Whitebox.setInternalState(fragment, "tvTitle", tvTitle);

        BaseDrawerContract.View drawerView = Mockito.mock(BaseDrawerContract.View.class);
        Mockito.doReturn("Area").when(drawerView).getOperationalArea();
        Whitebox.setInternalState(fragment, drawerView);


        fragment.onDrawerClosed();
        Mockito.verify(tvTitle).setText("Area");
    }

    @Test
    public void testOpenFilterFragmentStartsFragment() {
        fragment = Mockito.spy(fragment);
        FragmentActivity activity = Mockito.mock(FragmentActivity.class);
        Mockito.doReturn(activity).when(fragment).getActivity();

        fragment.openFilterFragment();
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        Mockito.verify(activity).startActivity(captor.capture());

        Bundle bundle = captor.getValue().getExtras();
        Assert.assertEquals(ChildFilterFragment.TAG, bundle.getString(ChildRegisterActivity.DISPLAY_FRAGMENT));
    }

    @Test
    public void testStartChildRegistrationForm() {
        Whitebox.setInternalState(fragment, presenter);

        fragment.startChildRegistrationForm();
        Mockito.verify(presenter).startChildRegistrationForm(Mockito.any());
    }

    @Test
    public void testStartRecordMDAForm() {
        Whitebox.setInternalState(fragment, presenter);

        fragment.startRecordMDAForm("12345");
        Mockito.verify(presenter).startMDAForm(Mockito.any(), Mockito.eq("12345"));
    }

    @Test
    public void testStartJsonFormForwardsToHost() {
        fragment = Mockito.spy(fragment);
        FormProcessor.Host host = Mockito.mock(FormProcessor.Host.class);
        Mockito.doReturn(host).when(fragment).getHostFormProcessor();

        fragment.startJsonForm(new JSONObject(), "title");
        Mockito.verify(host).startForm(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testReloadFromSource() {
        Whitebox.setInternalState(fragment, presenter);

        fragment.reloadFromSource();

        Mockito.verify(presenter).search(Mockito.any(), Mockito.any());
        Mockito.verify(presenter).fetchReportStats();
    }

    @Test
    public void testOnReportCountReloaded() {
        Map<String, Integer> reportCounts = new HashMap<>();
        reportCounts.put(Constants.ChildRegister.MMA_VISITED_AND_ADMINISTERED, 20);
        reportCounts.put(Constants.ChildRegister.MMA_VISITED_NOT_ADMINISTERED, 8);
        reportCounts.put(Constants.ChildRegister.MMA_NOT_VISITED, 5);
        reportCounts.put(Constants.ChildRegister.MMA_TARGET_REMAINING, 10);
        reportCounts.put(Constants.ChildRegister.MMA_COVERAGE, 10);

        View progressIndicatorsGroupView = Mockito.mock(View.class);
        Whitebox.setInternalState(fragment, "progressIndicatorsGroupView", progressIndicatorsGroupView);

        ProgressIndicatorView progressIndicatorView = Mockito.mock(ProgressIndicatorView.class);
        Mockito.doReturn(progressIndicatorView).when(progressIndicatorsGroupView).findViewById(Mockito.anyInt());

        View detailedReportCardView = Mockito.mock(View.class);
        Whitebox.setInternalState(fragment, "detailedReportCardView", detailedReportCardView);

        TextView tvDoseRecordedAndDrugsGiven = Mockito.mock(TextView.class);
        Mockito.doReturn(tvDoseRecordedAndDrugsGiven).when(detailedReportCardView).findViewById(R.id.tvDoseRecordedAndDrugsGiven);

        TextView tvChildrenNotAdministeredMedicine = Mockito.mock(TextView.class);
        Mockito.doReturn(tvChildrenNotAdministeredMedicine).when(detailedReportCardView).findViewById(R.id.tvChildrenNotAdministeredMedicine);

        TextView tvTotalChildrenRemaining = Mockito.mock(TextView.class);
        Mockito.doReturn(tvTotalChildrenRemaining).when(detailedReportCardView).findViewById(R.id.tvTotalChildrenRemaining);

        TextView tvChildrenRemainingUntilTarget = Mockito.mock(TextView.class);
        Mockito.doReturn(tvChildrenRemainingUntilTarget).when(detailedReportCardView).findViewById(R.id.tvChildrenRemainingUntilTarget);

        fragment.onReportCountReloaded(reportCounts);
        Mockito.verify(progressIndicatorView).setTitle(Mockito.anyString());
        Mockito.verify(progressIndicatorView).setProgress(10);

        Mockito.verify(tvDoseRecordedAndDrugsGiven).setText("20");
        Mockito.verify(tvChildrenNotAdministeredMedicine).setText("8");
        Mockito.verify(tvTotalChildrenRemaining).setText("5");
        Mockito.verify(tvChildrenRemainingUntilTarget).setText("10");
    }

    @Test
    public void testToggleDetailedReport() {
        View detailedReportCardView = Mockito.mock(View.class);
        Mockito.doReturn(View.VISIBLE).when(detailedReportCardView).getVisibility();

        Whitebox.setInternalState(fragment, "detailedReportCardView", detailedReportCardView);

        fragment.toggleDetailedReport();
        Mockito.verify(detailedReportCardView).setVisibility(View.GONE);


        Mockito.doReturn(View.GONE).when(detailedReportCardView).getVisibility();
        fragment.toggleDetailedReport();
        Mockito.verify(detailedReportCardView).setVisibility(View.VISIBLE);
    }

    @Test
    public void testOnResumeReloadsData() {
        fragment = Mockito.spy(fragment);
        fragment.onResume();
        Mockito.verify(fragment).reloadFromSource();
    }

    @Test
    public void testOnPause() {
        SyncStatusBroadcastReceiver receiverShadow = SyncStatusBroadcastReceiverShadowHelper.getInstance();
        fragment.onPause();
        Mockito.verify(receiverShadow).removeSyncStatusListener(Mockito.any());
    }

    @Test
    public void testSyncEvents() {
        fragment = Mockito.spy(fragment);

        // sync start
        fragment.onSyncStart();
        Mockito.verify(fragment).refreshSyncStatusViews(null);

        fragment.onSyncInProgress(FetchStatus.fetchProgress);
        Mockito.verify(fragment).refreshSyncStatusViews(FetchStatus.fetchProgress);

        fragment.onSyncComplete(FetchStatus.fetched);
        Mockito.verify(fragment).refreshSyncStatusViews(FetchStatus.fetched);
    }

    @Test
    public void testRefreshSyncStatusViewsFetchFailedMalformed() {
        fragment = Mockito.spy(fragment);

        FetchStatus fetchStatus = FetchStatus.fetchedFailed;
        fetchStatus.setDisplayValue(ResponseErrorStatus.malformed_url.name());
        fragment.refreshSyncStatusViews(fetchStatus);
        Mockito.verify(fragment, Mockito.times(2)).getString(org.smartregister.R.string.sync_failed_malformed_url);
    }

    @Test
    public void testRefreshSyncStatusViewsFetchFailedTimeout() {
        fragment = Mockito.spy(fragment);

        FetchStatus fetchStatus = FetchStatus.fetchedFailed;
        fetchStatus.setDisplayValue(ResponseErrorStatus.timeout.name());
        fragment.refreshSyncStatusViews(fetchStatus);
        Mockito.verify(fragment, Mockito.times(2)).getString(org.smartregister.R.string.sync_failed_timeout_error);
    }

    @Test
    public void testRefreshSyncStatusViewsFetchFailed() {
        fragment = Mockito.spy(fragment);

        FetchStatus fetchStatus = FetchStatus.fetchedFailed;
        fetchStatus.setDisplayValue(ResponseErrorStatus.not_found.name());
        fragment.refreshSyncStatusViews(fetchStatus);
        Mockito.verify(fragment, Mockito.times(2)).getString(org.smartregister.R.string.sync_failed);
    }

    @Test
    public void testRefreshSyncStatusViewsFetched() {
        fragment = Mockito.spy(fragment);
        FetchStatus fetchStatus = FetchStatus.fetched;

        fetchStatus.setDisplayValue(null);
        fragment.refreshSyncStatusViews(fetchStatus);
        Mockito.verify(fragment, Mockito.times(2)).getString(org.smartregister.R.string.sync_complete);
    }

    @Test
    public void testRefreshSyncStatusNoConnection() {
        fragment = Mockito.spy(fragment);
        FetchStatus fetchStatus = FetchStatus.noConnection;

        fetchStatus.setDisplayValue(null);
        fragment.refreshSyncStatusViews(fetchStatus);
        Mockito.verify(fragment, Mockito.times(2)).getString(org.smartregister.R.string.sync_failed_no_internet);
    }

    @Test
    public void testOnFormProcessingResultReceivedForm() throws JSONException {
        Whitebox.setInternalState(fragment, presenter);

        JSONObject jsonForm = new JSONObject();

        jsonForm.put(ENCOUNTER_TYPE, Constants.EventType.CHILD_REGISTRATION);
        fragment.onFormProcessingResult(jsonForm.toString());
        Mockito.verify(presenter).saveChild(Mockito.anyString(), Mockito.any());

        jsonForm.put(ENCOUNTER_TYPE, Constants.EventType.MDA_DISPENSE);
        fragment.onFormProcessingResult(jsonForm.toString());
        Mockito.verify(presenter).saveMDAForm(Mockito.anyString(), Mockito.any());
    }
}
