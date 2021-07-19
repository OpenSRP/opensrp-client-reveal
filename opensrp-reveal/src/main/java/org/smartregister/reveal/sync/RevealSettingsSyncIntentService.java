package org.smartregister.reveal.sync;

import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.smartregister.AllConstants;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.sync.intent.SettingsSyncIntentService;

import static org.smartregister.reveal.util.Constants.Action.STRUCTURE_TASK_SYNCED;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.UPDATE_LOCATION_BUFFER_RADIUS;

/**
 * @author Vincent Karuri
 */
public class RevealSettingsSyncIntentService extends SettingsSyncIntentService {
    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        Bundle data = intent.getExtras();
        if (data != null && data.getInt(AllConstants.INTENT_KEY.SYNC_TOTAL_RECORDS, 0) > 0) {
            RevealApplication.getInstance().processServerConfigs();
            // broadcast sync event
            Intent refreshGeoWidgetIntent = new Intent(STRUCTURE_TASK_SYNCED);
            refreshGeoWidgetIntent.putExtra(UPDATE_LOCATION_BUFFER_RADIUS, true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(refreshGeoWidgetIntent);
        }
    }
}
