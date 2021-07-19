package org.smartregister.reveal.fragment;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.util.TestingUtils;

import static io.ona.kujaku.services.MapboxOfflineDownloaderService.SERVICE_ACTION.DELETE_MAP;
import static io.ona.kujaku.services.MapboxOfflineDownloaderService.SERVICE_ACTION.DOWNLOAD_MAP;
import static io.ona.kujaku.services.MapboxOfflineDownloaderService.SERVICE_ACTION.STOP_CURRENT_DOWNLOAD;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Richard Kareko on 1/31/20.
 */

public class BaseOfflineMapsFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private  Context context = RuntimeEnvironment.application;

    private BaseOfflineMapsFragment baseOfflineMapsFragment;

    @Before
    public void setUp() {
        baseOfflineMapsFragment = new AvailableOfflineMapsFragment();
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().start().get();
        activity.setContentView(R.layout.activity_offline_maps);
        activity.getSupportFragmentManager().beginTransaction().add(baseOfflineMapsFragment, "Base").commit();
    }

    @Test
    public void testHandleFailureResponseForMapDelete() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "serviceAction", DELETE_MAP);
        Whitebox.setInternalState(baseOfflineMapsFragment, "message", "Failed to delete map");

        baseOfflineMapsFragment.handleFailureResponse();

        assertEquals("Failed to delete map", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testHandleFailureResponse() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "message", "Network failure");

        baseOfflineMapsFragment.handleFailureResponse();

        assertEquals("Network failure", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void testHandleSuccessResponseForDownloadProgress() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "message", "67.12");
        Whitebox.setInternalState(baseOfflineMapsFragment, "mapUniqueName", TestingUtils.DUMMY_OPERATIONAL_AREA);
        Whitebox.setInternalState(baseOfflineMapsFragment, "serviceAction", DOWNLOAD_MAP);
        Whitebox.setInternalState(baseOfflineMapsFragment, "serviceAction", DOWNLOAD_MAP);

        baseOfflineMapsFragment.handleSuccessResponse();

        assertEquals("Downloading: 67.12 %", ShadowToast.getTextOfLatestToast());
        verify(baseOfflineMapsFragment).downloadStarted(stringArgumentCaptor.capture());
        assertEquals(TestingUtils.DUMMY_OPERATIONAL_AREA, stringArgumentCaptor.getValue());

    }

    @Test
    public void testHandleSuccessResponseForDownloadComplete() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "message", "100.00");
        Whitebox.setInternalState(baseOfflineMapsFragment, "mapUniqueName", TestingUtils.DUMMY_OPERATIONAL_AREA);

        baseOfflineMapsFragment.handleSuccessResponse();

        assertEquals(context.getString(R.string.download_finished_successfuly), ShadowToast.getTextOfLatestToast());
        verify(baseOfflineMapsFragment).downloadCompleted(stringArgumentCaptor.capture());
        assertEquals(TestingUtils.DUMMY_OPERATIONAL_AREA, stringArgumentCaptor.getValue());

    }

    @Test
    public void testHandleSuccessResponseForNonValidDouble() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "message", "Download success");
        Whitebox.setInternalState(baseOfflineMapsFragment, "mapUniqueName", TestingUtils.DUMMY_OPERATIONAL_AREA);

        baseOfflineMapsFragment.handleSuccessResponse();

        assertEquals("Download success", ShadowToast.getTextOfLatestToast());

    }

    @Test
    public void testHandleSuccessResponseForMapDelete() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "serviceAction", DELETE_MAP);
        Whitebox.setInternalState(baseOfflineMapsFragment, "mapUniqueName", TestingUtils.DUMMY_OPERATIONAL_AREA);

        baseOfflineMapsFragment.handleSuccessResponse();

        verify(baseOfflineMapsFragment).mapDeletedSuccessfully(stringArgumentCaptor.capture());
        assertEquals(TestingUtils.DUMMY_OPERATIONAL_AREA, stringArgumentCaptor.getValue());

    }

    @Test
    public void testHandleSuccessResponseForStoppedMapDownload() {
        baseOfflineMapsFragment = spy(baseOfflineMapsFragment);
        Whitebox.setInternalState(baseOfflineMapsFragment, "serviceAction", STOP_CURRENT_DOWNLOAD);
        Whitebox.setInternalState(baseOfflineMapsFragment, "mapUniqueName", TestingUtils.DUMMY_OPERATIONAL_AREA);

        baseOfflineMapsFragment.handleSuccessResponse();

        verify(baseOfflineMapsFragment).downloadStopped(stringArgumentCaptor.capture());
        assertEquals(TestingUtils.DUMMY_OPERATIONAL_AREA, stringArgumentCaptor.getValue());

    }

}
