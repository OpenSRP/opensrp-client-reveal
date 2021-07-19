package org.smartregister.reveal.interactor;

import android.content.Context;

import androidx.core.util.Pair;

import com.mapbox.mapboxsdk.offline.OfflineRegion;

import org.smartregister.domain.Location;
import org.smartregister.repository.LocationRepository;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.DownloadedOfflineMapsContract;
import org.smartregister.reveal.model.OfflineMapModel;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.OfflineMapHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.ona.kujaku.data.realm.RealmDatabase;
import io.ona.kujaku.data.realm.objects.MapBoxOfflineQueueTask;

public class DownloadedOfflineMapsInteractor implements DownloadedOfflineMapsContract.Interactor {

    private AppExecutors appExecutors;

    private LocationRepository locationRepository;

    private DownloadedOfflineMapsContract.Presenter presenter;

    private RealmDatabase realmDatabase;

    private Map<String, MapBoxOfflineQueueTask> offlineQueueTaskMap;

    public DownloadedOfflineMapsInteractor(DownloadedOfflineMapsContract.Presenter presenter, Context context) {
        this.presenter = presenter;
        appExecutors = RevealApplication.getInstance().getAppExecutors();
        locationRepository = RevealApplication.getInstance().getLocationRepository();
        realmDatabase = RevealApplication.getInstance().getRealmDatabase(context);
        offlineQueueTaskMap = new HashMap<>();
    }

    @Override
    public void fetchLocationsWithOfflineMapDownloads(final Pair<List<String>, Map<String, OfflineRegion>> offlineRegionInfo) {

        Runnable runnable = new Runnable() {
            public void run() {
                if (offlineRegionInfo == null || offlineRegionInfo.first == null) {
                    presenter.onOAsWithOfflineDownloadsFetched(null);
                    return;
                }

                List<Location> operationalAreas = locationRepository.getLocationsByIds(offlineRegionInfo.first);

                setOfflineQueueTaskMap(OfflineMapHelper.populateOfflineQueueTaskMap(realmDatabase));

                List<OfflineMapModel> offlineMapModels = populateOfflineMapModelList(operationalAreas, offlineRegionInfo.second);

                appExecutors.mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        presenter.onOAsWithOfflineDownloadsFetched(offlineMapModels);
                    }
                });
            }
        };

        appExecutors.diskIO().execute(runnable);

    }

    public List<OfflineMapModel>  populateOfflineMapModelList(List<Location> locations, Map<String, OfflineRegion> offlineRegionMap) {

        List<OfflineMapModel> offlineMapModels = new ArrayList<>();
        for (Location location: locations) {
            OfflineMapModel offlineMapModel = new OfflineMapModel();
            offlineMapModel.setLocation(location);
            offlineMapModel.setOfflineMapStatus(OfflineMapModel.OfflineMapStatus.DOWNLOADED);
            offlineMapModel.setOfflineRegion(offlineRegionMap.get(location.getId()));

            if (offlineQueueTaskMap.get(location.getId()) != null) {
                offlineMapModel.setDateCreated(offlineQueueTaskMap.get(location.getId()).getDateCreated());
            }

            offlineMapModels.add(offlineMapModel);
        }

        return offlineMapModels;
    }

    public void setOfflineQueueTaskMap(Map<String, MapBoxOfflineQueueTask> offlineQueueTaskMap) {
        this.offlineQueueTaskMap = offlineQueueTaskMap;
    }

}
