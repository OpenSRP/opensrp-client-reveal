package org.smartregister.reveal.presenter;

import android.content.Context;

import org.jetbrains.annotations.Nullable;
import org.smartregister.reveal.R;
import org.smartregister.reveal.contract.ChildFilterFragmentContract;
import org.smartregister.util.CallableInteractor;
import org.smartregister.util.CallableInteractorCallBack;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ChildFilterFragmentPresenter implements ChildFilterFragmentContract.Presenter {

    private WeakReference<ChildFilterFragmentContract.View> weakReference;
    private CallableInteractor interactor;
    private ChildFilterFragmentContract.Model model;

    @Override
    public void fetchUniqueGrades(String schoolID) {
        ChildFilterFragmentContract.Model myModel = getModel();
        CallableInteractor myInteractor = getInteractor();

        if (myModel != null && myInteractor != null && getView() != null) {
            Callable<List<String>> callable = () -> myModel.fetchUniqueGrades(schoolID);
            getView().setLoadingState(true);

            myInteractor.execute(callable, new CallableInteractorCallBack<List<String>>() {
                @Override
                public void onResult(List<String> results) {
                    ChildFilterFragmentContract.View view = getView();
                    if (view != null) {
                        view.onGradesFetched(results);
                        view.setLoadingState(false);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    ChildFilterFragmentContract.View view = getView();
                    if (view != null) {
                        view.onError(ex);
                        view.setLoadingState(false);
                    }
                }
            });
        }
    }

    @Override
    public ChildFilterFragmentContract.Presenter usingView(ChildFilterFragmentContract.View view) {
        this.weakReference = new WeakReference<>(view);
        return this;
    }

    @Nullable
    @Override
    public ChildFilterFragmentContract.View getView() {
        if (weakReference != null)
            return weakReference.get();

        return null;
    }

    @Nullable
    @Override
    public ChildFilterFragmentContract.Model getModel() {
        return this.model;
    }

    @Override
    public ChildFilterFragmentContract.Presenter usingModel(ChildFilterFragmentContract.Model model) {
        this.model = model;
        return this;
    }

    @Nullable
    @Override
    public CallableInteractor getInteractor() {
        return interactor;
    }

    @Override
    public ChildFilterFragmentContract.Presenter usingInteractor(CallableInteractor interactor) {
        this.interactor = interactor;
        return this;
    }

    @Override
    public List<String> getSelectedAges(List<String> selectedRanges, Context context) {
        List<String> values = new ArrayList<>();

        for (String range : selectedRanges) {
            if (range.equalsIgnoreCase(context.getString(R.string.range_6_10))) {
                values.add("6:10");
            } else if (range.equalsIgnoreCase(context.getString(R.string.range_11_15))) {
                values.add("11:15");
            } else if (range.equalsIgnoreCase(context.getString(R.string.range_16_18))) {
                values.add("16:18");
            } else if (range.equalsIgnoreCase(context.getString(R.string.adult))) {
                values.add("Adult");
            }
        }

        return values;
    }
}
