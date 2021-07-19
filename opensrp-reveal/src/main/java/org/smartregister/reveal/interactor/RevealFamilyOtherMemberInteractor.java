package org.smartregister.reveal.interactor;


import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.db.EventClient;
import org.smartregister.family.interactor.FamilyOtherMemberProfileInteractor;
import org.smartregister.reveal.contract.FamilyOtherMemberProfileContract;
import org.smartregister.reveal.contract.FamilyOtherMemberProfileContract.Interactor;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.InteractorUtils;

import java.util.Collections;

import static org.smartregister.reveal.application.RevealApplication.getInstance;

public class RevealFamilyOtherMemberInteractor extends FamilyOtherMemberProfileInteractor implements Interactor {

    private CommonRepository commonRepository;

    private AppExecutors appExecutors;

    private InteractorUtils interactorUtils;

    private RevealClientProcessor clientProcessor;

    public RevealFamilyOtherMemberInteractor() {
        commonRepository = getInstance().getContext().commonrepository(getInstance().getMetadata().familyMemberRegister.tableName);
        appExecutors = getInstance().getAppExecutors();
        interactorUtils = new InteractorUtils(getInstance().getTaskRepository(), getInstance().getContext().getEventClientRepository());
        clientProcessor = (RevealClientProcessor) getInstance().getClientProcessor();
    }

    @Override
    public void getFamilyHead(FamilyOtherMemberProfileContract.BasePresenter presenter, String familyHeadId) {
        appExecutors.diskIO().execute(() -> {
            CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(familyHeadId);
            appExecutors.mainThread().execute(() -> {
                presenter.onFetchFamilyHead(commonPersonObject);
            });
        });
    }

    @Override
    public void archiveFamilyMember(FamilyOtherMemberProfileContract.BasePresenter presenter, CommonPersonObjectClient client) {
        appExecutors.diskIO().execute(() -> {
            getInstance().getRepository().getWritableDatabase().beginTransaction();
            EventClient eventClient;
            try {
                eventClient = interactorUtils.archiveClient(client.getCaseId(), false);
                clientProcessor.processClient(Collections.singletonList(eventClient), true);
                getInstance().getRepository().getWritableDatabase().setTransactionSuccessful();
            } finally {
                getInstance().getRepository().getWritableDatabase().endTransaction();
            }
            appExecutors.mainThread().execute(() -> {
                presenter.onArchiveMemberCompleted(eventClient != null);
            });

        });
    }
}
