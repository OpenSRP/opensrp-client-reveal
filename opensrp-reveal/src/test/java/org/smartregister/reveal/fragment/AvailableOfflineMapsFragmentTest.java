package org.smartregister.reveal.fragment;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.widget.CheckBox;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.domain.Location;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.R;
import org.smartregister.reveal.adapter.AvailableOfflineMapAdapter;
import org.smartregister.reveal.contract.OfflineMapDownloadCallback;
import org.smartregister.reveal.model.OfflineMapModel;
import org.smartregister.reveal.presenter.AvailableOfflineMapsPresenter;
import org.smartregister.reveal.util.TestingUtils;

import java.util.ArrayList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.smartregister.reveal.model.OfflineMapModel.OfflineMapStatus.DOWNLOADED;
import static org.smartregister.reveal.model.OfflineMapModel.OfflineMapStatus.DOWNLOAD_STARTED;
import static org.smartregister.reveal.model.OfflineMapModel.OfflineMapStatus.READY;

/**
 * Created by Richard Kareko on 1/27/20.
 */

public class AvailableOfflineMapsFragmentTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private AvailableOfflineMapsPresenter presenter;

    @Mock
    private AvailableOfflineMapAdapter adapter;

    @Mock
    private OfflineMapDownloadCallback callback;

    @Captor
    private ArgumentCaptor<List<OfflineMapModel>> offlineMapModelListCaptor;

    @Captor
    private ArgumentCaptor<List<String>> stringListCaptor;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private AvailableOfflineMapsFragment fragment;

    private Context context = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes= new ArrayList<>();
        fragment = new AvailableOfflineMapsFragment();
        AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).create().start().get();
        activity.setContentView(R.layout.activity_offline_maps);
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "Available").commit();
    }

    @Test
    public void testOnCreate() {
        assertNotNull(Whitebox.getInternalState(fragment, "presenter"));
    }

    @Test
    public void testOnCreateView() {
        assertNotNull(Whitebox.getInternalState(fragment, "offlineMapRecyclerView"));
        assertNotNull(Whitebox.getInternalState(fragment, "adapter"));
        assertNotNull(Whitebox.getInternalState(fragment, "offlineMapModelList"));
    }

    @Test
    public void testSetOfflineMapModelList() {

        Whitebox.setInternalState(fragment, "adapter", adapter);

        OfflineMapModel model = TestingUtils.getOfflineMapModel();
        List<OfflineMapModel> expectedOfflineMapModels = Collections.singletonList(model);
        fragment.setOfflineMapModelList(expectedOfflineMapModels);

        verify(adapter).setOfflineMapModels(offlineMapModelListCaptor.capture());
        assertNotNull(offlineMapModelListCaptor.getValue());

        List<OfflineMapModel> actualOfflineMapModels = offlineMapModelListCaptor.getValue();
        assertFalse(actualOfflineMapModels.isEmpty());
        assertEquals(expectedOfflineMapModels.get(0).getOfflineMapStatus(), actualOfflineMapModels.get(0).getOfflineMapStatus());
        assertEquals(expectedOfflineMapModels.get(0).getLocation().getId(), actualOfflineMapModels.get(0).getLocation().getId());

    }

    @Test
    public void testUpdateOperationalAreasToDownload() {

        List<Location> originalOperationalAreasToDownload = (Whitebox.getInternalState(fragment, "operationalAreasToDownload"));
        assertNotNull(originalOperationalAreasToDownload);
        assertTrue(originalOperationalAreasToDownload.isEmpty());
        CheckBox checkBox = new CheckBox(context);
        checkBox.setChecked(true);

        OfflineMapModel expectedOfflineMapModel = TestingUtils.getOfflineMapModel();
        Location expectedLocation = expectedOfflineMapModel.getLocation();
        checkBox.setTag(R.id.offline_map_checkbox, expectedOfflineMapModel);

        fragment.updateOperationalAreasToDownload(checkBox);

        List<Location> updatedOperationalAreasToDownload = (Whitebox.getInternalState(fragment, "operationalAreasToDownload"));
        assertNotNull(updatedOperationalAreasToDownload);
        assertFalse(updatedOperationalAreasToDownload.isEmpty());
        assertEquals(1, updatedOperationalAreasToDownload.size());
        assertEquals(expectedLocation.getId(), updatedOperationalAreasToDownload.get(0).getId());
    }

    @Test
    public void testDisableCheckBox() {

        List<OfflineMapModel> originalOfflineMapModelList = initializeOfflineMapModelList();
        assertEquals(READY, originalOfflineMapModelList.get(0).getOfflineMapStatus());

        fragment.disableCheckBox(originalOfflineMapModelList.get(0).getDownloadAreaId());

        List<OfflineMapModel> actualOfflineMapModelList = (Whitebox.getInternalState(fragment, "offlineMapModelList"));
        assertNotNull(actualOfflineMapModelList);
        assertFalse(actualOfflineMapModelList.isEmpty());
        assertEquals(DOWNLOAD_STARTED, actualOfflineMapModelList.get(0).getOfflineMapStatus());

    }

    @Test (expected = UnsupportedOperationException.class)
    public void testMoveDownloadedOAToDownloadedList() {
        Whitebox.setInternalState(fragment, "callback", callback);
        List<OfflineMapModel> originalOfflineMapModelList = initializeOfflineMapModelList();
        assertEquals(READY, originalOfflineMapModelList.get(0).getOfflineMapStatus());

        fragment.moveDownloadedOAToDownloadedList(originalOfflineMapModelList.get(0).getDownloadAreaId());

        List<OfflineMapModel> actualOfflineMapModelList = (Whitebox.getInternalState(fragment, "offlineMapModelList"));
        assertNotNull(actualOfflineMapModelList);
        assertFalse(actualOfflineMapModelList.isEmpty());
        assertEquals(DOWNLOADED, actualOfflineMapModelList.get(0).getOfflineMapStatus());

    }

    @Test
    public void testSetOfflineMapDownloadCallback() {
        OfflineMapDownloadCallback originalCallback = Whitebox.getInternalState(fragment, "callback");
        assertNull(originalCallback);

        fragment.setOfflineMapDownloadCallback(callback);

        OfflineMapDownloadCallback updatedCallback = Whitebox.getInternalState(fragment, "callback");
        assertNotNull(updatedCallback);

    }

    @Test
    public void testDownloadCompleted() {
        Whitebox.setInternalState(fragment, "presenter", presenter);
        fragment.downloadCompleted("Akros_1");
        verify(presenter).onDownloadComplete(stringArgumentCaptor.capture());
        assertNotNull(stringArgumentCaptor.getValue());
        assertEquals("Akros_1", stringArgumentCaptor.getValue());

    }

    @Test
    public void testDownloadStarted() {
        Whitebox.setInternalState(fragment, "presenter", presenter);
        fragment.downloadStarted("Akros_1");
        verify(presenter).onDownloadStarted(stringArgumentCaptor.capture());
        assertNotNull(stringArgumentCaptor.getValue());
        assertEquals("Akros_1", stringArgumentCaptor.getValue());

    }

    @Test
    public void testUpdateOperationalAreasToDownloadOfflineModel() {
        List<OfflineMapModel> originalOfflineMapModelList = (Whitebox.getInternalState(fragment, "offlineMapModelList"));
        assertTrue(originalOfflineMapModelList.isEmpty());

        OfflineMapModel expectedOfflineMapModel = TestingUtils.getOfflineMapModel();

        fragment.updateOperationalAreasToDownload(expectedOfflineMapModel);
        List<OfflineMapModel> updatedOfflineMapModelList = (Whitebox.getInternalState(fragment, "offlineMapModelList"));
        assertFalse(originalOfflineMapModelList.isEmpty());
        assertEquals(1, updatedOfflineMapModelList.size());
        assertEquals(expectedOfflineMapModel.getLocation().getId(), updatedOfflineMapModelList.get(0).getLocation().getId());
    }

    @Test
    public void testSetOfflineDownloadedMapNames() {
        Whitebox.setInternalState(fragment, "presenter", presenter);
        List<String> expectedOfflineRegionNames = Collections.singletonList("Akros_1");
        fragment.setOfflineDownloadedMapNames(expectedOfflineRegionNames);

        verify(presenter).fetchAvailableOAsForMapDownLoad(stringListCaptor.capture());
        assertNotNull(stringListCaptor.getValue());
        assertEquals(expectedOfflineRegionNames.get(0), stringListCaptor.getValue().get(0));
    }

    private List<OfflineMapModel> initializeOfflineMapModelList() {
        OfflineMapModel originalOfflineMapModel = TestingUtils.getOfflineMapModel();
        originalOfflineMapModel.setOfflineMapStatus(READY);

        List<OfflineMapModel> OfflineMapModelList = Collections.singletonList(originalOfflineMapModel);
        Whitebox.setInternalState(fragment, "offlineMapModelList", OfflineMapModelList);

        List<OfflineMapModel> originalOfflineMapModelList = (Whitebox.getInternalState(fragment, "offlineMapModelList"));
        assertNotNull(originalOfflineMapModelList);
        assertFalse(originalOfflineMapModelList.isEmpty());
        return OfflineMapModelList;
    }


}
