package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.PlanDefinition;
import org.smartregister.domain.Task;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.PlanDefinitionSearchRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.ChildRegisterFragmentContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.model.ChildModel;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.NativeFormProcessorHelper;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TaskUtils;
import org.smartregister.util.AppExecutors;
import org.smartregister.util.CallableInteractor;
import org.smartregister.util.CallableInteractorCallBack;
import org.smartregister.util.DateTimeTypeConverter;
import org.smartregister.util.GenericInteractor;
import org.smartregister.util.NativeFormProcessor;
import org.smartregister.util.PropertiesConverter;
import org.smartregister.view.ListContract;
import org.smartregister.view.presenter.ListPresenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.smartregister.reveal.util.Constants.DatabaseKeys.CHILD_TABLE;
import static org.smartregister.reveal.util.Constants.REGISTER_CHILD_EVENT;

public class ChildRegisterFragmentPresenter extends ListPresenter<Child> implements ChildRegisterFragmentContract.Presenter {

    public static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .registerTypeAdapter(DateTime.class, new DateTimeTypeConverter())
            .registerTypeAdapter(LocationProperty.class, new PropertiesConverter()).create();

    private CallableInteractor callableInteractor;
    private PlanDefinitionSearchRepository planDefinitionSearchRepository = RevealApplication.getInstance().getPlanDefinitionSearchRepository();
    private TaskUtils taskUtils = TaskUtils.getInstance();
    private TaskRepository taskRepository = RevealApplication.getInstance().getTaskRepository();
    private RevealApplication revealApplication;

    public ChildRegisterFragmentPresenter() {
        revealApplication = RevealApplication.getInstance();
    }

    @Override
    public void search(@Nullable HashMap<String, List<String>> sortAndFilter, @Nullable String searchText) {
        ChildModel model = getModel();
        this.fetchList(() -> model.searchAndFilter(sortAndFilter, searchText), AppExecutors.Request.DISK_THREAD);
    }

    @Override
    public void startMDAForm(Context context, String baseEntityID) {
        CallableInteractor myInteractor = getCallableInteractor();
        ChildModel model = getModel();
        if (model != null) {
            Callable<JSONObject> callable = () -> model.getMDAForm(context, baseEntityID);
            myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
                @Override
                public void onResult(JSONObject jsonObject) {
                    ChildRegisterFragmentContract.View view = getView();
                    if (view != null) {
                        if (jsonObject != null) {
                            view.startJsonForm(jsonObject, context.getString(R.string.record_dose));
                        } else {
                            view.onFetchError(new IllegalArgumentException("Form not found"));
                        }
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ListContract.View<Child> view = getView();
                    if (view != null) {
                        view.onFetchError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @Override
    public void startChildRegistrationForm(Context context) {
        CallableInteractor myInteractor = getCallableInteractor();
        ChildModel model = getModel();
        if (model != null) {
            Callable<JSONObject> callable = () -> model.getRegistrationForm(context);
            myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
                @Override
                public void onResult(JSONObject jsonObject) {
                    ChildRegisterFragmentContract.View view = getView();
                    if (view != null) {
                        if (jsonObject != null) {
                            view.startJsonForm(jsonObject, context.getString(R.string.add_student));
                        } else {
                            view.onFetchError(new IllegalArgumentException("Form not found"));
                        }
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ListContract.View<Child> view = getView();
                    if (view != null) {
                        view.onFetchError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @Override
    public CallableInteractor getCallableInteractor() {
        if (callableInteractor == null)
            callableInteractor = new GenericInteractor();

        return callableInteractor;
    }

    @Override
    public ChildRegisterFragmentContract.Presenter withInteractor(@NonNull CallableInteractor callable) {
        this.callableInteractor = callable;
        return this;
    }

    @Override
    public void saveChild(String jsonString, Context context) {
        CallableInteractor myInteractor = getCallableInteractor();

        Callable<Void> callable = () -> {


            NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonString);

            Location operationalArea = NativeFormProcessorHelper.getCurrentOperationalArea();
            Location selectedSchool = NativeFormProcessorHelper.getCurrentSelectedStructure();
            String entityId = UUID.randomUUID().toString();
            String ageString = processor.getFieldValue("sactaAge").split("y")[0];

            processor

                    // update metadata
                    .withBindType(CHILD_TABLE)
                    .withEncounterType(REGISTER_CHILD_EVENT)
                    .withEntityId(entityId)
                    .tagLocationData(operationalArea)
                    .tagEventMetadata()

                    // create and save client
                    .hasClient(true)
                    .saveClient(client -> client.addAttribute(Constants.ChildRegister.DEFAULT_RESIDENCE, selectedSchool.getId()))

                    // create and save event to db
                    .saveEvent()

                    // execute client processing
                    .clientProcessForm()

                    // close form id
                    .closeRegistrationID(Constants.DatabaseKeys.UNIQUE_ID);

            PreferencesUtil prefsUtil = PreferencesUtil.getInstance();
            if (StringUtils.isBlank(prefsUtil.getCurrentPlanId())) {
                Set<PlanDefinition> planDefinitionSet = planDefinitionSearchRepository.findActivePlansByJurisdiction(operationalArea.getId());
                PlanDefinition planDefinition = planDefinitionSet.iterator().next();
                if (planDefinition != null)
                    prefsUtil.setCurrentPlanId(planDefinition.getIdentifier());
            }

            // limit task generation from 6 to 18
            if (StringUtils.isNotBlank(ageString)) {
                int age = Integer.parseInt(ageString);
                if (age <= 18)
                    taskUtils.generateDrugAdministrationTask(context, entityId);
            }
            return null;
        };

        myInteractor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                ChildRegisterFragmentContract.View view = getView();
                revealApplication.setSynced(false);
                if (view != null) {
                    view.reloadFromSource();
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildRegisterFragmentContract.View view = getView();
                if (view == null) return;
                view.onFetchError(ex);
                view.setLoadingState(false);
            }
        });

    }

    @Override
    public void saveMDAForm(String jsonString, Context context) {
        CallableInteractor myInteractor = getCallableInteractor();

        Callable<Void> callable = () -> {

            ChildModel model = getModel();
            String entityId = new JSONObject(jsonString).getString(Constants.Properties.BASE_ENTITY_ID);

            // update metadata
            NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonString)
                    .withBindType(Constants.EventType.MDA_DISPENSE)
                    .withEncounterType(Constants.EventType.MDA_DISPENSE)
                    .withEntityId(entityId);

            // get task
            Task task = model.getCurrentTask(context, entityId);

            // update the task
            boolean completed = processor.getFieldValue("mmaDrugAdmin").equalsIgnoreCase("Yes");

            task.setBusinessStatus(completed ?
                    Constants.BusinessStatus.VISITED_DRUG_ADMINISTERED :
                    Constants.BusinessStatus.VISITED_DRUG_NOT_ADMINISTERED
            );

            task.setStatus(Task.TaskStatus.COMPLETED);
            if (BaseRepository.TYPE_Synced.equals(task.getSyncStatus())) {
                task.setSyncStatus(BaseRepository.TYPE_Unsynced);
            }
            task.setLastModified(new DateTime());
            taskRepository.addOrUpdate(task);

            // save event details
            Location operationalArea = NativeFormProcessorHelper.getCurrentOperationalArea();
            processor
                    .tagLocationData(operationalArea)
                    .tagTaskDetails(task)
                    .tagEventMetadata()

                    // save and clientM
                    .saveEvent()
                    .clientProcessForm();

            return null;
        };

        myInteractor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                ChildRegisterFragmentContract.View view = getView();
                revealApplication.setSynced(false);
                if (view != null) {
                    view.reloadFromSource();
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildRegisterFragmentContract.View view = getView();
                if (view == null) return;
                view.onFetchError(ex);
                view.setLoadingState(false);
            }
        });
    }

    @Override
    public void fetchReportStats() {
        CallableInteractor myInteractor = getCallableInteractor();
        ChildModel model = getModel();
        if (model != null) {
            Callable<Map<String, Integer>> callable = model::getReportCounts;
            myInteractor.execute(callable, new CallableInteractorCallBack<Map<String, Integer>>() {
                @Override
                public void onResult(Map<String, Integer> results) {
                    ChildRegisterFragmentContract.View view = getView();
                    if (view != null) {
                        if (results != null) {
                            view.onReportCountReloaded(results);
                        } else {
                            view.onFetchError(new IllegalStateException("An error occurred while fetching results"));
                        }
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ListContract.View<Child> view = getView();
                    if (view != null) {
                        view.onFetchError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @Override
    public ChildRegisterFragmentContract.View getView() {
        return (ChildRegisterFragmentContract.View) super.getView();
    }

}
