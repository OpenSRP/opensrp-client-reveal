package org.smartregister.reveal.presenter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.smartregister.domain.Location;
import org.smartregister.domain.Task;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.model.ChildProfileModel;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.NativeFormProcessorHelper;
import org.smartregister.util.CallableInteractor;
import org.smartregister.util.CallableInteractorCallBack;
import org.smartregister.util.NativeFormProcessor;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.DatabaseKeys.CHILD_TABLE;
import static org.smartregister.reveal.util.Constants.EventType.UPDATE_CHILD_REGISTRATION;

public class ChildProfilePresenter implements ChildProfileContract.Presenter {

    private CallableInteractor callableInteractor;
    private WeakReference<ChildProfileContract.View> viewWeakReference;
    private ChildProfileContract.Model model;

    private TaskRepository taskRepository = RevealApplication.getInstance().getTaskRepository();

    public ChildProfilePresenter(ChildProfileContract.View view) {
        this.viewWeakReference = new WeakReference<>(view);
    }

    public RevealApplication getRevealApplication(){
        return RevealApplication.getInstance();
    }

    @Override
    public void fetchProfileData(String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model myModel = getModel();

        ChildProfileContract.View view = getView();
        if (view != null) {
            Callable<Child> callable = () -> myModel.getChild(baseEntityID);
            view.setLoadingState(true);

            myInteractor.execute(callable, new CallableInteractorCallBack<Child>() {
                @Override
                public void onResult(Child result) {
                    ChildProfileContract.View view = getView();
                    if (view != null) {
                        if (result != null) {
                            view.onFetchResult(result);
                            view.enableEditMDAForm(result.getTaskStatus());
                        } else {
                            view.onError(new IllegalStateException("Child not found"));
                        }
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ChildProfileContract.View view = getView();
                    if (view != null) {
                        view.onError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @NonNull
    @Override
    public CallableInteractor getInteractor() {
        if (callableInteractor == null)
            throw new IllegalStateException("Interacator is not set");

        return callableInteractor;
    }

    @Override
    public ChildProfileContract.Presenter usingInteractor(CallableInteractor interactor) {
        this.callableInteractor = interactor;
        return this;
    }

    @Nullable
    @Override
    public ChildProfileContract.View getView() {
        if (viewWeakReference != null)
            return viewWeakReference.get();

        return null;
    }

    @NonNull
    @Override
    public ChildProfileContract.Model getModel() {
        if (model == null)
            model = new ChildProfileModel();

        return model;
    }

    @Override
    public void startChildRegistrationForm(Context context, String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model model = getModel();
        Callable<JSONObject> callable = () -> model.getRegistrationEditForm(context, baseEntityID);
        myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
            @Override
            public void onResult(JSONObject jsonObject) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    if (jsonObject != null) {
                        view.startJsonForm(jsonObject, context.getString(R.string.edit_student));
                    } else {
                        view.onError(new IllegalArgumentException("Form not found"));
                    }
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.onError(ex);
                    view.setLoadingState(false);
                }
            }
        });
    }

    @Override
    public void startEditMDAForm(Context context, String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model model = getModel();
        Callable<JSONObject> callable = () -> model.getEditMDAForm(context, baseEntityID);
        myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
            @Override
            public void onResult(JSONObject jsonObject) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    if (jsonObject != null) {
                        view.startJsonForm(jsonObject, context.getString(R.string.edit_student));
                    } else {
                        view.onError(new IllegalArgumentException("Form not found"));
                    }
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.onError(ex);
                    view.setLoadingState(false);
                }
            }
        });
    }

    @Override
    public void startADRForm(Context context, String baseEntityID) {
        CallableInteractor myInteractor = getInteractor();
        ChildProfileContract.Model model = getModel();
        Callable<JSONObject> callable = () -> model.getADRForm(context, baseEntityID);
        myInteractor.execute(callable, new CallableInteractorCallBack<JSONObject>() {
            @Override
            public void onResult(JSONObject jsonObject) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    if (jsonObject != null) {
                        view.startJsonForm(jsonObject, context.getString(R.string.record_adr));
                    } else {
                        view.onError(new IllegalArgumentException("Form not found"));
                    }
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.onError(ex);
                    view.setLoadingState(false);
                }
            }
        });
    }

    @Override
    public void updateChild(JSONObject jsonObject, Context context) {
        CallableInteractor myInteractor = getInteractor();

        Callable<Void> callable = () -> {

            NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonObject);
            Location operationalArea = NativeFormProcessorHelper.getCurrentOperationalArea();
            String entityId = jsonObject.getString(Constants.Properties.BASE_ENTITY_ID);

            // update metadata
            processor.withBindType(CHILD_TABLE)
                    .withEncounterType(UPDATE_CHILD_REGISTRATION)
                    .withEntityId(entityId)
                    .tagLocationData(operationalArea)
                    .tagEventMetadata()

                    // create and save client
                    .hasClient(true)
                    .mergeAndSaveClient()

                    // create and save event to db
                    .saveEvent()

                    // execute client processing
                    .clientProcessForm();

            return null;
        };

        myInteractor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.reloadFromSource();
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view == null) return;
                view.onError(ex);
                view.setLoadingState(false);
            }
        });
    }

    @Override
    public void saveMDAForm(String jsonString, Context context) {
        CallableInteractor myInteractor = getInteractor();

        Callable<Void> callable = () -> {

            ChildProfileContract.Model model = getModel();
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
            getRevealApplication().setSynced(false);

            return null;
        };

        myInteractor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.reloadFromSource();
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view == null) return;
                view.onError(ex);
                view.setLoadingState(false);
            }
        });
    }

    @Override
    public void saveADRForm(JSONObject jsonObject, Context context) {
        CallableInteractor myInteractor = getInteractor();

        Callable<Void> callable = () -> {

            String entityId = jsonObject.getString(Constants.Properties.BASE_ENTITY_ID);

            // save event details
            NativeFormProcessor processor = NativeFormProcessorHelper.createInstance(jsonObject);
            Location operationalArea = NativeFormProcessorHelper.getCurrentOperationalArea();

            // update metadata
            processor
                    .withBindType(Constants.EventType.MDA_ADVERSE_DRUG_REACTION)
                    .withEncounterType(Constants.EventType.MDA_ADVERSE_DRUG_REACTION)
                    .withEntityId(entityId)
                    .tagLocationData(operationalArea)
                    .tagEventMetadata()

                    // save and client
                    .saveEvent()
                    .clientProcessForm();
            getRevealApplication().setSynced(false);

            return null;
        };

        myInteractor.execute(callable, new CallableInteractorCallBack<Void>() {
            @Override
            public void onResult(Void aVoid) {
                ChildProfileContract.View view = getView();
                if (view != null) {
                    view.reloadFromSource();
                    view.setLoadingState(false);
                }
            }

            @Override
            public void onError(Exception ex) {
                ChildProfileContract.View view = getView();
                if (view == null) return;
                view.onError(ex);
                view.setLoadingState(false);
            }
        });
    }

    @Override
    public void onDestroy(boolean b) {
        Timber.v("onDestroy");
    }
}
