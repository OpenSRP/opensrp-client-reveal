package org.smartregister.reveal.contract;

import android.app.Activity;

import androidx.annotation.NonNull;

import org.smartregister.clientandeventmodel.Client;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Task;

/**
 * Created by samuelgithengi on 4/12/19.
 */
public interface FamilyProfileContract extends org.smartregister.family.contract.FamilyProfileContract {

    interface View extends org.smartregister.family.contract.FamilyProfileContract.View {

        void setStructureId(String structureId);

        void refreshTasks(String structureId);

        void updateFamilyName(String firstName);

        Activity getContext();

        void returnToMapView(String structureId, Task task);
    }

    interface Interactor extends org.smartregister.family.contract.FamilyProfileContract.Interactor {

        void updateFamilyMemberName(@NonNull Client family, Event event, @NonNull String oldFamilyName);

        void archiveFamily(String familyBaseEntityId, String structureId);
    }

    interface Presenter extends org.smartregister.family.contract.FamilyProfileContract.Presenter {

        void onTasksGenerated();

        void onMembersUpdated();

        void onAddFamilyMember();

        void onArchiveFamilyClicked();

        void onArchiveFamilyCompleted(boolean isSuccessfulSaved, Task task);
    }
}

