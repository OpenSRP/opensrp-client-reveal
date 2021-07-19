package org.smartregister.reveal.job;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.smartregister.AllConstants;
import org.smartregister.job.SyncSettingsServiceJob;
import org.smartregister.reveal.sync.RevealSettingsSyncIntentService;

/**
 * @author Vincent Karuri
 */
public class RevealSyncSettingsServiceJob extends SyncSettingsServiceJob {

    public static final String TAG = "RevealSyncSettingsServiceJob";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Intent intent = new Intent(getApplicationContext(), RevealSettingsSyncIntentService.class);
        getApplicationContext().startService(intent);
        return params != null && params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) ? Result.RESCHEDULE : Result.SUCCESS;
    }
}
