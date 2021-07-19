package org.smartregister.reveal.presenter;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import net.sqlcipher.Cursor;
import net.sqlcipher.MatrixCursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.cloudant.models.Client;
import org.smartregister.cloudant.models.Event;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.FetchStatus;
import org.smartregister.domain.Task;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.contract.FamilyOtherMemberProfileContract;
import org.smartregister.reveal.contract.FamilyProfileContract;
import org.smartregister.reveal.model.FamilyProfileModel;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.FamilyConstants;
import org.smartregister.reveal.util.FamilyConstants.DatabaseKeys;
import org.smartregister.reveal.util.FamilyConstants.EventType;
import org.smartregister.reveal.util.FamilyConstants.JSON_FORM;
import org.smartregister.reveal.util.FamilyJsonFormUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.view.FamilyProfileActivity;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.Intervention.FI;

/**
 * Created by samuelgithengi on 4/25/19.
 */
public class FamilyProfileFragmentPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FamilyProfileActivity view;

    @Mock
    private FamilyProfileModel model;

    @Mock
    private PreferencesUtil preferencesUtil;

    @Mock
    private SQLiteDatabase database;

    @Mock
    private FamilyProfileContract.Interactor interactor;

    @Mock
    private FamilyOtherMemberProfileContract.Interactor otherMemberInteractor;

    @Mock
    private FamilyJsonFormUtils familyJsonFormUtils;

    private Context context = RuntimeEnvironment.application;

    private FamilyProfilePresenter presenter;

    private String familyId = UUID.randomUUID().toString();

    private String familyHeadId = UUID.randomUUID().toString();

    private String structureId = UUID.randomUUID().toString();

    private CommonPersonObject familyHead = new CommonPersonObject(UUID.randomUUID().toString(), null, null, null);

    @Before
    public void setUp() {
        org.smartregister.Context.bindtypes = new ArrayList<>();
        presenter = new FamilyProfilePresenter(view, model, familyId, familyHeadId, null, null);
        Whitebox.setInternalState(presenter, "preferencesUtil", preferencesUtil);
        Whitebox.setInternalState(presenter, "database", database);
        Whitebox.setInternalState(presenter, "interactor", interactor);
        Whitebox.setInternalState(presenter, "familyJsonFormUtils", familyJsonFormUtils);
        Whitebox.setInternalState(presenter, "otherMemberInteractor", otherMemberInteractor);
        when(view.getApplicationContext()).thenReturn(context);
        when(view.getContext()).thenReturn(Robolectric.buildActivity(Activity.class).resume().get());
    }

    @Test
    public void testRefreshProfileTopSection() {
        CommonPersonObjectClient client = TestingUtils.getCommonPersonObjectClient();
        String oAname = "MTI_13";
        String district = "Chadiza";
        when(preferencesUtil.getCurrentOperationalArea()).thenReturn(oAname);
        when(preferencesUtil.getCurrentDistrict()).thenReturn(district);
        when(view.getString(R.string.family_title)).thenReturn(context.getString(R.string.family_title));
        presenter.refreshProfileTopSection(client);
        verify(view).setProfileDetailOne(oAname);
        verify(view).setProfileDetailTwo(district);
    }

    @Test
    public void testGetStructureId() throws Exception {
        String sql = "SELECT DISTINCT structure_id FROM EC_FAMILY WHERE base_entity_id = ?";
        when(database.rawQuery(sql, new String[]{familyId})).thenReturn(createCursor());
        Whitebox.invokeMethod(presenter, "getStructureId", familyId);
        verify(database, timeout(ASYNC_TIMEOUT).atLeastOnce()).rawQuery(sql,
                new String[]{familyId});
        verify(model, timeout(ASYNC_TIMEOUT).atLeastOnce()).setStructureId(structureId);
        verify(view, timeout(ASYNC_TIMEOUT).atLeastOnce()).setStructureId(structureId);
        assertEquals(structureId, presenter.getStructureId());
    }

    @Test
    public void testGetView() {
        assertEquals(view, presenter.getView());
    }

    @Test
    public void testOnRegistrationSavedForNewForms() {
        presenter = spy(presenter);
        PreferencesUtil.getInstance().setCurrentPlan("FI_2019_TV01_Focus");
        PreferencesUtil.getInstance().setInterventionTypeForPlan("FI_2019_TV01_Focus", FI);
        Whitebox.setInternalState(presenter, "structureId", structureId);
        String baseEntityId = UUID.randomUUID().toString();
        FamilyEventClient eventClient = new FamilyEventClient(new Client(), new Event().withBaseEntityId(baseEntityId));
        presenter.onRegistrationSaved(false, true, eventClient);
        verify(presenter, never()).onTasksGenerated();
        verify(view, never()).refreshTasks(structureId);
    }

    @Test
    public void testOnRegistrationSavedForEditedForms() {
        Whitebox.setInternalState(presenter, "structureId", structureId);
        String entityId = UUID.randomUUID().toString();
        FamilyEventClient eventClient = new FamilyEventClient((Client) new Client().withBaseEntityId(entityId), new Event()
                .withBaseEntityId(entityId).withObs(new ArrayList<>()));
        presenter.onRegistrationSaved(true, true, eventClient);
        verify(interactor, never()).updateFamilyMemberName(eventClient.getClient(), eventClient.getEvent(), null);
        verify(view, never()).refreshTasks(structureId);
    }

    @Test
    public void testOnRegistrationSavedForEditedFormsUpdatesMembers() {
        Whitebox.setInternalState(presenter, "structureId", structureId);
        String entityId = UUID.randomUUID().toString();
        FamilyEventClient eventClient = new FamilyEventClient((Client) new Client().withFirstName("Victor").withBaseEntityId(entityId), new Event()
                .withBaseEntityId(entityId).withObs(new Obs().withValue("Victoria").withFieldCode(DatabaseKeys.OLD_FAMILY_NAME)));
        presenter.onRegistrationSaved(true, true, eventClient);
        verify(interactor).updateFamilyMemberName(eventClient.getClient(), eventClient.getEvent(), "Victoria");
        verify(view).updateFamilyName("Victor");
        verify(view, never()).hideProgressDialog();
        verify(view, never()).refreshMemberList(FetchStatus.fetched);
    }


    @Test
    public void testOnRegistrationSavedForEditedFormsNeverUpdatesMembers() {
        Whitebox.setInternalState(presenter, "structureId", structureId);
        String entityId = UUID.randomUUID().toString();
        FamilyEventClient eventClient = new FamilyEventClient((Client) new Client().withFirstName("Victor").withBaseEntityId(entityId), new Event()
                .withBaseEntityId(entityId).withObs(new Obs().withValue("Victor").withFieldCode(DatabaseKeys.OLD_FAMILY_NAME)));
        presenter.onRegistrationSaved(true, true, eventClient);
        verify(interactor, never()).updateFamilyMemberName(eventClient.getClient(), eventClient.getEvent(), "Victoria");
        verify(view, never()).updateFamilyName("Victor");
        verify(view).hideProgressDialog();
        verify(view).refreshMemberList(FetchStatus.fetched);
    }

    @Test
    public void testOnTasksGenerated() {
        presenter.onTasksGenerated();
        verify(view).refreshTasks(null);
    }

    @Test
    public void onMembersUpdated() {
        presenter.onMembersUpdated();
        verify(view).refreshTasks(null);
    }


    @Test
    public void testStartFormForEdit() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        CommonPersonObjectClient client = TestingUtils.getCommonPersonObjectClient();
        presenter.startFormForEdit(client);
        verify(familyJsonFormUtils).getAutoPopulatedJsonEditFormString(JSON_FORM.FAMILY_UPDATE, client, EventType.UPDATE_FAMILY_REGISTRATION);
        verify(view).startFormActivity(null);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testStartFormForEditErrors() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        doThrow(new RuntimeException()).when(view).startFormActivity(any());
        CommonPersonObjectClient client = TestingUtils.getCommonPersonObjectClient();
        presenter.startFormForEdit(client);
        verify(familyJsonFormUtils).getAutoPopulatedJsonEditFormString(JSON_FORM.FAMILY_UPDATE, client, EventType.UPDATE_FAMILY_REGISTRATION);
        verify(view).startFormActivity(null);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }


    @Test
    public void testOnAddFamilyMemberFetchesFamilyHead() {
        presenter.onAddFamilyMember();
        verify(otherMemberInteractor).getFamilyHead(presenter, familyHeadId);
        verify(view).getApplicationContext();
        verifyNoMoreInteractions(view);
    }

    @Test
    public void testOnAddFamilyMemberOpensFormIfFamilyHeadIsFetched() throws Exception {
        FamilyMetadata metadata = RevealApplication.getInstance().getMetadata();
        metadata.updateFamilyMemberRegister(FamilyConstants.JSON_FORM.FAMILY_MEMBER_REGISTER, FamilyConstants.TABLE_NAME.FAMILY_MEMBER, FamilyConstants.EventType.FAMILY_MEMBER_REGISTRATION, FamilyConstants.EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, FamilyConstants.CONFIGURATION.FAMILY_MEMBER_REGISTER, FamilyConstants.RELATIONSHIP.FAMILY);

        when(model.getFamilyHeadPersonObject()).thenReturn(familyHead);
        presenter = spy(presenter);
        presenter.onAddFamilyMember();
        verify(otherMemberInteractor, never()).getFamilyHead(presenter, familyHeadId);
        verify(interactor).getNextUniqueId(any(), any());
        verify(presenter).startForm(JSON_FORM.FAMILY_MEMBER_REGISTER, null, null, "");
    }


    @Test
    public void testOnFetchFamilyHead() throws Exception {

        FamilyMetadata metadata = RevealApplication.getInstance().getMetadata();
        metadata.updateFamilyMemberRegister(FamilyConstants.JSON_FORM.FAMILY_MEMBER_REGISTER, FamilyConstants.TABLE_NAME.FAMILY_MEMBER, FamilyConstants.EventType.FAMILY_MEMBER_REGISTRATION, FamilyConstants.EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, FamilyConstants.CONFIGURATION.FAMILY_MEMBER_REGISTER, FamilyConstants.RELATIONSHIP.FAMILY);

        presenter = spy(presenter);
        presenter.onFetchFamilyHead(familyHead);
        verify(model).setFamilyHeadPersonObject(familyHead);
        verify(interactor).getNextUniqueId(any(), any());
        verify(presenter).startForm(JSON_FORM.FAMILY_MEMBER_REGISTER, null, null, "");
    }


    @Test
    public void testOnFetchFamilyHeadError() throws Exception {

        FamilyMetadata metadata = RevealApplication.getInstance().getMetadata();
        metadata.updateFamilyMemberRegister(FamilyConstants.JSON_FORM.FAMILY_MEMBER_REGISTER, FamilyConstants.TABLE_NAME.FAMILY_MEMBER, FamilyConstants.EventType.FAMILY_MEMBER_REGISTRATION, FamilyConstants.EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, FamilyConstants.CONFIGURATION.FAMILY_MEMBER_REGISTER, FamilyConstants.RELATIONSHIP.FAMILY);

        presenter = spy(presenter);
        doThrow(new RuntimeException()).when(presenter).startForm(JSON_FORM.FAMILY_MEMBER_REGISTER, null, null, "");
        presenter.onFetchFamilyHead(familyHead);
        verify(model).setFamilyHeadPersonObject(familyHead);
        verify(interactor, never()).getNextUniqueId(any(), any());
        verify(presenter).startForm(JSON_FORM.FAMILY_MEMBER_REGISTER, null, null, "");
    }


    @Test
    public void testOnArchiveMemberCompleted() {
        verify(view).getApplicationContext();
        presenter.onArchiveMemberCompleted(true);
        verifyZeroInteractions(interactor);
        verifyZeroInteractions(view);
    }

    @Test
    public void testOnArchiveFamily() {
        presenter.onArchiveFamilyClicked();
        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());
        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals(view.getContext().getString(R.string.confirm_archive_family_message), tv.getText());
        assertEquals(view.getContext().getString(R.string.confirm_archive_family), ((TextView) alertDialog.findViewById(R.id.alertTitle)).getText());
    }

    @Test
    public void testOnArchiveFamilyClicked() {
        String familyId = UUID.randomUUID().toString();
        String structureId = UUID.randomUUID().toString();
        Whitebox.setInternalState(presenter, "familyBaseEntityId", familyId);
        Whitebox.setInternalState(presenter, "structureId", structureId);
        presenter.onArchiveFamilyClicked();
        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(interactor).archiveFamily(familyId, structureId);
        verify(view).showProgressDialog(org.smartregister.family.R.string.saving_dialog_title);
        assertFalse(alertDialog.isShowing());
    }

    @Test
    public void testOnArchiveFamilyCompleted() {
        Task task = TestingUtils.getTask("12");
        Whitebox.setInternalState(presenter, "structureId", "12");
        presenter.onArchiveFamilyCompleted(true, task);
        verify(view).hideProgressDialog();
        verify(view).returnToMapView(task.getForEntity(), task);

    }

    @Test
    public void testOnArchiveFamilyFailed() {
        Whitebox.setInternalState(presenter, "familyName", "Otala");
        presenter.onArchiveFamilyCompleted(false, null);
        verify(view).hideProgressDialog();
        AlertDialog alertDialog = (AlertDialog) ShadowAlertDialog.getLatestDialog();
        assertTrue(alertDialog.isShowing());
        TextView tv = alertDialog.findViewById(android.R.id.message);
        assertEquals("Archiving family Otala failed", tv.getText());
    }

    private Cursor createCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                STRUCTURE_ID
        });
        cursor.addRow(new Object[]{
                structureId
        });
        return cursor;
    }


}
