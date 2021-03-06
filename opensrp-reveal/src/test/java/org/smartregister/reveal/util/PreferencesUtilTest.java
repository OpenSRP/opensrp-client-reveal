package org.smartregister.reveal.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.reflect.Whitebox;
import org.smartregister.domain.Location;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.util.Cache;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.reveal.util.Constants.Preferences.CURRENT_DISTRICT;
import static org.smartregister.reveal.util.Constants.Preferences.CURRENT_FACILITY;
import static org.smartregister.reveal.util.Constants.Preferences.CURRENT_OPERATIONAL_AREA;
import static org.smartregister.reveal.util.Constants.Preferences.CURRENT_OPERATIONAL_AREA_ID;
import static org.smartregister.reveal.util.Constants.Preferences.CURRENT_PROVINCE;
import static org.smartregister.reveal.util.Constants.Preferences.FACILITY_LEVEL;

/**
 * Created by Richard Kareko on 4/15/20.
 */

public class PreferencesUtilTest extends BaseUnitTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private AllSharedPreferences allSharedPreferences;

    @Mock
    private Cache<Location> cache;

    @Mock
    private Location location;

    private PreferencesUtil preferencesUtil;

    @Before
    public void setUp() {
        preferencesUtil = PreferencesUtil.getInstance();
        Whitebox.setInternalState(preferencesUtil, "allSharedPreferences", allSharedPreferences);
    }

    @After
    public void tearDown() {
        Whitebox.setInternalState(preferencesUtil, "allSharedPreferences", RevealApplication.getInstance().getContext().allSharedPreferences());
    }

    @Test
    public void testSetCurrentFacility() {
        preferencesUtil.setCurrentFacility("Akros_1");
        verify(allSharedPreferences).savePreference(CURRENT_FACILITY, "Akros_1");
    }

    @Test
    public void testGetCurrentFacility() {
        when(allSharedPreferences.getPreference(CURRENT_FACILITY)).thenReturn("Akros_1");

        String actualCurrentFacility = preferencesUtil.getCurrentFacility();
        assertEquals("Akros_1", actualCurrentFacility);
    }

    @Test
    public void testSetCurrentDistrict() {
        preferencesUtil.setCurrentDistrict("Chadiza");
        verify(allSharedPreferences).savePreference(CURRENT_DISTRICT, "Chadiza");
    }

    @Test
    public void testGetCurrentDistrict() {
        when(allSharedPreferences.getPreference(CURRENT_DISTRICT)).thenReturn("Chadiza");

        String actualCurrentDistrict = preferencesUtil.getCurrentDistrict();
        assertEquals("Chadiza", actualCurrentDistrict);
    }

    @Test
    public void testSetCurrentProvince() {
        preferencesUtil.setCurrentProvince("Lusaka");
        verify(allSharedPreferences).savePreference(CURRENT_PROVINCE, "Lusaka");
    }

    @Test
    public void testGetCurrentProvince() {
        when(allSharedPreferences.getPreference(CURRENT_PROVINCE)).thenReturn("Lusaka");

        String actualCurrentProvince = preferencesUtil.getCurrentProvince();
        assertEquals("Lusaka", actualCurrentProvince);
    }

    @Test
    public void testGetPreferenceValue() {
        when(allSharedPreferences.getPreference(CURRENT_PROVINCE)).thenReturn("Lusaka");

        String actualPreferenceValue = preferencesUtil.getPreferenceValue(CURRENT_PROVINCE);
        assertEquals("Lusaka", actualPreferenceValue);
    }

    @Test
    public void testSetCurrentFacilityLevel() {
        preferencesUtil.setCurrentFacilityLevel("Village");
        verify(allSharedPreferences).savePreference(FACILITY_LEVEL, "Village");
    }

    @Test
    public void testGetCurrentFacilityLevel() {
        when(allSharedPreferences.getPreference(FACILITY_LEVEL)).thenReturn("Village");

        String actualFacilityLevel = preferencesUtil.getCurrentFacilityLevel();
        assertEquals("Village", actualFacilityLevel);
    }


    @Test
    public void testSetCurrentOperationalAreaShouldUpdatePreferences() {
        String operationalArea = "oa_1";
        Whitebox.setInternalState(Utils.class, "cache", cache);
        when(cache.get(eq(operationalArea), any())).thenReturn(location);
        when(location.getId()).thenReturn("id_11121121");
        when(preferencesUtil.getCurrentOperationalArea()).thenReturn(operationalArea);

        preferencesUtil.setCurrentOperationalArea(operationalArea);
        verify(allSharedPreferences).savePreference(CURRENT_OPERATIONAL_AREA, operationalArea);
        verify(allSharedPreferences).savePreference(CURRENT_OPERATIONAL_AREA_ID, "id_11121121");
    }

    @Test
    public void testSetCurrentOperationalAreaShouldClearPreferences() {
        preferencesUtil.setCurrentOperationalArea(null);
        verify(allSharedPreferences).savePreference(CURRENT_OPERATIONAL_AREA, null);
        verify(allSharedPreferences).savePreference(CURRENT_OPERATIONAL_AREA_ID, null);
    }

    @Test
    public void testSetActionCodesForPlan() {
        String planId = "plan-id1";
        preferencesUtil.setActionCodesForPlan(planId, Collections.singletonList(Constants.Intervention.BLOOD_SCREENING));
        verify(allSharedPreferences).savePreference("plan-id1~actions", "[\"Blood Screening\"]");
    }

    @Test
    public void testGetActionCodesForPlan() {
        String planId = "plan-id1";
        String actionCodeJsonString = "[\"Blood Screening\"]";
        when(allSharedPreferences.getPreference("plan-id1~actions")).thenReturn(actionCodeJsonString);
        List<String> actualActionCodes =  preferencesUtil.getActionCodesForPlan(planId);
        verify(allSharedPreferences).getPreference("plan-id1~actions");
        assertEquals(1, actualActionCodes.size());
        assertEquals(Constants.Intervention.BLOOD_SCREENING, actualActionCodes.get(0));
    }

}
