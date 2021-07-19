package org.smartregister.reveal.util;

import android.content.Context;

import androidx.core.util.Pair;

import com.mapbox.geojson.Feature;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.Geometry;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.Obs;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.MosquitoHarvestCardDetails;
import org.smartregister.reveal.model.StructureTaskDetails;
import org.smartregister.reveal.util.Constants.JsonForm;
import org.smartregister.util.AssetHandler;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.vijay.jsonwizard.constants.JsonFormConstants.TEXT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.BEDNET_DISTRIBUTION_EVENT;
import static org.smartregister.reveal.util.Constants.BEHAVIOUR_CHANGE_COMMUNICATION;
import static org.smartregister.reveal.util.Constants.BLOOD_SCREENING_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.CASE_CONFIRMATION_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.CDD_SUPERVISOR_DAILY_SUMMARY;
import static org.smartregister.reveal.util.Constants.EventType.DAILY_SUMMARY_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.IRS_FIELD_OFFICER_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.IRS_LITE_VERIFICATION;
import static org.smartregister.reveal.util.Constants.EventType.IRS_SA_DECISION_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.IRS_VERIFICATION;
import static org.smartregister.reveal.util.Constants.EventType.MDA_ADHERENCE;
import static org.smartregister.reveal.util.Constants.EventType.MDA_DISPENSE;
import static org.smartregister.reveal.util.Constants.EventType.MOBILIZATION_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.PAOT_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.TABLET_ACCOUNTABILITY_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.TEAM_LEADER_DOS_EVENT;
import static org.smartregister.reveal.util.Constants.EventType.VERIFICATION_EVENT;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.CASE_CONFIRMATION;
import static org.smartregister.reveal.util.Constants.Intervention.CDD_SUPERVISION;
import static org.smartregister.reveal.util.Constants.Intervention.IRS;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.PAOT;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Constants.STRUCTURE;
import static org.smartregister.reveal.util.Constants.TASK_RESET_EVENT;
import static org.smartregister.reveal.util.Constants.Tags.HEALTH_CENTER;
import static org.smartregister.reveal.util.Utils.getPropertyValue;

/**
 * Created by Vincent Karuri on 25/04/2019
 */
public class RevealJsonFormUtilsTest extends BaseUnitTest {
    private final static String BASIC_EMPTY_FORM = (
            "{\"count\": \"1\", \"step1\": {\"fields\": []}}");
    private final static String BASIC_CHECKBOX_FORM = (
            "{\"count\": \"1\",\"form_version\": \"0.0.1\",\"step1\": {\"fields\": [" +
                    "      {" +
                    "        \"key\": \"testMicrosResult\"," +
                    "        \"openmrs_entity_parent\": \"\"," +
                    "        \"openmrs_entity\": \"\"," +
                    "        \"openmrs_entity_id\": \"\"," +
                    "        \"type\": \"check_box\"," +
                    "        \"label\": \"Microscopy Result\"," +
                    "        \"combine_checkbox_option_values\": \"true\"," +
                    "        \"options\": [" +
                    "          {" +
                    "            \"key\": \"Negative\"," +
                    "            \"text\": \"Negative\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"PositiveFalciparum\"," +
                    "            \"text\": \"Positive - Falciparum\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"PositiveVivax\"," +
                    "            \"text\": \"Positive - Vivax\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"PositiveMalariae\"," +
                    "            \"text\": \"Positive - Malariae\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"PositiveOvalae\"," +
                    "            \"text\": \"Positive - Ovalae\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"PositiveKnowelsi\"," +
                    "            \"text\": \"Positive - Knowelsi\"" +
                    "          }," +
                    "          {" +
                    "            \"key\": \"Fg\"," +
                    "            \"text\": \"Fg\"" +
                    "          }" +
                    "        ]," +
                    "        \"exclusive\": [" +
                    "          \"Negative\"" +
                    "        ]," +
                    "        \"relevance\": {" +
                    "          \"step1:testType\": {" +
                    "            \"type\": \"string\"," +
                    "            \"ex\": \"equalTo(., \\\"Microscopy\\\")\"" +
                    "          }" +
                    "        }" +
                    "      }" +
                    "    ]" +
                    "  }" +
                    "}");

    private RevealJsonFormUtils revealJsonFormUtils;

    @Mock
    private LocationHelper locationHelper;

    @Mock
    private Map<String, Object> serverConfigs;

    private Context context = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        revealJsonFormUtils = new RevealJsonFormUtils();
        Whitebox.setInternalState(revealJsonFormUtils, "locationHelper", locationHelper);
        Whitebox.setInternalState(RevealApplication.getInstance(), "serverConfigs", serverConfigs);
    }

    @Test
    public void testGetFormNameCountryZambiaShouldReturnLarvalDippingFormForLarvalDippingEvent() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        assertEquals(JsonForm.LARVAL_DIPPING_FORM, revealJsonFormUtils.getFormName(LARVAL_DIPPING_EVENT, null));
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetFormNameShouldReturnThailandLarvalDippingFormForLarvalDippingIntervention() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        assertEquals(JsonForm.THAILAND_EN_LARVAL_DIPPING_FORM, revealJsonFormUtils.getFormName(null, LARVAL_DIPPING));
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetFormNameCountryZambiaShouldReturnMosquitoCollectionFormForLarvalDippingEvent() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        assertEquals(JsonForm.MOSQUITO_COLLECTION_FORM, revealJsonFormUtils.getFormName(MOSQUITO_COLLECTION_EVENT, null));
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetFormNameShouldReturnMosquitoCollectionFormForMosquitoCollectionIntervention() {
        List<Country> excludedCountries = Arrays.asList(Country.ZAMBIA, Country.NAMIBIA, Country.BOTSWANA, Country.KENYA, Country.SENEGAL);
        Collections.shuffle(excludedCountries);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, excludedCountries.get(0));
        assertEquals(JsonForm.MOSQUITO_COLLECTION_FORM, revealJsonFormUtils.getFormName(null, MOSQUITO_COLLECTION));
    }

    @Test
    public void testGetFormNameShouldReturnPAOTForm() {
        List<Country> countryList = Arrays.asList(Country.values());
        List<Country> excludedCountries = new ArrayList<>(countryList.size());
        for (Country country : countryList) {
            if (country.equals(Country.THAILAND) || country.equals(Country.REFAPP)) continue;
            excludedCountries.add(country);
        }
        Collections.shuffle(excludedCountries);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, excludedCountries.get(0));
        assertEquals(JsonForm.PAOT_FORM, revealJsonFormUtils.getFormName(null, PAOT));
    }

    @Test
    public void testGetFormNameCountryThailandShouldReturnThailandPAOTForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        assertEquals(JsonForm.THAILAND_PAOT_FORM, revealJsonFormUtils.getFormName(null, PAOT));
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }


    @Test
    public void testPopulatePAOTForm() throws JSONException {
        MosquitoHarvestCardDetails cardDetails = new MosquitoHarvestCardDetails("Active", "2019-07-30", null, PAOT);
        cardDetails.setComments("Paot point");
        JSONObject form = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.PAOT_FORM, context));
        revealJsonFormUtils.populatePAOTForm(cardDetails, form);
        assertEquals("Active", JsonFormUtils.getFieldValue(JsonFormUtils.fields(form), JsonForm.PAOT_STATUS));
        assertEquals("2019-07-30", JsonFormUtils.getFieldValue(JsonFormUtils.fields(form), JsonForm.LAST_UPDATED_DATE));
        assertEquals("Paot point", JsonFormUtils.getFieldValue(JsonFormUtils.fields(form), JsonForm.PAOT_COMMENTS));
    }

    @Test
    public void testPopulateField() throws JSONException {
        JSONObject form = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.ADD_STRUCTURE_FORM, context));
        assertEquals("", JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(form), JsonForm.SELECTED_OPERATIONAL_AREA_NAME).get(TEXT).toString());

        revealJsonFormUtils.populateField(form, Constants.JsonForm.SELECTED_OPERATIONAL_AREA_NAME, "TLV1", TEXT);
        assertEquals("TLV1", JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(form), JsonForm.SELECTED_OPERATIONAL_AREA_NAME).get(TEXT));
    }

    @Test
    public void testGetFormJSON() throws JSONException {
        StructureTaskDetails task = new StructureTaskDetails("d12202fb-d347-4d7a-8859-fb370304c34c");
        task.setBusinessStatus("Not Visited");
        task.setTaskEntity("c72310fd-9c60-403e-a6f8-e38bf5d6359b");
        task.setStructureId("e5246812-f66c-41d9-8739-464f913b112d");
        task.setTaskCode("Blood Screening");
        task.setTaskStatus("READY");
        task.setTaskName("Yogi  Feri, 9");
        task.setTaskAction("Record\n" +"Screening");

        Location structure = new Location();
        structure.setId("e5246812-f66c-41d9-8739-464f913b112d");
        structure.setServerVersion(1569490867604L);
        structure.setSyncStatus("Synced");
        structure.setType("Feature");
        structure.setJurisdiction(false);

        Geometry g = new Geometry();
        g.setType(Geometry.GeometryType.MULTI_POLYGON);
        structure.setGeometry(g);

        LocationProperty lp = new LocationProperty();
        lp.setParentId("6fffaf7f-f16f-4713-a1ac-0cf6e2fe7f2a");
        HashMap<String, String> hm = new HashMap<>();
        hm.put("houseNumber", "6533");
        lp.setCustomProperties(hm);
        lp.setStatus(LocationProperty.PropertyStatus.ACTIVE);
        structure.setProperties(lp);

        JSONObject jsonObject = revealJsonFormUtils.getFormJSON(context, JsonForm.BLOOD_SCREENING_FORM, task, structure);
        assertEquals(jsonObject.getJSONObject("details").getString(Constants.Properties.FORM_VERSION), "0.0.1");
    }

    @Test
    public void testGetFormJsonFromFeature() throws JSONException {
        Feature structure = TestingUtils.getStructure();
        String expectedTaskIdentifier = getPropertyValue(structure, Constants.Properties.TASK_IDENTIFIER);
        JSONObject jsonObject = revealJsonFormUtils.getFormJSON(context, JsonForm.SPRAY_FORM, structure, Constants.BusinessStatus.SPRAYED, "John");
        assertNotNull(jsonObject);
        assertEquals(structure.id(), jsonObject.getJSONObject("details").getString(Constants.Properties.LOCATION_ID));
        assertEquals(expectedTaskIdentifier, jsonObject.getJSONObject("details").getString(Constants.Properties.TASK_IDENTIFIER));
    }

    @Test
    public void testCreateEvent() {

        String baseEntityId = UUID.randomUUID().toString();
        String locationId = UUID.randomUUID().toString();
        Map<String, String> details = new HashMap<>();
        String  eventType =  TASK_RESET_EVENT;
        String entityType = STRUCTURE;

        Event actualEvent = revealJsonFormUtils.createTaskEvent(baseEntityId, locationId, details, eventType, entityType);

        assertEquals(baseEntityId, actualEvent.getBaseEntityId());
        assertEquals(locationId, actualEvent.getLocationId());
        assertEquals(eventType, actualEvent.getEventType());
        assertEquals(entityType, actualEvent.getEntityType());
        assertEquals(baseEntityId, actualEvent.getBaseEntityId());
    }

    @Test
    public void testGetNamibiaSprayForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, IRS);
        assertEquals(JsonForm.SPRAY_FORM_NAMIBIA, actualFormName);
    }

    @Test
    public void testGetBotswanaSprayForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.BOTSWANA);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, IRS);
        assertEquals(JsonForm.SPRAY_FORM_BOTSWANA, actualFormName);
    }

    @Test
    public void testGetZambiaSprayForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, IRS);
        assertEquals(JsonForm.SPRAY_FORM_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetThailandSprayForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, IRS);
        assertEquals(JsonForm.THAILAND_SPRAY_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppSprayForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, IRS);
        assertEquals(JsonForm.SPRAY_FORM_NAMIBIA, actualFormName);
    }

    @Test
    public void testGetRefAppBloodScreeningForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(BLOOD_SCREENING_EVENT, null);
        assertEquals(JsonForm.THAILAND_EN_BLOOD_SCREENING_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppLarvalDippingForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(LARVAL_DIPPING_EVENT, null);
        assertEquals(JsonForm.THAILAND_EN_LARVAL_DIPPING_FORM, actualFormName);
    }

    @Test
    public void testGetThailandBednetDistributionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(BEDNET_DISTRIBUTION_EVENT, BEDNET_DISTRIBUTION);
        assertEquals(JsonForm.THAILAND_BEDNET_DISTRIBUTION_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppBednetDistributionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(BEDNET_DISTRIBUTION_EVENT, BEDNET_DISTRIBUTION);
        assertEquals(JsonForm.THAILAND_EN_BEDNET_DISTRIBUTION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandCaseConfirmationForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(CASE_CONFIRMATION_EVENT, CASE_CONFIRMATION);
        assertEquals(JsonForm.THAILAND_CASE_CONFIRMATION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandLarvalDippingForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(LARVAL_DIPPING_EVENT, null);
        assertEquals(JsonForm.THAILAND_LARVAL_DIPPING_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppCaseConfirmationForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(CASE_CONFIRMATION_EVENT, CASE_CONFIRMATION);
        assertEquals(JsonForm.CASE_CONFIRMATION_FORM, actualFormName);
    }


    @Test
    public void testGetBednetDistributionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.BOTSWANA);
        String actualFormName = revealJsonFormUtils.getFormName(BEDNET_DISTRIBUTION_EVENT, BEDNET_DISTRIBUTION);
        assertEquals(JsonForm.BEDNET_DISTRIBUTION_FORM, actualFormName);
    }

    @Test
    public void testGetCaseConfirmationDistributionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.BOTSWANA);
        String actualFormName = revealJsonFormUtils.getFormName(CASE_CONFIRMATION_EVENT, CASE_CONFIRMATION);
        assertEquals(JsonForm.CASE_CONFIRMATION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandBloodScreeningForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(BLOOD_SCREENING_EVENT, BLOOD_SCREENING);
        assertEquals(JsonForm.THAILAND_BLOOD_SCREENING_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENMosquitoCollectionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(MOSQUITO_COLLECTION_EVENT, MOSQUITO_COLLECTION);
        assertEquals(JsonForm.THAILAND_EN_MOSQUITO_COLLECTION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENLarvalDippingForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(LARVAL_DIPPING_EVENT, LARVAL_DIPPING);
        assertEquals(JsonForm.THAILAND_EN_LARVAL_DIPPING_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENBednetDistributionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(BEDNET_DISTRIBUTION_EVENT, BEDNET_DISTRIBUTION);
        assertEquals(JsonForm.THAILAND_EN_BEDNET_DISTRIBUTION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENBloodScreeningForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(BLOOD_SCREENING_EVENT, BLOOD_SCREENING);
        assertEquals(JsonForm.THAILAND_EN_BLOOD_SCREENING_FORM, actualFormName);
    }

    @Test
    public void testGetThailandBCCForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(BEHAVIOUR_CHANGE_COMMUNICATION, BCC);
        assertEquals(JsonForm.THAILAND_BEHAVIOUR_CHANGE_COMMUNICATION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENBCCForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(BEHAVIOUR_CHANGE_COMMUNICATION, BCC);
        assertEquals(JsonForm.BEHAVIOUR_CHANGE_COMMUNICATION_FORM, actualFormName);
    }

    @Test
    public void testGetThailandENAddStructureForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        String actualFormName = revealJsonFormUtils.getFormName(REGISTER_STRUCTURE_EVENT, null);
        assertEquals(JsonForm.ADD_STRUCTURE_FORM, actualFormName);
    }

    @Test
    public void testGetThailandAddStructureForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(REGISTER_STRUCTURE_EVENT, null);
        assertEquals(JsonForm.THAILAND_ADD_STRUCTURE_FORM, actualFormName);
    }

    @Test
    public void testGetNamibiaAddStructureForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        String actualFormName = revealJsonFormUtils.getFormName(REGISTER_STRUCTURE_EVENT, null);
        assertEquals(JsonForm.NAMIBIA_ADD_STRUCTURE_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppPAOTForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(PAOT_EVENT, null);
        assertEquals(JsonForm.PAOT_FORM, actualFormName);
    }

    @Test
    public void testGetZambiaMDAAdherenceForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(MDA_ADHERENCE, Constants.Intervention.MDA_ADHERENCE);
        assertEquals(JsonForm.ZAMBIA_MDA_ADHERENCE_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppMDAAdherenceForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(MDA_ADHERENCE, Constants.Intervention.MDA_ADHERENCE);
        assertEquals(JsonForm.REFAPP_MDA_ADHERENCE_FORM, actualFormName);
    }

    @Test
    public void testGetZambiaMDADispenceForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(MDA_DISPENSE, Constants.Intervention.MDA_DISPENSE);
        assertEquals(JsonForm.ZAMBIA_MDA_DISPENSE_FORM, actualFormName);
    }

    @Test
    public void testGetRefAppMDADispenseForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(MDA_DISPENSE, Constants.Intervention.MDA_DISPENSE);
        assertEquals(JsonForm.REFAPP_MDA_DISPENSE_FORM, actualFormName);
    }

    @Test
    public void testGetZambiaIRSVerificationForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", Boolean.FALSE);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_VERIFICATION, null);
        assertEquals(JsonForm.ZAMBIA_IRS_VERIFICATION_FORM, actualFormName);
    }

    @Test
    public void testGetZambiaDailySummaryForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(DAILY_SUMMARY_EVENT, null);
        assertEquals(JsonForm.DAILY_SUMMARY_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetZambiaIRSFieldOfficerForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_FIELD_OFFICER_EVENT, null);
        assertEquals(JsonForm.IRS_FIELD_OFFICER_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetZambiaIRSSADecisionForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_SA_DECISION_EVENT, null);
        assertEquals(JsonForm.IRS_SA_DECISION_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetZambiaMobilizationForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(MOBILIZATION_EVENT, null);
        assertEquals(JsonForm.MOBILIZATION_FORM_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetTeamLeaderDOSForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(TEAM_LEADER_DOS_EVENT, null);
        assertEquals(JsonForm.TEAM_LEADER_DOS_ZAMBIA, actualFormName);
    }

    @Test
    public void testGetVerificationEventForm() {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        String actualFormName = revealJsonFormUtils.getFormName(VERIFICATION_EVENT, null);
        assertEquals(JsonForm.VERIFICATION_FORM_ZAMBIA, actualFormName);
    }

    @Test
    public void testPopulateUserAssignedLocations() throws Exception {

        JSONObject formJSON = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.TEAM_LEADER_DOS_ZAMBIA, context));
        List<String> defaultLocations = new ArrayList<>();
        defaultLocations.add("Lusaka");
        when(locationHelper.generateDefaultLocationHierarchy(any())).thenReturn(defaultLocations);

        Whitebox.invokeMethod(revealJsonFormUtils, "populateUserAssignedLocations", formJSON, JsonForm.ZONE, Arrays.asList(HEALTH_CENTER));
        assertEquals("Lusaka", JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formJSON), JsonForm.ZONE).getJSONArray("keys").get(0));
        assertEquals("Lusaka", JsonFormUtils.getFieldJSONObject(JsonFormUtils.fields(formJSON), JsonForm.ZONE).getJSONArray("values").get(0));
    }

    @Test
    public void testPopulateIRSSADecisionFormWithServerOptions() throws JSONException {

        JSONObject formJSON = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.IRS_SA_DECISION_ZAMBIA, context));
        revealJsonFormUtils = spy(revealJsonFormUtils);
        Map<String, JSONObject> fieldsMap = revealJsonFormUtils.getFields(formJSON);
        PreferencesUtil.getInstance().setCurrentDistrict("Lusaka");
        revealJsonFormUtils.populateFormWithServerOptions(JsonForm.IRS_SA_DECISION_ZAMBIA,formJSON);

        verify(revealJsonFormUtils).populateServerOptions(serverConfigs, Constants.CONFIGURATION.SUPERVISORS, fieldsMap.get(JsonForm.SUPERVISOR), "Lusaka");
    }

    @Test
    public void testPopulateVerificationFormWithServerOptions() throws JSONException {

        JSONObject formJSON = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.VERIFICATION_FORM_ZAMBIA, context));
        revealJsonFormUtils = spy(revealJsonFormUtils);
        Map<String, JSONObject> fieldsMap = revealJsonFormUtils.getFields(formJSON);
        PreferencesUtil.getInstance().setCurrentDistrict("Lusaka");
        revealJsonFormUtils.populateFormWithServerOptions(JsonForm.VERIFICATION_FORM_ZAMBIA,formJSON);

        verify(revealJsonFormUtils).populateServerOptions(serverConfigs, Constants.CONFIGURATION.FIELD_OFFICERS, fieldsMap.get(JsonForm.FIELD_OFFICER), "Lusaka");
    }

    @Test
    public void testGetFormName() {
        revealJsonFormUtils = spy(revealJsonFormUtils);
        revealJsonFormUtils.getFormName("X");
        verify(revealJsonFormUtils).getFormName("X", null);
    }

    @Test
    public void testGenerateRepeatingGroupFields() throws JSONException {
        JSONObject mockedObject = mock(JSONObject.class);
        JSONObject formObject = mock(JSONObject.class);
        JSONArray mockedJsonArray= mock(JSONArray.class);
        Obs obs = mock(Obs.class);
        List<Obs> mockedObs = new ArrayList<>();
        mockedObs.add(obs);
        List<Object> obsValues = new ArrayList<>();
        obsValues.add("some value");

        when(obs.getValues()).thenReturn(obsValues);
        when(obs.getFormSubmissionField()).thenReturn("field_name");
        when(mockedObject.optString(JsonFormConstants.KEY)).thenReturn("field");
        when(mockedJsonArray.length()).thenReturn(1);
        when(mockedJsonArray.optJSONObject(0)).thenReturn(mockedObject);
        when(mockedObject.optJSONArray(JsonFormConstants.VALUE)).thenReturn(mockedJsonArray);
        when(formObject.optJSONObject(JsonFormConstants.STEP1)).thenReturn(formObject);
        when(formObject.optJSONArray(JsonFormConstants.FIELDS)).thenReturn(mockedJsonArray);
        when(mockedObject.toString()).thenReturn("{\"type\"=\"label\", \"key\"=\"field\" }");

        revealJsonFormUtils.generateRepeatingGroupFields(mockedObject, mockedObs, formObject);
        verify(mockedJsonArray).put(anyInt(), any());
    }

    @Test
    public void testGetSenegalSprayForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(SPRAY_EVENT, null);
        assertEquals(JsonForm.SPRAY_FORM_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetThailandMosquitoCollectionForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        String actualFormName = revealJsonFormUtils.getFormName(MOSQUITO_COLLECTION_EVENT, null);
        assertEquals(JsonForm.THAILAND_MOSQUITO_COLLECTION_FORM, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetRefAppMosquitoCollectionForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.REFAPP);
        String actualFormName = revealJsonFormUtils.getFormName(MOSQUITO_COLLECTION_EVENT, null);
        assertEquals(JsonForm.THAILAND_EN_MOSQUITO_COLLECTION_FORM, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetIRSLiteVerificationForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", true);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_LITE_VERIFICATION, null);
        assertEquals(JsonForm.IRS_LITE_VERIFICATION, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", false);
    }

    @Test
    public void testGetSenegalDailySummaryForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(DAILY_SUMMARY_EVENT, null);
        assertEquals(JsonForm.DAILY_SUMMARY_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetSenegalIRSFieldOfficerForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_FIELD_OFFICER_EVENT, null);
        assertEquals(JsonForm.IRS_FIELD_OFFICER_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetSenegalIRSSADecisionForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(IRS_SA_DECISION_EVENT, null);
        assertEquals(JsonForm.IRS_SA_DECISION_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetSenegalMobilizationForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(MOBILIZATION_EVENT, null);
        assertEquals(JsonForm.MOBILIZATION_FORM_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetSenegalTeamLeaderDOSForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(TEAM_LEADER_DOS_EVENT, null);
        assertEquals(JsonForm.TEAM_LEADER_DOS_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetSenegalVerificationForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(VERIFICATION_EVENT, null);
        assertEquals(JsonForm.VERIFICATION_FORM_SENEGAL, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }

    @Test
    public void testGetTabletAccountabilityForm() {
        Country buildCountry = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.SENEGAL);
        String actualFormName = revealJsonFormUtils.getFormName(TABLET_ACCOUNTABILITY_EVENT, null);
        assertEquals(JsonForm.TABLET_ACCOUNTABILITY_FORM, actualFormName);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, buildCountry);
    }


    @Test
    public void testGetCDDSupervisionForm(){
        Whitebox.setInternalState(BuildConfig.class,BuildConfig.BUILD_COUNTRY,Country.KENYA);
        String actualFormName = revealJsonFormUtils.getFormName(CDD_SUPERVISOR_DAILY_SUMMARY,null);
        assertEquals(JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM,actualFormName);
    }

    @Test
    public void testGetCDDSupervisionFormFromTaskCode(){
        Whitebox.setInternalState(BuildConfig.class,BuildConfig.BUILD_COUNTRY,Country.KENYA);
        String actualFormName = revealJsonFormUtils.getFormName(null,CDD_SUPERVISION);
        assertEquals(JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM,actualFormName);
    }

    @Test
    public void testPopulateCDDSupervisionFormWithServerOptions() throws JSONException {
        JSONObject formJSON = new JSONObject(AssetHandler.readFileFromAssetsFolder(JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM, context));
        revealJsonFormUtils = spy(revealJsonFormUtils);
        Map<String,JSONObject> fieldsMap = revealJsonFormUtils.getFields(formJSON);
        PreferencesUtil.getInstance().setCurrentOperationalArea("emuhaya");
        revealJsonFormUtils.populateFormWithServerOptions(JsonForm.CDD_SUPERVISOR_DAILY_SUMMARY_FORM,formJSON);

        verify(revealJsonFormUtils).populateServerOptions(serverConfigs,Constants.CONFIGURATION.HEALTH_WORKER_SUPERVISORS,fieldsMap.get(JsonForm.HEALTH_WORKER_SUPERVISOR),"emuhaya");
        verify(revealJsonFormUtils).populateServerOptions(serverConfigs,Constants.CONFIGURATION.COMMUNITY_DRUG_DISTRIBUTORS,fieldsMap.get(JsonForm.COMMUNITY_DRUG_DISTRIBUTOR_NAME),"emuhaya");

    }

    @SuppressWarnings("SameParameterValue")
    private Map<String, Object> getCheckBoxSampleResultValuesFrom(JSONObject jsonObject, int index) throws JSONException {
        JSONArray firstField = jsonObject.getJSONObject(JsonFormConstants.STEP1)
                .getJSONArray(JsonFormConstants.FIELDS)
                .getJSONObject(index)
                .getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
        Map<String, Object> keyAndTextMap = new HashMap<>(firstField.length());
        for (int i = 0; i < firstField.length(); i++) {
            JSONObject option = firstField.getJSONObject(i);
            keyAndTextMap.put(option.getString(JsonFormConstants.KEY), option.getString(TEXT));
        }
        return keyAndTextMap;
    }

    @Test
    public void testPopulateFormFieldCheckBox() throws JSONException {
        final int sampledIndex = 0;
        JSONObject formJSON = new JSONObject(BASIC_CHECKBOX_FORM);
        org.smartregister.domain.Event basicEvent = mock(org.smartregister.domain.Event.class);
        Obs obs1 = spy(Obs.class);
        Map<String, Object> choices = getCheckBoxSampleResultValuesFrom(formJSON, sampledIndex);
        obs1.setValues(new ArrayList<>(choices.values()));
        when(basicEvent.findObs(isNull(), eq(false), any())).thenReturn(obs1);

        revealJsonFormUtils.populateForm(basicEvent, formJSON);
        verify(basicEvent).findObs(isNull(), eq(false), any());
        JSONObject sampledField = JsonFormUtils.fields(formJSON).getJSONObject(sampledIndex);
        assertTrue(sampledField.has(JsonFormConstants.VALUE));
        assertThat(sampledField.getJSONArray(JsonFormConstants.VALUE).toString(),
                is(equalTo(new JSONArray(choices.keySet()).toString())));
    }

    private org.smartregister.domain.Event eventFor(Obs obs){
        org.smartregister.domain.Event event = mock(org.smartregister.domain.Event.class);
        when(event.findObs(isNull(), eq(false), any())).thenReturn(obs);
        return event;
    }

    @Test
    public void testPopulateFormNonEditableKenyaSetsFieldReadOnly() throws JSONException {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.KENYA);
        Obs obs = new Obs();
        obs.setValues(new ArrayList<>());
        org.smartregister.domain.Event event = eventFor(obs);

        final String testKey = "field_testing_key";
        JSONObject fieldJsonObject = mock(JSONObject.class);
        when(fieldJsonObject.getString(eq(JsonFormConstants.KEY))).thenReturn(testKey);

        JSONObject formJSON = new JSONObject(BASIC_EMPTY_FORM);
        JsonFormUtils.fields(formJSON)
                .put(fieldJsonObject);

        Set<String> nonEditables = spy(new HashSet<>());
        when(nonEditables.contains(eq(testKey))).thenReturn(true);
        Whitebox.setInternalState(revealJsonFormUtils, "nonEditablefields", nonEditables);

        revealJsonFormUtils.populateForm(event, formJSON);
        verify(fieldJsonObject).put(eq(JsonFormConstants.READ_ONLY), eq(true));
    }

    @Test
    public void testPopulateFormNonEditableNamibia() throws JSONException {
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        Obs obs = new Obs();
        obs.setValue(JsonForm.YES);
        org.smartregister.domain.Event event = eventFor(obs);

        JSONObject fieldJsonObject = mock(JSONObject.class);
        final String testKey = "namibia_test_key";
        final String typeTest = "type_test";
        when(fieldJsonObject.getString(eq(JsonFormConstants.KEY))).thenReturn(testKey);
        when(fieldJsonObject.optString(eq(JsonFormConstants.TYPE))).thenReturn(typeTest);

        JSONObject formJson = new JSONObject(BASIC_EMPTY_FORM);
        JsonFormUtils.fields(formJson)
                .put(fieldJsonObject);
        Set<String> nonEditableFields = spy(new HashSet<>());
        when(nonEditableFields.contains(eq(testKey))).thenReturn(true);
        Whitebox.setInternalState(revealJsonFormUtils, "nonEditablefields", nonEditableFields);

        revealJsonFormUtils.populateForm(event, formJson);
        verify(fieldJsonObject).put(eq(JsonFormConstants.VALUE), any());
        verify(fieldJsonObject).put(eq(JsonFormConstants.READ_ONLY), eq(true));
        verify(fieldJsonObject).remove(eq(JsonFormConstants.RELEVANCE));
    }

    @SuppressWarnings("SameParameterValue")
    private JSONArray exampleSettingsConfigJSONArray(final String filterKey) throws JSONException {
        JSONArray options = new JSONArray();
        options.put(new JSONObject());
        JSONObject keyJSONObjectOptions = new JSONObject();
        keyJSONObjectOptions.put(filterKey, options);
        JSONArray testServerConfigJSONArray = mock(JSONArray.class);
        when(testServerConfigJSONArray.optJSONObject(eq(0))).thenReturn(keyJSONObjectOptions);
        return testServerConfigJSONArray;
    }

    @Test
    public void testPopulateServerOptions() throws JSONException {
        final String fakeSettingsConfigKey = "dummy_key";
        final String fakeFilterKey = "dummy_filter_key";
        Map<String, Object> testsServerConfig = spy(new HashMap<>());
        JSONArray testServerConfigJSONArray = exampleSettingsConfigJSONArray(fakeFilterKey);
        testsServerConfig.put(fakeSettingsConfigKey, testServerConfigJSONArray);

        JSONObject testField = mock(JSONObject.class);
        when(testsServerConfig.get(eq(fakeSettingsConfigKey))).thenReturn(testServerConfigJSONArray);

        Pair<JSONArray, JSONArray> keyValuePaired= revealJsonFormUtils.populateServerOptions(testsServerConfig, fakeSettingsConfigKey, testField, fakeFilterKey);
        assertNotNull(keyValuePaired);
        verify(testField).put(eq(JsonFormConstants.KEYS), any(JSONArray.class));
        verify(testField).put(eq(JsonFormConstants.VALUES), any(JSONArray.class));
    }
}

