package org.smartregister.reveal.util;

import android.view.View;

import com.mapbox.geojson.Feature;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.smartregister.domain.Action;
import org.smartregister.domain.Obs;
import org.smartregister.domain.PlanDefinition;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.ADMIN_PASSWORD_NOT_NEAR_STRUCTURES;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.DEFAULT_GEO_JSON_CIRCLE_SIDES;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.DEFAULT_INDEX_CASE_CIRCLE_RADIUS_IN_METRES;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.DISPLAY_ADD_STRUCTURE_OUT_OF_BOUNDARY_WARNING_DIALOG;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.DISPLAY_DISTANCE_SCALE;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.RESOLVE_LOCATION_TIMEOUT_IN_SECONDS;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.VALIDATE_FAR_STRUCTURES;
import static org.smartregister.reveal.util.Constants.Intervention.BCC;
import static org.smartregister.reveal.util.Constants.Intervention.BEDNET_DISTRIBUTION;
import static org.smartregister.reveal.util.Constants.Intervention.BLOOD_SCREENING;
import static org.smartregister.reveal.util.Constants.Intervention.LARVAL_DIPPING;
import static org.smartregister.reveal.util.Constants.Intervention.MOSQUITO_COLLECTION;
import static org.smartregister.reveal.util.Constants.Intervention.REGISTER_FAMILY;
import static org.smartregister.reveal.util.Utils.createCircleFeature;
import static org.smartregister.reveal.util.Utils.displayAddStructureOutOfBoundaryWarningDialog;
import static org.smartregister.reveal.util.Utils.displayDistanceScale;
import static org.smartregister.reveal.util.Utils.getAdminPasswordNotNearStructures;
import static org.smartregister.reveal.util.Utils.getCoordsFromGeometry;
import static org.smartregister.reveal.util.Utils.getDrawOperationalAreaBoundaryAndLabel;
import static org.smartregister.reveal.util.Utils.getInterventionLabel;
import static org.smartregister.reveal.util.Utils.getResolveLocationTimeoutInSeconds;
import static org.smartregister.reveal.util.Utils.isResidentialStructure;
import static org.smartregister.reveal.util.Utils.showWhenTrue;
import static org.smartregister.reveal.util.Utils.validateFarStructures;

/**
 * Created by Vincent Karuri on 08/05/2019
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({PreferencesUtil.class, MapboxMap.class, RevealApplication.class})
public class UtilsTest {

    public static String circleFeatureJSonString = "{\"geometry\":{\"coordinates\":[[[101.19890199019984,15.0913957],[101.1988892394033,15.091869011641458],[101.19885102196272,15.092341025968473],[101.198787442638,15.092810449170178],[101.1986986756701,15.093275994746735],[101.19858496440278,15.093736386462165],[101.19844662049366,15.094190362519466],[101.1982840231337,15.094636678601969],[101.19809761790235,15.095074111581843],[101.19788791588995,15.095501462094832],[101.19765549179704,15.095917559000648],[101.1974009826818,15.096321261806732],[101.19712508613597,15.096711463991884],[101.1968285583727,15.097087096039164],[101.19651221215388,15.097447128367365],[101.1961769145624,15.097790574153018],[101.19582358430188,15.098116492318496],[101.19545319079602,15.098423988695782],[101.19506674794505,15.098712221556715],[101.19466531634164,15.098980399846],[101.19424999484845,15.099227789463226],[101.19382192331562,15.099453711447405],[101.19338227352888,15.099657547368826],[101.19293225210737,15.099838737799658],[101.19247309092155,15.099996786756533],[101.19200605013964,15.100131260473976],[101.19153240822018,15.100241790849067],[101.1910534650738,15.100328074531108],[101.19057053174008,15.100389875330636],[101.19008493362993,15.100427023635312],[101.18959799999999,15.100439417756867],[101.18911106723996,15.100427023679627],[101.18862546739356,15.100389875242128],[101.18814253492619,15.100328074531108],[101.1876635917798,15.100241790849067],[101.18718995070176,15.100131260693127],[101.18672290824996,15.099996786494879],[101.18626374789261,15.099838737799658],[101.1858137264711,15.099657547368826],[101.18537407746051,15.099453711831814],[101.18494600439713,15.099227789039858],[101.18453068365834,15.098980399846],[101.18412925205493,15.098712221556715],[101.18374280988094,15.098423989228648],[101.18337241505074,15.09811649175192],[101.18301908543758,15.097790574153018],[101.18268378813754,15.097447128681988],[101.18236744107908,15.097087095381129],[101.182070913608,15.096711463649372],[101.18179501731818,15.096321261806732],[101.18154050842072,15.095917559367296],[101.18130808371455,15.095501461340387],[101.18109838192048,15.095074111195078],[101.18091197694433,15.094636678799594],[101.18074937970822,15.094190363123436],[101.1806110354281,15.093736385848754],[101.1804973242846,15.093275994539677],[101.18040855741309,15.092810449483794],[101.18034497811692,15.092341026705308],[101.18030676057103,15.091869011165825],[101.18029400980014,15.0913957],[101.18030676057103,15.090922388834175],[101.18034497793482,15.090450374978882],[101.18040855714055,15.089980952188824],[101.18049732464682,15.08951540380386],[101.18061103587902,15.08905501251548],[101.18074937970822,15.088601036876563],[101.18091197694433,15.088154721200405],[101.18109838192048,15.087717288804921],[101.18130808371455,15.087289938659612],[101.18154050754961,15.08687384209929],[101.1817950163693,15.086470139613528],[101.18207091463205,15.086079934980582],[101.18236744217549,15.085704303302798],[101.18268378813754,15.08534427131801],[101.18301908543758,15.085000825846981],[101.18337241505074,15.084674908248079],[101.18374280852699,15.084367411837086],[101.18412925064547,15.08407917943868],[101.18453068511948,15.08381099923167],[101.18494600590593,15.083563610113407],[101.18537407746051,15.083337688168186],[101.1858137264711,15.083133852631173],[101.18626374789261,15.082952662200341],[101.18672290824996,15.08279461350512],[101.18718994901893,15.082660139745174],[101.18766359007566,15.082549609503026],[101.18814253664694,15.082463325203975],[101.18862546912622,15.082401524580856],[101.18911106723996,15.082364376320372],[101.18959799999999,15.082351982243132],[101.19008493276002,15.082364376320372],[101.19057053087376,15.082401524580856],[101.19105346335304,15.082463325203975],[101.19153240992432,15.082549609503026],[101.19200605098105,15.082660139745174],[101.19247309175002,15.08279461350512],[101.19293225210737,15.082952662200341],[101.19338227352888,15.083133852631173],[101.19382192253947,15.083337688168186],[101.19424999409405,15.083563610113407],[101.1946653148805,15.08381099923167],[101.19506674935451,15.08407917943868],[101.19545319147299,15.084367411837086],[101.19582358494924,15.084674908248079],[101.1961769145624,15.085000825846981],[101.19651221186244,15.08534427131801],[101.19682855782449,15.085704303302798],[101.19712508536793,15.086079934980582],[101.19740098363067,15.086470139613528],[101.19765549245037,15.08687384209929],[101.19788791628542,15.087289938659612],[101.1980976180795,15.087717288804921],[101.19828402305565,15.088154721200405],[101.19844662029176,15.088601036876563],[101.19858496412095,15.08905501251548],[101.19869867535316,15.08951540380386],[101.19878744285943,15.089980952188824],[101.19885102206516,15.090450374978882],[101.19888923942895,15.090922388834175]]],\"type\":\"Polygon\"},\"type\":\"Feature\"}";

    public static String originalPolyGonFeatureJson = "{\"type\":\"Feature\",\"id\":\"b95b6d9d-75f0-456d-889d-8b06ad167d36\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[101.887646,13.515772],[101.887932,13.515634],[101.888134,13.514786],[101.888607,13.514849],[101.890729,13.508303],[101.88829,13.506659],[101.887477,13.506287],[101.886434,13.509284],[101.884834,13.512652],[101.884184,13.512509],[101.883769,13.512952],[101.883029,13.513252],[101.882791,13.5143],[101.887646,13.515772]]]},\"properties\":{\"geographicLevel\":5,\"name\":\"นาอิสาน (2408021601)\",\"parentId\":\"6f615652-b304-462f-9dd6-26b778a3a2ee\",\"status\":\"Active\",\"username\":\"thaiuser3\",\"version\":0,\"name_en\":\" (2408021601)\",\"OpenMRS_Id\":\"ac856e35-d636-4f94-b627-dd8194052730\",\"externalId\":\"2408021601\"}}";
    public static String originalMultipolygonFeatureJson = "{\"type\":\"Feature\",\"id\":\"a7baf57d-ad31-46d2-8d28-0c81dd306b09\",\"geometry\":{\"type\":\"MultiPolygon\",\"coordinates\":[[[[100.5244829,13.8576014],[100.5242194,13.8435594],[100.5151606,13.8435594],[100.5123746,13.8519458],[100.517497,13.8608167],[100.5244829,13.8576014]]]]},\"properties\":{\"geographicLevel\":5,\"name\":\"Thailand test site BVBD 2\",\"parentId\":\"45042d61-2305-4b67-87f4-a451339f79c7\",\"status\":\"Active\",\"username\":\"bvbd_test\",\"version\":0,\"name_en\":\"Thailand test site BVBD 2\",\"OpenMRS_Id\":\"eed81741-f168-4723-99d8-16a513445a35\",\"externalId\":\"1201030202\"}}";
    public static String editedPolygonFeatureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[101.887646,13.515772],[101.887932,13.515634],[101.888134,13.514786],[101.888607,13.514849]]]}}";
    public static String editedMultiPolygonFeatureJson = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[100.5244829,13.8576014],[100.5242194,13.8435594],[100.5151606,13.8435594],[100.5123746,13.8519458],[100.5244829,13.8576014]]]}}";
    @Test
    public void testGetInterventionLabel() throws Exception {
        PowerMockito.mockStatic(PreferencesUtil.class);
        PreferencesUtil preferencesUtil = mock(PreferencesUtil.class);
        PowerMockito.when(PreferencesUtil.class, "getInstance").thenReturn(preferencesUtil);
        when(preferencesUtil.getCurrentPlan()).thenReturn("IRS_1");
        when(preferencesUtil.getInterventionTypeForPlan("IRS_1")).thenReturn("IRS");
        assertEquals(getInterventionLabel(), R.string.irs);
    }

    @Test
    public void testgetDrawOperationalAreaBoundaryAndLabelReturnsDefaultValueWhenSettingsValueIsNull() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        when(revealApplication.getServerConfigs()).thenReturn(new HashMap<>());
        assertEquals(getDrawOperationalAreaBoundaryAndLabel(), Constants.CONFIGURATION.DEFAULT_DRAW_OPERATIONAL_AREA_BOUNDARY_AND_LABEL);
    }

    @Test
    public void testgetDrawOperationalAreaBoundaryAndLabelReturnsFalseWhenSettingsValueIsFalse() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Map<String, Object> settings = new HashMap<>();
        settings.put("draw_operational_area_boundary_and_label", "false");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertFalse(getDrawOperationalAreaBoundaryAndLabel());
    }

    @Test
    public void testgetDrawOperationalAreaBoundaryAndLabelReturnsTrueWhenSettingsValueIsTrue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Map<String, Object> settings = new HashMap<>();
        settings.put("draw_operational_area_boundary_and_label", "true");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assert(getDrawOperationalAreaBoundaryAndLabel());
    }

    @Test
    public void testCreateGeoJSONCircle() throws Exception {

        Feature expectedFeature = Feature.fromJson(circleFeatureJSonString);
        assertNotNull(expectedFeature);
        LatLng center = new LatLng(15.0913957, 101.18959799999999);
        Feature actualFeature = createCircleFeature(center, DEFAULT_INDEX_CASE_CIRCLE_RADIUS_IN_METRES, DEFAULT_GEO_JSON_CIRCLE_SIDES );

        assertNotNull(actualFeature);
        assertEquals(expectedFeature.type(), actualFeature.type());
        assertEquals(expectedFeature.type(), actualFeature.type());
        assertNotNull(actualFeature.geometry());
        assertEquals(expectedFeature.geometry().type(), actualFeature.geometry().type());

    }

    @Test
    public void testIsResidentialStructureReturnsCorrectValue() throws Exception {

        assertTrue(isResidentialStructure(REGISTER_FAMILY));
        assertTrue(isResidentialStructure(BEDNET_DISTRIBUTION));
        assertTrue(isResidentialStructure(BLOOD_SCREENING));
        assertFalse(isResidentialStructure(MOSQUITO_COLLECTION));
        assertFalse(isResidentialStructure(LARVAL_DIPPING));
        assertFalse(isResidentialStructure(null));
        assertFalse(isResidentialStructure(""));
    }

    private RevealApplication initRevealApplicationMock() throws Exception {
        PowerMockito.mockStatic(RevealApplication.class);
        RevealApplication revealApplication = mock(RevealApplication.class);
        PowerMockito.when(RevealApplication.class, "getInstance").thenReturn(revealApplication);
        return revealApplication;
    }

    @Test
    public void testValidateFarStructuresReturnsTrueWhenSettingsValueIsTrue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Map<String, Object> settings = new HashMap<>();
        settings.put(VALIDATE_FAR_STRUCTURES, "true");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertTrue(validateFarStructures());
    }

    @Test
    public void testValidateFarStructuresReturnsFalseWhenSettingsValueIsFalse() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Map<String, Object> settings = new HashMap<>();
        settings.put(VALIDATE_FAR_STRUCTURES, "false");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertFalse(validateFarStructures());
    }

    @Test
    public void testGetResolveLocationTimeoutInSecondsReturnsCorrectSettingsValue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Integer testResolveLocationTimeoutInSeconds = 20;
        Map<String, Object> settings = new HashMap<>();
        settings.put(RESOLVE_LOCATION_TIMEOUT_IN_SECONDS, testResolveLocationTimeoutInSeconds.toString());

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertEquals(testResolveLocationTimeoutInSeconds.intValue(), getResolveLocationTimeoutInSeconds());
        assertNotEquals(testResolveLocationTimeoutInSeconds.intValue(), BuildConfig.RESOLVE_LOCATION_TIMEOUT_IN_SECONDS);
    }

    @Test
    public void testGetAdminPasswordNotNearStructuresReturnsCorrectSettingsValue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        String testAdminPass = "NewAdminPass";
        Map<String, Object> settings = new HashMap<>();
        settings.put(ADMIN_PASSWORD_NOT_NEAR_STRUCTURES, testAdminPass);

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertEquals(testAdminPass, getAdminPasswordNotNearStructures());
        assertNotEquals(testAdminPass, BuildConfig.ADMIN_PASSWORD_NOT_NEAR_STRUCTURES);
    }

    @Test
    public void testDisplayAddStructureOutOfBoundaryWarningDialogReturnsTrueWhenSettingsValueIsTrue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();
        Map<String, Object> settings = new HashMap<>();
        settings.put(DISPLAY_ADD_STRUCTURE_OUT_OF_BOUNDARY_WARNING_DIALOG, "true");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertTrue(displayAddStructureOutOfBoundaryWarningDialog());

    }

    @Test
    public void testDisplayAddStructureOutOfBoundaryWarningDialogReturnsFalseWhenSettingsValueIsFalse() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();
        Map<String, Object> settings = new HashMap<>();
        settings.put(DISPLAY_ADD_STRUCTURE_OUT_OF_BOUNDARY_WARNING_DIALOG, "false");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertFalse(displayAddStructureOutOfBoundaryWarningDialog());

    }

    @Test
    public void testDisplayDistanceScaleReturnsTrueWhenSettingsValueIsTrue() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        Map<String, Object> settings = new HashMap<>();
        settings.put(DISPLAY_DISTANCE_SCALE, "true");

        when(revealApplication.getServerConfigs()).thenReturn(settings);
        assertTrue(displayDistanceScale());
    }

    @Test
    public void testGetCoordsFromGeometryReturnsCorrectValuesFromPolygon() {

        Feature originalFeature = Feature.fromJson(originalPolyGonFeatureJson);
        Feature editedFeature = Feature.fromJson(editedPolygonFeatureJson);
        JSONArray actualCoords = getCoordsFromGeometry(editedFeature.geometry(), originalFeature.geometry());

        assertEquals("[[[101.887646,13.515772],[101.887932,13.515634],[101.888134,13.514786],[101.888607,13.514849]]]",actualCoords.toString());
        
    }

    @Test
    public void testBuildRepeatingGroup(){
        JSONObject mockedObject = mock(JSONObject.class);
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

        LinkedHashMap<String, HashMap<String, String>> repeatingGroup = Utils.buildRepeatingGroup(mockedObject, mockedObs);
        assertEquals(1, repeatingGroup.size());
        assertEquals("some value", repeatingGroup.get("name").get("field"));
    }

    @Test
    public void testGenerateListMapOfRepeatingGrp() {
        Map<String, HashMap<String, String>> testData = new HashMap<>();
        testData.put("entry1", new HashMap<>());
        testData.put("entry2", new HashMap<>());
        List<HashMap<String, String>> resultData = Utils.generateListMapOfRepeatingGrp(testData);
        assertEquals(testData.size(), resultData.size());
    }

    @Test
    public void testGetCoordsFromGeometryReturnsCorrectValuesFromMultiPolygon() {

        Feature originalFeature = Feature.fromJson(originalMultipolygonFeatureJson);
        Feature editedFeature = Feature.fromJson(editedMultiPolygonFeatureJson);
        JSONArray actualCoords = getCoordsFromGeometry(editedFeature.geometry(), originalFeature.geometry());

        assertEquals("[[[[100.5244829,13.8576014],[100.5242194,13.8435594],[100.5151606,13.8435594],[100.5123746,13.8519458],[100.5244829,13.8576014]]]]",actualCoords.toString());

    }

    @Test
    public void testGetSyncIntervalDefaultForThailand() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();

        when(revealApplication.getServerConfigs()).thenReturn(new HashMap<>());
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND);
        assertEquals(Utils.getSyncInterval(), 720);

        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.THAILAND_EN);
        assertEquals(Utils.getSyncInterval(), 720);
    }

    @Test
    public void testGetSyncIntervalDefaultForOthers() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();
        when(revealApplication.getServerConfigs()).thenReturn(new HashMap<>());
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        assertEquals(Utils.getSyncInterval(), 480);
    }

    @Test
    public void testGetSyncIntervalFromGlobalConfig() throws Exception {
        RevealApplication revealApplication = initRevealApplicationMock();
        Map<String, Object> globalConfigs= new HashMap<>();
        globalConfigs.put(Constants.CONFIGURATION.SYNC_INTERVAL_IN_MINUTES, "80");
        when(revealApplication.getServerConfigs()).thenReturn(globalConfigs);
        assertEquals(Utils.getSyncInterval(), 80);
    }

    @Test
    public void testGetDefinitionUri() {
        PlanDefinition mockDefinition = mock(PlanDefinition.class);
        Action action = mock(Action.class);
        List<Action> actions = Collections.singletonList(action);
        when(mockDefinition.getActions()).thenReturn(actions);
        when(action.getCode()).thenReturn(BCC);
        when(action.getDefinitionUri()).thenReturn("BCC.json");
        String result = Utils.getDefinitionUri(mockDefinition, BCC);
        assertEquals("BCC", result);
    }

    @Test
    public void testGetDefinitionUriNotFound() {
        PlanDefinition mockDefinition = mock(PlanDefinition.class);
        Action action = mock(Action.class);
        List<Action> actions = Collections.singletonList(action);
        when(mockDefinition.getActions()).thenReturn(actions);
        when(action.getCode()).thenReturn(BCC);
        when(action.getDefinitionUri()).thenReturn("BCC.json");
        String result = Utils.getDefinitionUri(mockDefinition, BEDNET_DISTRIBUTION);
        assertNull(result);
    }

    @Test
    public void testShowWhenTrueCheckVisible() {
        View view = mock(View.class);
        showWhenTrue(view, true);
        verify(view).setVisibility(View.VISIBLE);
    }

    @Test
    public void testShowWhenTrueCheckGone() {
        View view = mock(View.class);
        showWhenTrue(view, false);
        verify(view).setVisibility(View.GONE);
    }

}
