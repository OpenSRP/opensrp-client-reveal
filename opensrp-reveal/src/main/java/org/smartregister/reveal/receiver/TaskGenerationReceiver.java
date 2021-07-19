package org.smartregister.reveal.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.smartregister.AllConstants;
import org.smartregister.domain.Task;

import timber.log.Timber;

/**
 * Created by samuelgithengi on 10/30/20.
 */
public class TaskGenerationReceiver extends BroadcastReceiver {

    private final TaskGenerationCallBack taskGenerationCallBack;

    public TaskGenerationReceiver(TaskGenerationCallBack taskGenerationCallBack) {
        this.taskGenerationCallBack = taskGenerationCallBack;
    }

    public interface TaskGenerationCallBack {
        void onTaskCreatedOrUpdated(Task task);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Bundle extras = intent.getExtras();
        if (AllConstants.INTENT_KEY.TASK_GENERATED_EVENT.equals(intent.getAction())) {
            Task task = extras != null ? (Task) extras.getSerializable(AllConstants.INTENT_KEY.TASK_GENERATED) : null;
            if (task != null) {
                Timber.d("Task created or updated %s", task);
                taskGenerationCallBack.onTaskCreatedOrUpdated(task);
            }
        }
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }
}
