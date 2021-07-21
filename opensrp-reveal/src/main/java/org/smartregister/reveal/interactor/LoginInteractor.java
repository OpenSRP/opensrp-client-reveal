package org.smartregister.reveal.interactor;

import org.smartregister.CoreLibrary;
import org.smartregister.job.DocumentConfigurationServiceJob;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.login.interactor.BaseLoginInteractor;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.job.LocationTaskServiceJob;
import org.smartregister.reveal.util.Utils;
import org.smartregister.view.contract.BaseLoginContract;

import java.lang.ref.WeakReference;

import timber.log.Timber;

public class LoginInteractor extends BaseLoginInteractor implements BaseLoginContract.Interactor {

    public LoginInteractor(BaseLoginContract.Presenter loginPresenter) {
        super(loginPresenter);
    }

    @Override
    protected void scheduleJobsPeriodically() {
        LocationTaskServiceJob.scheduleJob(LocationTaskServiceJob.TAG,
                Utils.getSyncInterval(), getFlexValue((int) Utils.getSyncInterval()));

        PullUniqueIdsServiceJob.scheduleJob(SyncServiceJob.TAG,
                BuildConfig.PULL_UNIQUE_IDS_MINUTES, getFlexValue((int) BuildConfig.PULL_UNIQUE_IDS_MINUTES));

        DocumentConfigurationServiceJob
                .scheduleJob(DocumentConfigurationServiceJob.TAG, Utils.getSyncInterval(), getFlexValue((int) Utils.getSyncInterval()));
    }

    @Override
    protected void scheduleJobsImmediately() {
        Utils.startImmediateSync();
    }

    @Override
    public void loginWithLocalFlag(WeakReference<BaseLoginContract.View> view, boolean localLogin, String userName, char[] password) {
        if (!localLogin) {
            RevealApplication.getInstance().getContext().getHttpAgent().setConnectTimeout(CoreLibrary.getInstance().getSyncConfiguration().getConnectTimeout());
            RevealApplication.getInstance().getContext().getHttpAgent().setReadTimeout(CoreLibrary.getInstance().getSyncConfiguration().getReadTimeout());
        }
        try {
            super.loginWithLocalFlag(view, localLogin, userName, password);
        }catch (Exception e){
            Timber.e(e);
        }
    }
}
