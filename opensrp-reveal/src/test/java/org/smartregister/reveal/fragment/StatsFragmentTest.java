package org.smartregister.reveal.fragment;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
import org.smartregister.reveal.presenter.StatsFragmentPresenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.smartregister.reveal.util.Constants.SyncInfo.INVALID_CLIENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.INVALID_EVENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.SYNCED_CLIENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.SYNCED_EVENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.TASK_UNPROCESSED_EVENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.UNSYNCED_CLIENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.UNSYNCED_EVENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.VALID_CLIENTS;
import static org.smartregister.reveal.util.Constants.SyncInfo.VALID_EVENTS;

/**
 * Created by Richard Kareko on 2/25/20.
 */

public class StatsFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private StatsFragmentPresenter presenter;

    private StatsFragment fragment;

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes= new ArrayList<>();
        fragment = new StatsFragment();
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().start().get();
        activity.setContentView(R.layout.activity_stats);
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "Stats").commit();
    }

    @Test
    public void testOnCreate() {
        assertNotNull(Whitebox.getInternalState(fragment,"presenter"));
    }

    @Test
    public void testOnCreateView() {
        Whitebox.setInternalState(fragment, "presenter", presenter);
        assertNotNull(Whitebox.getInternalState(fragment, "tvSyncedEvents"));
        assertNotNull(Whitebox.getInternalState(fragment, "tvSyncedClient"));
        assertNotNull(Whitebox.getInternalState(fragment, "tvUnSyncedClients"));
        assertNotNull(Whitebox.getInternalState(fragment, "tvValidatedEvents"));
        assertNotNull(Whitebox.getInternalState(fragment, "tvValidatedClients"));
        assertNotNull(Whitebox.getInternalState(fragment, "tvTaskUnprocessedEvents"));

    }

    @Test
    public void testRefreshSyncInfo() {

        Map<String, Integer> expectedSyncInfoMap = new HashMap<>();
        expectedSyncInfoMap.put(SYNCED_EVENTS, 1);
        expectedSyncInfoMap.put(SYNCED_CLIENTS, 2);
        expectedSyncInfoMap.put(UNSYNCED_EVENTS, 3);
        expectedSyncInfoMap.put(UNSYNCED_CLIENTS, 4);
        expectedSyncInfoMap.put(VALID_EVENTS, 5);
        expectedSyncInfoMap.put(INVALID_EVENTS, 6);
        expectedSyncInfoMap.put(VALID_CLIENTS, 7);
        expectedSyncInfoMap.put(INVALID_CLIENTS, 8);
        expectedSyncInfoMap.put(TASK_UNPROCESSED_EVENTS, 9);

        fragment.refreshECSyncInfo(expectedSyncInfoMap);

        assertEquals(expectedSyncInfoMap.get(SYNCED_EVENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvSyncedEvents")).getText());
        assertEquals(expectedSyncInfoMap.get(SYNCED_CLIENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvSyncedClient")).getText());
        assertEquals(expectedSyncInfoMap.get(UNSYNCED_CLIENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvUnSyncedClients")).getText());
        assertEquals(expectedSyncInfoMap.get(VALID_EVENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvValidatedEvents")).getText());
        assertEquals(expectedSyncInfoMap.get(VALID_CLIENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvValidatedClients")).getText());
        assertEquals(expectedSyncInfoMap.get(TASK_UNPROCESSED_EVENTS).toString(),  ((TextView)Whitebox.getInternalState(fragment, "tvTaskUnprocessedEvents")).getText());

    }

}
