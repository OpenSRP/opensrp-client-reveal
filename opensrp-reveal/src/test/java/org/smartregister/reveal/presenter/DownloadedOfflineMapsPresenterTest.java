package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.core.util.Pair;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.DownloadedOfflineMapsContract;
import org.smartregister.reveal.interactor.DownloadedOfflineMapsInteractor;
import org.smartregister.reveal.model.OfflineMapModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.mockito.Mockito.verify;

/**
 * @author Richard Kareko
 */
public class DownloadedOfflineMapsPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DownloadedOfflineMapsContract.View view;

    @Mock
    private DownloadedOfflineMapsInteractor interactor;

    private Context context = RuntimeEnvironment.application;

    private DownloadedOfflineMapsPresenter presenter;

    @Before
    public void setUp() {
        presenter = new DownloadedOfflineMapsPresenter(view, context);
        Whitebox.setInternalState(presenter, "interactor", interactor);
    }

    @Test
    public void testOnDeleteDownloadMap() {

        presenter.onDeleteDownloadMap(Collections.singletonList(OfflineMapModel.class));
        verify(view).deleteDownloadedOfflineMaps();

    }

    @Test
    public void testFetchOAsWithOfflineDownloads() {

        Pair<List<String>, Map<String, OfflineRegion>>  offlineRegionsInfo =
                new Pair(Collections.singletonList("test"), new HashMap<>());

        presenter.fetchOAsWithOfflineDownloads(offlineRegionsInfo);
        verify(interactor).fetchLocationsWithOfflineMapDownloads(offlineRegionsInfo);

    }

    @Test
    public void testOnOAsWithOfflineDownloadsFetched() {

        presenter.onOAsWithOfflineDownloadsFetched(Collections.singletonList(OfflineMapModel.class));
        verify(view).setDownloadedOfflineMapModelList(Collections.singletonList(OfflineMapModel.class));

    }

}
