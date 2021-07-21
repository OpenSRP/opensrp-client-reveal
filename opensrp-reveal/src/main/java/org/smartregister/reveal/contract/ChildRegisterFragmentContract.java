package org.smartregister.reveal.contract;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.reveal.model.Child;
import org.smartregister.util.CallableInteractor;
import org.smartregister.view.ListContract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ChildRegisterFragmentContract {

    interface View extends ListContract.View<Child> {
        void openFilterFragment();

        void startChildRegistrationForm();

        void startRecordMDAForm(String baseEntityID);

        void startJsonForm(JSONObject form, String formTitle);

        void reloadFromSource();

        void onReportCountReloaded(Map<String, Integer> reportCounts);

        void toggleDetailedReport();

        Location getCurrentStructure();

    }

    interface Presenter extends ListContract.Presenter<Child> {

        void search(@Nullable HashMap<String, List<String>> sortAndFilter, @Nullable String searchText);

        void startMDAForm(Context context, String baseEntityID);

        void startChildRegistrationForm(Context context);

        @NonNull
        CallableInteractor getCallableInteractor();

        Presenter withInteractor(@NonNull CallableInteractor callable);

        void saveChild(String jsonString, Context context);

        void saveMDAForm(String jsonString, Context context);

        void fetchReportStats();
    }

    interface Model extends ListContract.Model<Child> {

        @WorkerThread
        List<Child> searchAndFilter(@Nullable HashMap<String, List<String>> sortAndFilter, @Nullable String searchText);

        @WorkerThread
        JSONObject getMDAForm(Context context, String baseEntityID) throws JSONException;

        @WorkerThread
        JSONObject getRegistrationForm(Context context) throws JSONException;

        @WorkerThread
        Task getCurrentTask(Context context, String baseEntityID);

        String getCurrentLocationID();

        @WorkerThread
        Map<String, Integer> getReportCounts();
    }

}
