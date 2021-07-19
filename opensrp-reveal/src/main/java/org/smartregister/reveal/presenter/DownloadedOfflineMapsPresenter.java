package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.core.util.Pair;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.smartregister.reveal.contract.DownloadedOfflineMapsContract;
import org.smartregister.reveal.interactor.DownloadedOfflineMapsInteractor;
import org.smartregister.reveal.model.OfflineMapModel;

import java.util.List;
import java.util.Map;

public class DownloadedOfflineMapsPresenter implements DownloadedOfflineMapsContract.Presenter {

    private DownloadedOfflineMapsContract.View view;
    private DownloadedOfflineMapsContract.Interactor interactor;

    public DownloadedOfflineMapsPresenter(DownloadedOfflineMapsContract.View view, Context context) {
        this.view = view;
        this.interactor = new DownloadedOfflineMapsInteractor(this, context);
    }

    @Override
    public void onDeleteDownloadMap(List<OfflineMapModel> offlineMapModels) {
        view.deleteDownloadedOfflineMaps();
    }

    @Override
    public void fetchOAsWithOfflineDownloads(Pair<List<String>, Map<String, OfflineRegion>> offlineRegionInfo) {
        interactor.fetchLocationsWithOfflineMapDownloads(offlineRegionInfo);
    }

    @Override
    public void onOAsWithOfflineDownloadsFetched(List<OfflineMapModel> downloadedOfflineMapModelList) {
        view.setDownloadedOfflineMapModelList(downloadedOfflineMapModelList);
    }
}
