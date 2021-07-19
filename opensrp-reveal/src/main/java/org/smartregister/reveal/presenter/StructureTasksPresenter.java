package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.mapbox.geojson.Feature;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Event;
import org.smartregister.domain.Task;
import org.smartregister.domain.Task.TaskStatus;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.BaseFormFragmentContract;
import org.smartregister.reveal.contract.StructureTasksContract;
import org.smartregister.reveal.interactor.BaseFormFragmentInteractor;
import org.smartregister.reveal.interactor.StructureTasksInteractor;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.Utils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import static org.smartregister.reveal.contract.StructureTasksContract.Interactor;
import static org.smartregister.reveal.contract.StructureTasksContract.Presenter;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;

/**
 * Created by samuelgithengi on 4/12/19.
 */
public class StructureTasksPresenter extends BaseFormFragmentPresenter implements Presenter {

    private WeakReference<StructureTasksContract.View> view;

    private Interactor interactor;

    private PreferencesUtil prefsUtil;

    private StructureTaskDetails indexCase;

    private String structureId;

    private BaseFormFragmentContract.Interactor formInteractor;


    public StructureTasksPresenter(StructureTasksContract.View view, Context context) {
        this(view, context, null, PreferencesUtil.getInstance());
        interactor = new StructureTasksInteractor(this);
        formInteractor = new BaseFormFragmentInteractor(this);
    }

    @VisibleForTesting
    public StructureTasksPresenter(StructureTasksContract.View view, Context context, Interactor interactor, PreferencesUtil prefsUtil) {
        super(view, context);
        this.view = new WeakReference<>(view);
        this.interactor = interactor;
        this.prefsUtil = prefsUtil;
    }

    @Override
    public void findTasks(String structureId) {
        this.structureId = structureId;
        interactor.findTasks(structureId, prefsUtil.getCurrentPlanId(),
                Utils.getOperationalAreaLocation(prefsUtil.getCurrentOperationalArea()).getId());
    }

    @Override
    public void refreshTasks() {
        if (structureId != null)
            findTasks(structureId);
    }


    @Override
    public void onTasksFound(List<StructureTaskDetails> taskDetailsList, StructureTaskDetails incompleteIndexCase) {
        indexCase = incompleteIndexCase;
        getView().setTaskDetailsList(taskDetailsList);
        if (incompleteIndexCase != null) {
            getView().displayDetectCaseButton();
        }
    }

    @Override
    public void onTaskSelected(StructureTaskDetails details, boolean isEdit, boolean isUndo) {
        if (details != null) {
            if (TaskStatus.COMPLETED.name().equals(details.getTaskStatus())) {
                if (isEdit) {
                    details.setEdit(true);
                    getView().showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
                    interactor.getStructure(details);
                } else if(isUndo) {
                    getView().displayResetTaskInfoDialog(details);
                } else {
                    getView().displayToast("Task Completed");
                }
            } else {
                getView().showProgressDialog(R.string.opening_form_title, R.string.opening_form_message);
                interactor.getStructure(details);
            }
        }
    }


    @Override
    public void onLocationValidated() {
        StructureTaskDetails taskDetails = (StructureTaskDetails) getTaskDetails();
        if (Constants.Intervention.REGISTER_FAMILY.equals(getTaskDetails().getTaskCode())
                && !Task.TaskStatus.COMPLETED.name().equals(getTaskDetails().getTaskStatus())) {
            getView().registerFamily(getTaskDetails());
        }
        if (taskDetails.isEdit() && (Constants.Intervention.BEDNET_DISTRIBUTION.equals(taskDetails.getTaskCode()) || BLOOD_SCREENING.equals(taskDetails.getTaskCode()))) {
            interactor.findLastEvent(taskDetails);
        } else {
            super.onLocationValidated();
        }
    }

    public StructureTasksContract.View getView() {
        return view.get();
    }

    @Override
    public void saveJsonForm(String json) {
        getView().showProgressDialog(R.string.saving_title, R.string.saving_message);
        interactor.saveJsonForm(json);
    }

    @Override
    public void onDetectCase() {
        indexCase.setStructureId(structureId);
        interactor.getStructure(indexCase);
    }

    @Override
    public void onIndexConfirmationFormSaved(String taskID, TaskStatus taskStatus, String businessStatus, Set<Task> removedTasks) {
        getView().updateTasks(taskID, taskStatus, businessStatus, removedTasks);
        if (taskStatus.equals(TaskStatus.COMPLETED)) {
            getView().hideDetectCaseButton();
        }
        if (!removedTasks.isEmpty()) {
            getView().updateNumberOfTasks();
        }
        getView().hideProgressDialog();
    }

    @Override
    public void onEventFound(Event event) {

        String formName = getView().getJsonFormUtils().getFormName(null, getTaskDetails().getTaskCode());
        if (StringUtils.isBlank(formName)) {
            getView().displayError(R.string.opening_form_title, R.string.form_not_found);
        } else {
            JSONObject formJSON = getView().getJsonFormUtils().getFormJSON(getView().getContext(), formName, getTaskDetails(), getStructure());
            getView().getJsonFormUtils().populateForm(event, formJSON);
            if (Constants.Intervention.BEDNET_DISTRIBUTION.equals(getTaskDetails().getTaskCode())) {
                formInteractor.findNumberOfMembers(getTaskDetails().getTaskEntity(), formJSON);
            } else {
                getView().startForm(formJSON);
            }
        }
        getView().hideProgressDialog();

    }

    @Override
    public void resetTaskInfo(StructureTaskDetails taskDetails) {
        interactor.resetTaskInfo(getView().getContext(), taskDetails);
    }

    @Override
    public void onTaskInfoReset(String structureId) {
        findTasks(structureId);
    }

    @Override
    public void onFormSaved(@NonNull String structureId, String taskID, @NonNull TaskStatus taskStatus, @NonNull String businessStatus, String interventionType) {
        getView().hideProgressDialog();
        getView().updateTask(taskID, taskStatus, businessStatus);
    }

    @Override
    public void onStructureAdded(Feature feature, JSONArray featureCoordinates, double zoomlevel) {//not used
    }

    @Override
    public void onFormSaveFailure(String eventType) {
        getView().hideProgressDialog();//register will refresh on resume
    }

    @Override
    public void onFamilyFound(CommonPersonObjectClient finalFamily) {//not used
    }
}
