package org.smartregister.reveal.presenter;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.ChildProfileContract;
import org.smartregister.reveal.model.Child;
import org.smartregister.reveal.shadow.NativeFormProcessorShadowHelper;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.TestGenericInteractor;
import org.smartregister.util.NativeFormProcessor;
import org.smartregister.util.QueryComposer;

import static org.smartregister.reveal.util.Constants.DatabaseKeys.CHILD_TABLE;
import static org.smartregister.reveal.util.Constants.EventType.UPDATE_CHILD_REGISTRATION;

/**
 * @author ronald
 */
@Config(shadows = {NativeFormProcessorShadowHelper.class})
public class ChildProfilePresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ChildProfilePresenter presenter;

    @Mock
    private ChildProfileContract.View view;

    @Mock
    private ChildProfileContract.Model model;

    @Before
    public void setUp() {
        presenter = new ChildProfilePresenter(view);
        presenter.usingInteractor(new TestGenericInteractor());
        ReflectionHelpers.setField(presenter, "model", model);
    }

    @Test
    public void testFetchProfileData() throws QueryComposer.InvalidQueryException {
        String baseEntityID = "baseEntityID";

        Child result = new Child();
        Mockito.doReturn(result).when(model).getChild(baseEntityID);

        presenter.fetchProfileData(baseEntityID);
        Mockito.verify(view).onFetchResult(result);
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testFetchProfileDataWithNoChildInfo() throws QueryComposer.InvalidQueryException {
        String baseEntityID = "baseEntityID";

        Mockito.doReturn(null).when(model).getChild(baseEntityID);

        presenter.fetchProfileData(baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testFetchProfileDataWithError() throws QueryComposer.InvalidQueryException {
        String baseEntityID = "baseEntityID";

        Mockito.doThrow(new QueryComposer.InvalidQueryException("Invalid query")).when(model).getChild(baseEntityID);

        presenter.fetchProfileData(baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testStartChildRegistrationForm() throws Exception {
        String baseEntityID = "baseEntityID";

        JSONObject result = new JSONObject();
        Mockito.doReturn(result).when(model).getRegistrationEditForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startChildRegistrationForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).startJsonForm(Mockito.eq(result), Mockito.anyString());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testStartChildRegistrationFormNoForm() throws Exception {
        String baseEntityID = "baseEntityID";
        Mockito.doReturn(null).when(model).getRegistrationEditForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startChildRegistrationForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
        Mockito.verify(model).getRegistrationEditForm(Mockito.any(), Mockito.eq(baseEntityID));
    }

    @Test
    public void testStartChildRegistrationFormWithError() throws Exception {
        String baseEntityID = "baseEntityID";

        Mockito.doThrow(new Exception("Invalid Request")).when(model).getRegistrationEditForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startChildRegistrationForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testStartADRForm() throws JSONException {
        String baseEntityID = "baseEntityID";

        JSONObject result = new JSONObject();
        Mockito.doReturn(result).when(model).getADRForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startADRForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).startJsonForm(Mockito.eq(result), Mockito.anyString());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testStartADRFormNoForm() throws Exception {
        String baseEntityID = "baseEntityID";
        Mockito.doReturn(null).when(model).getADRForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startADRForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
        Mockito.verify(model).getADRForm(Mockito.any(), Mockito.eq(baseEntityID));
    }

    @Test
    public void testStartADRFormWithError() throws Exception {
        String baseEntityID = "baseEntityID";

        Mockito.doThrow(new JSONException("Invalid Form")).when(model).getADRForm(RuntimeEnvironment.application, baseEntityID);

        presenter.startADRForm(RuntimeEnvironment.application, baseEntityID);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testUpdateChild() throws Exception {
        String jsonString = "{\"count\": \"1\", \"step1\": {\"title\": \"Add Student\", \"fields\": [{\"key\": \"unique_id\", \"hint\": \"ID\", \"type\": \"hidden\", \"read_only\": true, \"v_required\": {\"err\": \"Please enter the ID\", \"value\": \"true\"}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"opensrp_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaNoNationalId\", \"type\": \"check_box\", \"label\": \"\", \"options\": [{\"key\": \"noNationalID\", \"text\": \"Child does not have a national ID\", \"value\": \"false\", \"text_size\": \"18px\"}], \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"has_no_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaNationalId\", \"hint\": \"National ID\", \"type\": \"edit_text\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_numeric\": {\"err\": \"Must be a number.\", \"value\": \"true\"}, \"v_required\": {\"err\": \"Please enter the ID\", \"value\": \"true\"}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"national_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaRevealId\", \"hint\": \"Reveal ID\", \"type\": \"hidden\", \"text_size\": \"8sp\", \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"reveal_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaGivenName\", \"hint\": \"Child's given name\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"first_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaSurname\", \"hint\": \"Child's surname\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"last_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaSex\", \"type\": \"native_radio\", \"label\": \"Child's allocated sex at birth\", \"options\": [{\"key\": \"Male\", \"text\": \"Male\"}, {\"key\": \"Female\", \"text\": \"Female\"}], \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"gender\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaDob\", \"hint\": \"Child's date of birth\", \"type\": \"date_picker\", \"duration\": {\"label\": \"Age\"}, \"expanded\": false, \"max_date\": \"today-5y\", \"min_date\": \"today-120y\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"birthdate\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaDobUnk\", \"type\": \"check_box\", \"label\": \"\", \"options\": [{\"key\": \"sactaDobUnk\", \"text\": \"Date of birth estimated\", \"value\": \"false\", \"text_size\": \"18px\"}], \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"birthdateApprox\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaAge\", \"hint\": \"Child's age\", \"type\": \"edit_text\", \"v_max\": {\"err\": \"Age must be equal or less than 120\", \"value\": \"120\"}, \"v_min\": {\"err\": \"Age must be equal or greater than 5\", \"value\": \"5\"}, \"read_only\": true, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Please enter the age\", \"value\": true}, \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"age_entered\", \"v_numeric_integer\": {\"err\": \"Please enter a number\", \"value\": \"true\"}, \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaAgeCat\", \"hint\": \"Age category\", \"type\": \"edit_text\", \"read_only\": true, \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"age_category\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaCurrEnroll\", \"type\": \"native_radio\", \"label\": \"Is this child currently enrolled in school?\", \"options\": [{\"key\": \"Yes\", \"text\": \"Yes\"}, {\"key\": \"No\", \"text\": \"No\"}], \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"school_enrolled\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaCurrSchName\", \"hint\": \"Name of school attending, if from another school\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"school_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaGrade\", \"type\": \"native_radio\", \"label\": \"School grade\", \"options\": [{\"key\": \"Grade 1\", \"text\": \"Grade 1\"}, {\"key\": \"Grade 2\", \"text\": \"Grade 2\"}, {\"key\": \"Grade 3\", \"text\": \"Grade 3\"}, {\"key\": \"Grade 4\", \"text\": \"Grade 4\"}, {\"key\": \"Grade 5\", \"text\": \"Grade 5\"}, {\"key\": \"Grade 6\", \"text\": \"Grade 6\"}, {\"key\": \"Grade 7\", \"text\": \"Grade 7\"}, {\"key\": \"Form 1\", \"text\": \"Form 1\"}, {\"key\": \"Form 2\", \"text\": \"Form 2\"}, {\"key\": \"Form 3\", \"text\": \"Form 3\"}, {\"key\": \"Form 4\", \"text\": \"Form 4\"}, {\"key\": \"Form 5\", \"text\": \"Form 5\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"grade\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaClass\", \"hint\": \"Grade class (e.g. 1A, 1B, 1C/D)\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"grade_class\", \"openmrs_entity_parent\": \"\"}]}, \"metadata\": {\"end\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"end\", \"openmrs_entity_id\": \"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"start\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"start\", \"openmrs_entity_id\": \"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"today\": {\"openmrs_entity\": \"encounter\", \"openmrs_entity_id\": \"encounter_date\", \"openmrs_entity_parent\": \"\"}, \"look_up\": {\"value\": \"\", \"entity_id\": \"\"}, \"deviceid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"deviceid\", \"openmrs_entity_id\": \"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"simserial\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"simserial\", \"openmrs_entity_id\": \"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"phonenumber\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"phonenumber\", \"openmrs_entity_id\": \"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"subscriberid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"subscriberid\", \"openmrs_entity_id\": \"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"encounter_location\": \"\"}, \"entity_id\": \"\", \"baseEntityId\": \"12345\", \"relational_id\": \"\", \"encounter_type\": \"Child Registration\"}";
        presenter.updateChild(new JSONObject(jsonString), RuntimeEnvironment.application);


        NativeFormProcessor processor = NativeFormProcessorShadowHelper.getProcessorSpy();

        // verify form manipulations
        Mockito.verify(processor).withBindType(CHILD_TABLE);
        Mockito.verify(processor).withEncounterType(UPDATE_CHILD_REGISTRATION);
        Mockito.verify(processor).withEntityId(Mockito.anyString());
        Mockito.verify(processor).tagLocationData(NativeFormProcessorShadowHelper.getCurrentOperationalArea());
        Mockito.verify(processor).tagEventMetadata();

        Mockito.verify(processor).hasClient(true);
        Mockito.verify(processor).mergeAndSaveClient();
        Mockito.verify(processor).saveEvent();
        Mockito.verify(processor).clientProcessForm();

        Mockito.verify(view).reloadFromSource();
        view.setLoadingState(false);
    }

    @Test
    public void testUpdateChildWithError() {
        presenter.updateChild(null, RuntimeEnvironment.application);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }

    @Test
    public void testSaveADRForm() throws Exception {

        String jsonString = "{\"count\": \"1\", \"step1\": {\"title\": \"ADR Report\", \"fields\": [{\"key\": \"mmaAdrType\", \"type\": \"check_box\", \"label\": \"ADR type\", \"options\": [{\"key\": \"vomitting\", \"text\": \"Vomitting\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"vomitting\"}, {\"key\": \"rash\", \"text\": \"Rash\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"rash\"}, {\"key\": \"headache\", \"text\": \"Headache\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"headache\"}, {\"key\": \"diarrhea\", \"text\": \"Diarrhea\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"diarrhea\"}, {\"key\": \"convulsions\", \"text\": \"Convulsions\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"convulsions\"}, {\"key\": \"visual_impairment\", \"text\": \"Visual impairment\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"visual_impairment\"}, {\"key\": \"Shock\", \"text\": \"Shock\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"Shock\"}, {\"key\": \"other\", \"text\": \"Other (specify)\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"other\"}], \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"\", \"openmrs_entity_id\": \"\", \"openmrs_entity_parent\": \"\", \"combine_checkbox_option_values\": \"true\"}, {\"key\": \"other_mmaAdrType\", \"hint\": \"Specify other type\", \"type\": \"edit_text\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-mma-adr-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"other_mmaAdrType\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaAdrGrade\", \"type\": \"native_radio\", \"label\": \"ADR grade\", \"options\": [{\"key\": \"mild\", \"text\": \"Mild\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mild\"}, {\"key\": \"moderate\", \"text\": \"Moderate\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"moderate\"}, {\"key\": \"severe\", \"text\": \"Severe\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"severe\"}], \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaAdrGrade\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaAdrOutcome\", \"type\": \"native_radio\", \"label\": \"ADR outcome\", \"options\": [{\"key\": \"resolved\", \"text\": \"Resolved\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"resolved\"}, {\"key\": \"requiredOutpatientTreatment\", \"text\": \"Required outpatient treatment\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"requiredOutpatientTreatment\"}, {\"key\": \"admission\", \"text\": \"Admission\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"admission\"}, {\"key\": \"died\", \"text\": \"Died\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"died\"}], \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"\", \"openmrs_entity_id\": \"\", \"openmrs_entity_parent\": \"\"}], \"display_back_button\": \"true\"}, \"metadata\": {\"end\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"end\", \"openmrs_entity_id\": \"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"start\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"start\", \"openmrs_entity_id\": \"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"today\": {\"openmrs_entity\": \"encounter\", \"openmrs_entity_id\": \"encounter_date\", \"openmrs_entity_parent\": \"\"}, \"deviceid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"deviceid\", \"openmrs_entity_id\": \"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"simserial\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"simserial\", \"openmrs_entity_id\": \"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"phonenumber\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"phonenumber\", \"openmrs_entity_id\": \"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"subscriberid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"subscriberid\", \"openmrs_entity_id\": \"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"encounter_location\": \"\"}, \"entity_id\": \"\", \"baseEntityId\": \"12345\", \"encounter_type\": \"mma_adr\"}";
        presenter.saveADRForm(new JSONObject(jsonString), RuntimeEnvironment.application);

        NativeFormProcessor processor = NativeFormProcessorShadowHelper.getProcessorSpy();

        // verify form manipulations
        Mockito.verify(processor).withBindType(Constants.EventType.MDA_ADVERSE_DRUG_REACTION);
        Mockito.verify(processor).withEncounterType(Constants.EventType.MDA_ADVERSE_DRUG_REACTION);
        Mockito.verify(processor).withEntityId(Mockito.anyString());
        Mockito.verify(processor).tagLocationData(NativeFormProcessorShadowHelper.getCurrentOperationalArea());
        Mockito.verify(processor).tagEventMetadata();

        Mockito.verify(processor).clientProcessForm();
        Mockito.verify(processor).saveEvent();

        Mockito.verify(view).reloadFromSource();
        view.setLoadingState(false);
    }


    @Test
    public void testSaveADRFormWithError() {
        presenter.saveADRForm(null, RuntimeEnvironment.application);
        Mockito.verify(view).onError(Mockito.any());
        Mockito.verify(view).setLoadingState(false);
    }
}
