package org.smartregister.reveal.presenter;


import org.smartregister.clientandeventmodel.Client;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.presenter.BaseFamilyRegisterPresenter;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.FamilyRegisterContract;
import org.smartregister.reveal.interactor.RevealFamilyRegisterInteractor;
import org.smartregister.reveal.model.FamilyRegisterModel;
import org.smartregister.reveal.util.FamilyConstants;

import java.util.List;

/**
 * Created by samuelgithengi on 4/14/19.
 */
public class FamilyRegisterPresenter extends BaseFamilyRegisterPresenter implements FamilyRegisterContract.Presenter {

    private FamilyRegisterContract.View view;

    public FamilyRegisterPresenter(FamilyRegisterContract.View view, FamilyRegisterContract.Model model) {
        super(view, model);
        this.view = view;
        setInteractor(new RevealFamilyRegisterInteractor(this));
    }


    @Override
    public void onRegistrationSaved(boolean isEditMode, boolean isSaved, List<FamilyEventClient> familyEventClientList) {
        if (isEditMode || !isSaved) {
            onTasksGenerated(familyEventClientList);
        }
    }

    @Override
    public void onTasksGenerated(List<FamilyEventClient> familyEventClientList) {
        view.hideProgressDialog();
        openProfileActivity(familyEventClientList);
        RevealApplication.getInstance().setRefreshMapOnEventSaved(true);
    }

    private void openProfileActivity(List<FamilyEventClient> familyEventClientList) {
        for (FamilyEventClient eventClient : familyEventClientList) {
            if (eventClient.getClient().getLastName().equals("Family")) {
                Client family = eventClient.getClient();
                view.startProfileActivity(family.getBaseEntityId(),
                        family.findRelatives(FamilyConstants.RELATIONSHIP.FAMILY_HEAD).get(0),
                        family.findRelatives(FamilyConstants.RELATIONSHIP.PRIMARY_CAREGIVER).get(0),
                        family.getFirstName());
                break;
            }
        }
    }
}
