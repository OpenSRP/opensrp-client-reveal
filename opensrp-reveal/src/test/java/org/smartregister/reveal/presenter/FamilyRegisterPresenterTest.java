package org.smartregister.reveal.presenter;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.clientandeventmodel.Address;
import org.smartregister.cloudant.models.Client;
import org.smartregister.cloudant.models.Event;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.FamilyRegisterContract;
import org.smartregister.reveal.model.FamilyRegisterModel;
import org.smartregister.reveal.util.FamilyConstants.RELATIONSHIP;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by samuelgithengi on 4/25/19.
 */
public class FamilyRegisterPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FamilyRegisterContract.View view;

    @Mock
    private FamilyRegisterModel model;

    @Mock
    private FamilyRegisterContract.Interactor interactor;

    private FamilyRegisterPresenter presenter;

    private String structureId = UUID.randomUUID().toString();

    private Context context = RuntimeEnvironment.application;

    private String baseEntityId = UUID.randomUUID().toString();

    private String familyHead = UUID.randomUUID().toString();

    private String careGiver = UUID.randomUUID().toString();

    private List<FamilyEventClient> eventClientList = new ArrayList<>();

    @Before
    public void setUp() {
        Client family = new Client();
        family.withLastName("Family")
                .withFirstName("Otala")
                .withBaseEntityId(baseEntityId)
                .addAddress(new Address().withAddressType("").withCityVillage("vl1"));
        family.addRelationship(RELATIONSHIP.FAMILY_HEAD, familyHead);
        family.addRelationship(RELATIONSHIP.PRIMARY_CAREGIVER, careGiver);
        eventClientList.add(new FamilyEventClient(family, new Event().withBaseEntityId(baseEntityId)));
        when(model.getStructureId()).thenReturn(structureId);
        when(view.getContext()).thenReturn(context);
        presenter = new FamilyRegisterPresenter(view, model);
        presenter.setInteractor(interactor);
    }

    @Test
    public void testOnRegistrationSavedForNewForms() {
        presenter = spy(presenter);
        presenter.onRegistrationSaved(false,true,eventClientList);
        verify(presenter, never()).onTasksGenerated(eventClientList);
    }


    @Test
    public void testOnRegistrationSavedForEditedForms() {
        presenter = spy(presenter);
        doNothing().when(presenter).onTasksGenerated(eventClientList);
        presenter.onRegistrationSaved(true,true,eventClientList);
        verify(presenter).onTasksGenerated(eventClientList);
    }

    @Test
    public void testOnTasksGenerated() {
        presenter.onTasksGenerated(eventClientList);
        verify(view).hideProgressDialog();
        verify(view).startProfileActivity(baseEntityId, familyHead, careGiver, "Otala");
        assertTrue(RevealApplication.getInstance().isRefreshMapOnEventSaved());
    }
}
