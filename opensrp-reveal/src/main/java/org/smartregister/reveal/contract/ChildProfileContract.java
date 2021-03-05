package org.smartregister.reveal.contract;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Task;
import org.smartregister.reveal.model.Child;
import org.smartregister.util.CallableInteractor;
import org.smartregister.util.QueryComposer;
import org.smartregister.view.contract.BaseProfileContract;

public interface ChildProfileContract {

    interface View extends BaseProfileContract.View {

        void onFetchResult(Child child);

        void setLoadingState(boolean loadingState);

        Presenter getPresenter();

        void onError(Exception e);

        void startEditForm();

        void startADRForm();

        void startEditMDAForm();

        void startJsonForm(JSONObject jsonObject, String formTitle);

        void reloadFromSource();

        void enableEditMDAForm(String status);
    }

    interface Presenter extends BaseProfileContract.Presenter {

        void fetchProfileData(String baseEntityID);

        @NonNull
        CallableInteractor getInteractor();

        Presenter usingInteractor(CallableInteractor interactor);

        @Nullable
        View getView();

        @NonNull
        Model getModel();

        void startChildRegistrationForm(Context context, String baseEntityID);

        void startEditMDAForm(Context context, String baseEntityID);

        void startADRForm(Context context, String baseEntityID);

        void updateChild(JSONObject jsonObject, Context context);

        void saveMDAForm(String jsonString, Context context);

        void saveADRForm(JSONObject jsonObject, Context context);
    }

    interface Model {

        @WorkerThread
        @Nullable
        Child getChild(String baseEntityID) throws QueryComposer.InvalidQueryException;

        @WorkerThread
        Task getCurrentTask(Context context, String baseEntityID);

        @WorkerThread
        JSONObject getRegistrationEditForm(Context context, String baseEntityID) throws Exception;

        @WorkerThread
        JSONObject getEditMDAForm(Context context, String baseEntityID) throws Exception;

        @WorkerThread
        JSONObject getADRForm(Context context, String baseEntityID) throws JSONException;

    }
}
