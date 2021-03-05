package org.smartregister.reveal.contract;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.smartregister.util.CallableInteractor;
import org.smartregister.util.QueryComposer;

import java.util.HashMap;
import java.util.List;

public interface ChildFilterFragmentContract {

    interface View {

        void bindLayout();

        void updateParameters();

        @NonNull
        Presenter loadPresenter();

        void reloadParameters();

        HashMap<String, List<String>> getFilterValues();

        void clearFilters();

        void onGradesFetched(List<String> grades);

        void onError(Exception e);

        void setLoadingState(boolean loadingState);

        void executeFilter();

        String getCurrentLocation();
    }

    interface Presenter {

        void fetchUniqueGrades(String schoolID);

        Presenter usingView(ChildFilterFragmentContract.View view);

        @Nullable
        View getView();

        @Nullable
        Model getModel();

        Presenter usingModel(Model model);

        @Nullable
        CallableInteractor getInteractor();

        Presenter usingInteractor(CallableInteractor interactor);

        List<String> getSelectedAges(List<String> selectedRanges, Context context);
    }

    interface Model {

        @WorkerThread
        List<String> fetchUniqueGrades(String schoolID) throws QueryComposer.InvalidQueryException;

    }
}
