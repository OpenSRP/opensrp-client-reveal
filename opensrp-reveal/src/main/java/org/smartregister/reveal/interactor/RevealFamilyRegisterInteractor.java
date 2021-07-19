package org.smartregister.reveal.interactor;

import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.reflect.TypeToken;

import org.smartregister.domain.db.EventClient;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.FamilyRegisterContract;
import org.smartregister.reveal.receiver.TaskGenerationReceiver;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.sync.ClientProcessorForJava;

import java.lang.reflect.Type;
import java.util.List;

import static org.smartregister.AllConstants.INTENT_KEY.TASK_GENERATED_EVENT;

/**
 * Created by samuelgithengi on 4/15/19.
 */
public class RevealFamilyRegisterInteractor extends org.smartregister.family.interactor.FamilyRegisterInteractor implements FamilyRegisterContract.Interactor {


    private AppExecutors appExecutors;

    private RevealClientProcessor clientProcessor;

    private FamilyRegisterContract.Presenter presenter;

    public RevealFamilyRegisterInteractor(FamilyRegisterContract.Presenter presenter) {
        this.presenter = presenter;
        clientProcessor = (RevealClientProcessor) RevealApplication.getInstance().getClientProcessor();
        appExecutors = RevealApplication.getInstance().getAppExecutors();
    }

    @Override
    public ClientProcessorForJava getClientProcessorForJava() {
        return RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext());
    }

    @Override
    protected void processClient(List<EventClient> eventClientList) {
        IntentFilter filter = new IntentFilter(TASK_GENERATED_EVENT);
        TaskGenerationReceiver taskGenerationReceiver = new TaskGenerationReceiver(task -> {
            Type type = new TypeToken<List<FamilyEventClient>>() {
            }.getType();
            appExecutors.mainThread().execute(() -> presenter.onTasksGenerated(JsonFormUtils.gson.fromJson(JsonFormUtils.gson.toJson(eventClientList), type)));
        });
        LocalBroadcastManager.getInstance(RevealApplication.getInstance().getApplicationContext()).registerReceiver(taskGenerationReceiver, filter);
        clientProcessor.processClient(eventClientList, true);
    }
}
