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
import org.smartregister.domain.PlanDefinition;
import org.smartregister.domain.Task;
import org.smartregister.repository.PlanDefinitionSearchRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.contract.ChildRegisterFragmentContract;
import org.smartregister.reveal.model.ChildModel;
import org.smartregister.reveal.shadow.NativeFormProcessorShadowHelper;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.TaskUtils;
import org.smartregister.reveal.util.TestGenericInteractor;
import org.smartregister.util.NativeFormProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.smartregister.reveal.util.Constants.DatabaseKeys.CHILD_TABLE;
import static org.smartregister.reveal.util.Constants.REGISTER_CHILD_EVENT;

/**
 * @author ronald
 */
@Config(shadows = {NativeFormProcessorShadowHelper.class})
public class ChildRegisterFragmentPresenterTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private ChildRegisterFragmentPresenter presenter;

    @Mock
    private ChildRegisterFragmentContract.View view;

    @Mock
    private ChildModel model;

    @Before
    public void setUp() {
        presenter = new ChildRegisterFragmentPresenter();
        presenter.with(view)
                .withModel(model);

        presenter.withInteractor(new TestGenericInteractor());
    }

    @Test
    public void testSearch() {
        presenter = Mockito.spy(presenter);

        presenter.search(null, null);
        Mockito.verify(presenter).fetchList(Mockito.any(), Mockito.any());
    }

    @Test
    public void testStartMDAForm() throws JSONException {
        // no error
        JSONObject jsonObject = new JSONObject();
        Mockito.doReturn(jsonObject).when(model).getMDAForm(Mockito.any(), Mockito.anyString());

        presenter.startMDAForm(RuntimeEnvironment.application, "12345");
        Mockito.verify(view).startJsonForm(Mockito.any(), Mockito.any());
        Mockito.verify(model).getMDAForm(Mockito.any(), Mockito.eq("12345"));
    }

    @Test
    public void testStartMDAFormNoForm() throws JSONException {
        Mockito.doReturn(null).when(model).getMDAForm(Mockito.any(), Mockito.anyString());
        presenter.startMDAForm(RuntimeEnvironment.application, "12345");
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testStartMDAFormWithError() throws JSONException {
        Mockito.doThrow(new JSONException("Sample")).when(model).getMDAForm(Mockito.any(), Mockito.anyString());
        presenter.startMDAForm(RuntimeEnvironment.application, "12345");
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testStartChildRegistrationForm() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        Mockito.doReturn(jsonObject).when(model).getRegistrationForm(Mockito.any());

        presenter.startChildRegistrationForm(RuntimeEnvironment.application);
        Mockito.verify(view).startJsonForm(Mockito.any(), Mockito.any());
        Mockito.verify(model).getRegistrationForm(Mockito.any());
    }

    @Test
    public void testStartChildRegistrationFormNoForm() throws JSONException {
        Mockito.doReturn(null).when(model).getRegistrationForm(Mockito.any());
        presenter.startChildRegistrationForm(RuntimeEnvironment.application);
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testStartChildRegistrationFormWithError() throws JSONException {
        Mockito.doThrow(new JSONException("Sample")).when(model).getRegistrationForm(Mockito.any());
        presenter.startChildRegistrationForm(RuntimeEnvironment.application);
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testSaveChild() throws Exception {
        JSONObject jsonObject = new JSONObject();
        Mockito.doReturn(jsonObject).when(model).getRegistrationForm(Mockito.any());

        PlanDefinitionSearchRepository planDefinitionSearchRepository = Mockito.mock(PlanDefinitionSearchRepository.class);
        Set<PlanDefinition> planDefinitionSet = new HashSet<>();
        planDefinitionSet.add(new PlanDefinition());

        Mockito.doReturn(planDefinitionSet).when(planDefinitionSearchRepository).findActivePlansByJurisdiction(Mockito.any());
        ReflectionHelpers.setField(presenter, "planDefinitionSearchRepository", planDefinitionSearchRepository);

        TaskUtils taskUtils = Mockito.mock(TaskUtils.class);
        ReflectionHelpers.setField(presenter, "taskUtils", taskUtils);


        String jsonString = "{\"count\": \"1\", \"step1\": {\"title\": \"Add Student\", \"fields\": [{\"key\": \"unique_id\", \"hint\": \"ID\", \"type\": \"hidden\", \"read_only\": true, \"value\": false, \"v_required\": {\"err\": \"Please enter the ID\", \"value\": \"true\"}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"opensrp_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaNoNationalId\", \"type\": \"check_box\", \"label\": \"\", \"options\": [{\"key\": \"noNationalID\", \"text\": \"Child does not have a national ID\", \"value\": \"false\", \"text_size\": \"18px\"}], \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"has_no_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaNationalId\", \"hint\": \"National ID\", \"type\": \"edit_text\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_numeric\": {\"err\": \"Must be a number.\", \"value\": \"true\"}, \"v_required\": {\"err\": \"Please enter the ID\", \"value\": \"true\"}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"national_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaRevealId\", \"hint\": \"Reveal ID\", \"type\": \"hidden\", \"text_size\": \"8sp\", \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_identifier\", \"openmrs_entity_id\": \"reveal_id\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaGivenName\", \"hint\": \"Child's given name\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"first_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaSurname\", \"hint\": \"Child's surname\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"last_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaSex\", \"type\": \"native_radio\", \"label\": \"Child's allocated sex at birth\", \"options\": [{\"key\": \"Male\", \"text\": \"Male\"}, {\"key\": \"Female\", \"text\": \"Female\"}], \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"gender\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaDob\", \"hint\": \"Child's date of birth\", \"type\": \"date_picker\", \"duration\": {\"label\": \"Age\"}, \"expanded\": false, \"max_date\": \"today-5y\", \"min_date\": \"today-120y\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"birthdate\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaDobUnk\", \"type\": \"check_box\", \"label\": \"\", \"options\": [{\"key\": \"sactaDobUnk\", \"text\": \"Date of birth estimated\", \"value\": \"false\", \"text_size\": \"18px\"}], \"openmrs_entity\": \"person\", \"openmrs_entity_id\": \"birthdateApprox\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaAge\", \"hint\": \"Child's age\", \"type\": \"edit_text\", \"v_max\": {\"err\": \"Age must be equal or less than 120\", \"value\": \"120\"}, \"v_min\": {\"err\": \"Age must be equal or greater than 5\", \"value\": \"5\"}, \"read_only\": true, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Please enter the age\", \"value\": true}, \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"age_entered\", \"v_numeric_integer\": {\"err\": \"Please enter a number\", \"value\": \"true\"}, \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaAgeCat\", \"hint\": \"Age category\", \"type\": \"edit_text\", \"read_only\": true, \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-calculation.yml\"}}}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"age_category\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaCurrEnroll\", \"type\": \"native_radio\", \"label\": \"Is this child currently enrolled in school?\", \"options\": [{\"key\": \"Yes\", \"text\": \"Yes\"}, {\"key\": \"No\", \"text\": \"No\"}], \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"school_enrolled\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaCurrSchName\", \"hint\": \"Name of school attending, if from another school\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"school_name\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaGrade\", \"type\": \"native_radio\", \"label\": \"School grade\", \"options\": [{\"key\": \"Grade 1\", \"text\": \"Grade 1\"}, {\"key\": \"Grade 2\", \"text\": \"Grade 2\"}, {\"key\": \"Grade 3\", \"text\": \"Grade 3\"}, {\"key\": \"Grade 4\", \"text\": \"Grade 4\"}, {\"key\": \"Grade 5\", \"text\": \"Grade 5\"}, {\"key\": \"Grade 6\", \"text\": \"Grade 6\"}, {\"key\": \"Grade 7\", \"text\": \"Grade 7\"}, {\"key\": \"Form 1\", \"text\": \"Form 1\"}, {\"key\": \"Form 2\", \"text\": \"Form 2\"}, {\"key\": \"Form 3\", \"text\": \"Form 3\"}, {\"key\": \"Form 4\", \"text\": \"Form 4\"}, {\"key\": \"Form 5\", \"text\": \"Form 5\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"grade\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"sactaClass\", \"hint\": \"Grade class (e.g. 1A, 1B, 1C/D)\", \"type\": \"edit_text\", \"edit_type\": \"name\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd-child-registration-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": \"true\"}, \"openmrs_entity\": \"person_attribute\", \"openmrs_entity_id\": \"grade_class\", \"openmrs_entity_parent\": \"\"}]}, \"metadata\": {\"end\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"end\", \"openmrs_entity_id\": \"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"start\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"start\", \"openmrs_entity_id\": \"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"today\": {\"openmrs_entity\": \"encounter\", \"openmrs_entity_id\": \"encounter_date\", \"openmrs_entity_parent\": \"\"}, \"look_up\": {\"value\": \"\", \"entity_id\": \"\"}, \"deviceid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"deviceid\", \"openmrs_entity_id\": \"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"simserial\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"simserial\", \"openmrs_entity_id\": \"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"phonenumber\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"phonenumber\", \"openmrs_entity_id\": \"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"subscriberid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"subscriberid\", \"openmrs_entity_id\": \"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"encounter_location\": \"\"}, \"entity_id\": \"\", \"relational_id\": \"\", \"encounter_type\": \"Child Registration\"}";
        presenter.saveChild(jsonString, RuntimeEnvironment.application);

        NativeFormProcessor processor = NativeFormProcessorShadowHelper.getProcessorSpy();

        // verify form manipulations
        Mockito.verify(processor).withBindType(CHILD_TABLE);
        Mockito.verify(processor).withEncounterType(REGISTER_CHILD_EVENT);
        Mockito.verify(processor).withEntityId(Mockito.anyString());
        Mockito.verify(processor).tagEventMetadata();
        Mockito.verify(processor).hasClient(true);
        Mockito.verify(processor).saveClient(Mockito.any());
        Mockito.verify(processor).saveEvent();
        Mockito.verify(processor).clientProcessForm();
        Mockito.verify(processor).closeRegistrationID(Mockito.anyString());

        Mockito.verify(view).reloadFromSource();
    }

    @Test
    public void testSaveChildWithError() throws JSONException {
        presenter.saveChild(null, RuntimeEnvironment.application);
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testSaveMDAForm() throws Exception {

        // prepare
        Task task = Mockito.mock(Task.class);
        Mockito.doReturn(task).when(model).getCurrentTask(Mockito.any(), Mockito.any());

        TaskRepository taskRepository = Mockito.mock(TaskRepository.class);
        ReflectionHelpers.setField(presenter, "taskRepository", taskRepository);

        // execute
        String jsonString = "{\"count\": \"1\", \"step1\": {\"title\": \"MDA Dispense\", \"fields\": [{\"key\": \"mmaConsent\", \"type\": \"native_radio\", \"label\": \"Has the child's parent/caregiver granted consent?\", \"options\": [{\"key\": \"Yes\", \"text\": \"Yes\", \"openmrs_entity\": \"Yes\", \"openmrs_entity_id\": \"Yes\"}, {\"key\": \"No\", \"text\": \"No\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"No\"}], \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaConsent\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaheight\", \"hint\": \"Height in cm\", \"type\": \"edit_text\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"Required field\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaheight\", \"v_numeric_integer\": {\"err\": \"Please enter a number\", \"value\": \"true\"}, \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaDrugAdmin\", \"type\": \"native_radio\", \"label\": \"Was the drug administered?\", \"options\": [{\"key\": \"Yes\", \"text\": \"Yes\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"Yes\"}, {\"key\": \"No\", \"text\": \"No\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"No\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaDrugAdmin\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaNoDrugAdminReason\", \"type\": \"native_radio\", \"label\": \"Why was the drug not administered?\", \"options\": [{\"key\": \"refused\", \"text\": \"Refused\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaNoDrugAdminReason\"}, {\"key\": \"contraindicated\", \"text\": \"Contraindicated\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"contraindicated\"}, {\"key\": \"pregnant\", \"text\": \"Pregnant\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"pregnant\"}, {\"key\": \"sick\", \"text\": \"Sick\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"sick\"}, {\"key\": \"other\", \"text\": \"Other\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"other\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaNoDrugAdminReason\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"other_imaNoDrugAdminReason\", \"hint\": \"Specify other reason\", \"type\": \"edit_text\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"text_size\": \"8sp\", \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"other_imaNoDrugAdminReason\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaTreatCateg\", \"type\": \"native_radio\", \"label\": \"Treatment category\", \"options\": [{\"key\": \"albOneTab\", \"text\": \"Alb 1 tab\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"albOneTab\"}, {\"key\": \"PrazByHeight\", \"text\": \"Praziquental (by height)\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"PrazByHeight\"}, {\"key\": \"PrazPlusAlb\", \"text\": \"Praz (by height) + 1 tab Alb\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"PrazPlusAlb\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaTreatCateg\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaPzqDosage\", \"hint\": \"PZQ pills to be given\", \"type\": \"edit_text\", \"read_only\": \"True\", \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"text_size\": \"8sp\", \"calculation\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-calculation.yml\"}}}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaPzqDosage\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaPzqDosageGiven\", \"type\": \"native_radio\", \"label\": \"Number of pills (PZQ) given\", \"options\": [{\"key\": \"one\", \"text\": \"1\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"1\"}, {\"key\": \"onePointFive\", \"text\": \"1.5\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"1.5\"}, {\"key\": \"two\", \"text\": \"2\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"2\"}, {\"key\": \"twoPointFive\", \"text\": \"2.5\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"2.5\"}, {\"key\": \"three\", \"text\": \"3\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"3\"}, {\"key\": \"four\", \"text\": \"4\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"4\"}, {\"key\": \"five\", \"text\": \"5\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"5\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"v_required\": {\"err\": \"This field is required\", \"value\": true}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaPzqDosageGiven\", \"openmrs_entity_parent\": \"\"}, {\"key\": \"mmaAlbGiven\", \"type\": \"native_radio\", \"label\": \"Was 1 ALB pill given?\", \"options\": [{\"key\": \"Yes\", \"text\": \"Yes\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"Yes\"}, {\"key\": \"No\", \"text\": \"No\", \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"No\"}], \"relevance\": {\"rules-engine\": {\"ex-rules\": {\"rules-file\": \"ntd_mma_dispense-relevance.yml\"}}}, \"openmrs_entity\": \"concept\", \"openmrs_entity_id\": \"mmaAlbGiven\", \"openmrs_entity_parent\": \"\"}], \"display_back_button\": \"true\"}, \"metadata\": {\"end\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"end\", \"openmrs_entity_id\": \"163138AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"start\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"start\", \"openmrs_entity_id\": \"163137AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"today\": {\"openmrs_entity\": \"encounter\", \"openmrs_entity_id\": \"encounter_date\", \"openmrs_entity_parent\": \"\"}, \"deviceid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"deviceid\", \"openmrs_entity_id\": \"163149AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"simserial\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"simserial\", \"openmrs_entity_id\": \"163151AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"phonenumber\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"phonenumber\", \"openmrs_entity_id\": \"163152AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"subscriberid\": {\"openmrs_entity\": \"concept\", \"openmrs_data_type\": \"subscriberid\", \"openmrs_entity_id\": \"163150AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\", \"openmrs_entity_parent\": \"\"}, \"encounter_location\": \"\"}, \"baseEntityId\": \"12345\", \"encounter_type\": \"mda_dispense\"}";
        presenter.saveMDAForm(jsonString, RuntimeEnvironment.application);

        NativeFormProcessor processor = NativeFormProcessorShadowHelper.getProcessorSpy();
        Mockito.verify(processor).tagLocationData(NativeFormProcessorShadowHelper.getCurrentOperationalArea());
        Mockito.verify(processor).tagEventMetadata();
        Mockito.verify(processor).tagTaskDetails(task);
        Mockito.verify(processor).saveEvent();
        Mockito.verify(processor).clientProcessForm();

        Mockito.verify(task).setBusinessStatus(Constants.BusinessStatus.VISITED_DRUG_NOT_ADMINISTERED);
        Mockito.verify(taskRepository).addOrUpdate(task);
    }


    @Test
    public void testSaveMDAFormWithError() throws JSONException {
        presenter.saveMDAForm(null, RuntimeEnvironment.application);
        Mockito.verify(view).onFetchError(Mockito.any());
    }

    @Test
    public void testFetchReportStats() {
        Map<String, Integer> result = new HashMap<>();
        Mockito.doReturn(result).when(model).getReportCounts();

        presenter.fetchReportStats();
        Mockito.verify(view).onReportCountReloaded(result);
    }

    @Test
    public void testFetchReportStatsWithError() {
        Mockito.doReturn(null).when(model).getReportCounts();
        presenter.fetchReportStats();
        Mockito.verify(view).onFetchError(Mockito.any());
    }
}
