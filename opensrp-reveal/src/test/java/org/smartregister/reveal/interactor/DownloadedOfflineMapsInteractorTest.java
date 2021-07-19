package org.smartregister.reveal.interactor;

import android.content.Context;

import androidx.core.util.Pair;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.domain.Location;
import org.smartregister.repository.LocationRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.DownloadedOfflineMapsContract;
import org.smartregister.reveal.model.OfflineMapModel;
import org.smartregister.reveal.util.TestingUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ona.kujaku.data.realm.RealmDatabase;
import io.ona.kujaku.data.realm.objects.MapBoxOfflineQueueTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by Richard Kareko on 1/24/20.
 */

public class DownloadedOfflineMapsInteractorTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private DownloadedOfflineMapsContract.Presenter presenter;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private RealmDatabase realmDatabase;

    @Captor
    private ArgumentCaptor<List<OfflineMapModel>> offlineMapModelListArgumentCaptor;

    private Context context = RuntimeEnvironment.application;

    private DownloadedOfflineMapsInteractor interactor;

    private String locationId;

    @Before
    public void setUp() {
        interactor = new DownloadedOfflineMapsInteractor(presenter, context);
        Whitebox.setInternalState(interactor, "locationRepository", locationRepository);
        Whitebox.setInternalState(interactor, "realmDatabase", realmDatabase);
        locationId = "location_1";
    }

    @Test
    public void testFetchLocationsWithOfflineMapDownloadsWithNullParams() {
        interactor.fetchLocationsWithOfflineMapDownloads(null);
        verify(presenter, timeout(ASYNC_TIMEOUT)).onOAsWithOfflineDownloadsFetched(null);
        verifyNoMoreInteractions(presenter);
        verifyNoMoreInteractions(locationRepository);
    }

    @Test
    public void testFetchLocationsWithOfflineMapDownloads() throws Exception{

        List<String> locationIds = Collections.singletonList(locationId);
        List<Location> locations = Collections.singletonList(initLocation());
        Pair<List<String>, Map<String, OfflineRegion>> offlineRegionInfo = TestingUtils.getOfflineRegionInfo();
        List<OfflineMapModel> expectedOfflineMapModelList = Collections.singletonList(TestingUtils.getOfflineMapModel());

        interactor = spy(interactor);

        Whitebox.setInternalState(interactor, "offlineQueueTaskMap", TestingUtils.getOfflineQueueTaskMap());
        doNothing().when(interactor).setOfflineQueueTaskMap(anyMap());
        when(locationRepository.getLocationsByIds(locationIds, false)).thenReturn(locations);
        when(interactor.populateOfflineMapModelList(anyList(), anyMap())).thenReturn(expectedOfflineMapModelList);

        interactor.fetchLocationsWithOfflineMapDownloads(offlineRegionInfo);

        verify(presenter, timeout(ASYNC_TIMEOUT)).onOAsWithOfflineDownloadsFetched(offlineMapModelListArgumentCaptor.capture());
        assertNotNull(offlineMapModelListArgumentCaptor.getValue());
        List<OfflineMapModel> actualOfflineMapModelList = offlineMapModelListArgumentCaptor.getValue();
        assertFalse(actualOfflineMapModelList.isEmpty());
        assertEquals(expectedOfflineMapModelList.get(0).getDownloadAreaId(), actualOfflineMapModelList.get(0).getDownloadAreaId());

    }

    @Test
    public void testPopulateOfflineMapModelList() throws Exception{

        List<Location> locations = Collections.singletonList(initLocation());
        Map<String, OfflineRegion> offlineRegionMap = new HashMap<>();
        OfflineRegion expectedOfflineRegion = TestingUtils.createMockOfflineRegion();
        offlineRegionMap.put(locationId, expectedOfflineRegion);

        Map<String, MapBoxOfflineQueueTask> expectedOfflineQueueTaskMap = TestingUtils.getOfflineQueueTaskMap();

        interactor = spy(interactor);
        Whitebox.setInternalState(interactor, "offlineQueueTaskMap", expectedOfflineQueueTaskMap);

        List<OfflineMapModel> actualOfflineMapModelList = interactor.populateOfflineMapModelList(locations, offlineRegionMap);

        assertNotNull(actualOfflineMapModelList);
        assertFalse(actualOfflineMapModelList.isEmpty());
        assertEquals(locationId, actualOfflineMapModelList.get(0).getLocation().getId());
        assertEquals("Polygon", actualOfflineMapModelList.get(0).getLocation().getType());
        assertTrue(actualOfflineMapModelList.get(0).getLocation().isJurisdiction());
        assertEquals(expectedOfflineQueueTaskMap.get(locationId).getDateCreated(), actualOfflineMapModelList.get(0).getDateCreated());

    }

    @Test
    public void testSetOfflineQueueTaskMap() throws Exception {

        Map<String, MapBoxOfflineQueueTask> originalOfflineQueueTaskMap = Whitebox.getInternalState(interactor, "offlineQueueTaskMap");
        assertTrue(originalOfflineQueueTaskMap.isEmpty());

        Map<String, MapBoxOfflineQueueTask> expectedOfflineQueueTaskMap = TestingUtils.getOfflineQueueTaskMap();

        interactor.setOfflineQueueTaskMap(expectedOfflineQueueTaskMap);

        Map<String, MapBoxOfflineQueueTask> actualOfflineQueueTaskMap = Whitebox.getInternalState(interactor, "offlineQueueTaskMap");
        assertFalse(actualOfflineQueueTaskMap.isEmpty());
        assertEquals(1, actualOfflineQueueTaskMap.size());
        assertEquals(expectedOfflineQueueTaskMap.get("location_1").getDateCreated(), actualOfflineQueueTaskMap.get("location_1").getDateCreated());

    }

    private Location initLocation() {
        Location location = new Location();
        location.setType("Polygon");
        location.setId(locationId);
        location.setJurisdiction(true);
        return location;
    }

}
