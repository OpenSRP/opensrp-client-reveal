package org.smartregister.reveal.job;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;

import org.smartregister.AllConstants;
import org.smartregister.job.BaseJob;
import org.smartregister.reveal.sync.LocationTaskIntentService;
public class LocationTaskServiceJob extends BaseJob {

    public static final String TAG = "LocationTaskServiceJob";

    @NonNull
    @Override
    protected Job.Result onRunJob(@NonNull Job.Params params) {
        Intent intent = new Intent(getApplicationContext(), LocationTaskIntentService.class);
        getApplicationContext().startService(intent);
        return params != null && params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) ? Job.Result.RESCHEDULE : Job.Result.SUCCESS;
    }
}
