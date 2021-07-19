package org.smartregister.reveal.contract;


import android.content.Context;
import android.location.Location;

import androidx.annotation.StringRes;

import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Event;
import org.smartregister.reveal.adapter.TaskRegisterAdapter;
import org.smartregister.reveal.model.BaseTaskDetails;
import org.smartregister.reveal.model.TaskDetails;
import org.smartregister.reveal.model.TaskFilterParams;
import org.smartregister.reveal.util.LocationUtils;
import org.smartregister.view.contract.BaseRegisterFragmentContract;

import java.util.List;
import java.util.Set;

/**
 * Created by samuelgithengi on 3/18/19.
 */
public interface TaskRegisterFragmentContract {

    interface Presenter extends BaseRegisterFragmentContract.Presenter, BaseFormFragmentContract.Presenter, BaseContract.BasePresenter {
        void onTasksFound(List<TaskDetails> tasks, int structuresWithinBuffer);

        void onDestroy();

        void onDrawerClosed();

        void onTaskSelected(TaskDetails details, boolean isActionClicked);

        @StringRes
        int getInterventionLabel();

        void onIndexCaseFound(JSONObject indexCase, boolean isLinkedToJurisdiction);

        void searchTasks(String searchText);

        void filterTasks(TaskFilterParams filterParams);

        void onFilterTasksClicked();

        void setTaskFilterParams(TaskFilterParams filterParams);

        void onOpenMapClicked();

        void resetTaskInfo(TaskDetails taskDetails);

        void onTaskInfoReset();

        void onEventFound(Event event);
    }

    interface View extends BaseRegisterFragmentContract.View, BaseFormFragmentContract.View {

        Location getLastLocation();

        void initializeAdapter(Set<org.smartregister.configurableviews.model.View> visibleColumns);

        void setTotalTasks(int structuresWithinBuffer);

        void setTaskDetails(List<TaskDetails> tasks);

        void displayNotification(int title, @StringRes int message, Object... formatArgs);

        void showProgressDialog(@StringRes int title, @StringRes int message);

        void hideProgressDialog();

        LocationUtils getLocationUtils();

        void setInventionType(int interventionLabel);

        void registerFamily(BaseTaskDetails taskDetails);

        void openFamilyProfile(CommonPersonObjectClient family, BaseTaskDetails taskDetails);

        void openTasksScreen(BaseTaskDetails taskDetails);

        void displayIndexCaseDetails(JSONObject indexCase);

        void setNumberOfFilters(int numberOfFilters);

        void clearFilter();

        TaskRegisterAdapter getAdapter();

        void openFilterActivity(TaskFilterParams filterParams);

        void setSearchPhrase(String searchPhrase);

        void startMapActivity(TaskFilterParams taskFilterParams);
    }

    interface Interactor {
        void resetTaskInfo(Context context, TaskDetails taskDetails);

        void findLastEvent(String eventBaseEntityId, String eventType);
    }


}
