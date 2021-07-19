package org.smartregister.reveal.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.OfflineMapsFragmentContract;

import io.ona.kujaku.services.MapboxOfflineDownloaderService;
import io.ona.kujaku.utils.Constants;
import timber.log.Timber;

public abstract class BaseOfflineMapsFragment extends Fragment implements OfflineMapsFragmentContract.View {

    private MapDownloadReceiver mapDownloadReceiver = new MapDownloadReceiver();

    private String mapUniqueName;
    private String resultStatus;
    private String message;
    private MapboxOfflineDownloaderService.SERVICE_ACTION serviceAction;

    protected String currentMapDownload;

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(Constants.INTENT_ACTION_MAP_DOWNLOAD_SERVICE_STATUS_UPDATES);
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(mapDownloadReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).unregisterReceiver(mapDownloadReceiver);
    }

    @Override
    public void displayToast(String message) {
        Toast.makeText(RevealApplication.getInstance(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayError(int title, String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    public class MapDownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Timber.i( intent.getExtras().toString());
                if (bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULT_STATUS)
                        && bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE)
                        && bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION)
                        && bundle.containsKey(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME)) {

                    mapUniqueName = bundle.getString(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME);
                    resultStatus = bundle.getString(MapboxOfflineDownloaderService.KEY_RESULT_STATUS);
                    serviceAction = (MapboxOfflineDownloaderService.SERVICE_ACTION) bundle.get(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION);

                    message = bundle.getString(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE);

                    if (MapboxOfflineDownloaderService.SERVICE_ACTION_RESULT.FAILED.name().equals(resultStatus)) {
                        handleFailureResponse();
                    } else {
                        handleSuccessResponse();
                    }
                }
            } else {
                Timber.i( "Broadcast message has null Extras");
            }

        }
    }

    public void handleFailureResponse() {
        if (!TextUtils.isEmpty(message) &&
                (!message.contains("MapBox Tile Count limit exceeded")
                        || serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.DELETE_MAP)) {
            displayError(R.id.download_map,  message);
        }

    }

    public void handleSuccessResponse() {
        // We should disable the stop offline download button if it was stopped successfully
        if (serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.STOP_CURRENT_DOWNLOAD) {
            currentMapDownload = null;
            downloadStopped(mapUniqueName);
        } else if (serviceAction == MapboxOfflineDownloaderService.SERVICE_ACTION.DELETE_MAP) {
            mapDeletedSuccessfully(mapUniqueName);
        } else {
            if (!TextUtils.isEmpty(message)) {
                // This is a download progress message
                if (isValidDouble(message)) {
                    if (Double.valueOf(message) == 100d) {
                        currentMapDownload = null;
                        displayToast(getString(R.string.download_finished_successfuly));
                        downloadCompleted(mapUniqueName);
                        // setCanStopMapDownload(false);
                    } else {
                        // setCanStopMapDownload(true);
                        displayToast(getString(R.string.map_download_progress, Double.valueOf(message)));
                        downloadStarted(mapUniqueName);
                    }
                } else {
                    displayToast(message);
                }
            }
        }
    }

    protected boolean isValidDouble(String doubleString) {
        String doubleRegex = "[+-]{0,1}[0-9]*.{0,1}[0-9]*";
        return (!doubleString.isEmpty() && doubleString.matches(doubleRegex));
    }

    protected abstract void downloadCompleted(String mapUniqueName);

    protected abstract void downloadStarted(String mapUniqueName);

    protected abstract void mapDeletedSuccessfully(String mapUniqueName);

    protected abstract void downloadStopped(String mapUniqueName);
}
