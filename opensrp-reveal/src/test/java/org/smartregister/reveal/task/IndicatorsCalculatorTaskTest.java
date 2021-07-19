package org.smartregister.reveal.task;

import android.view.View;

import junit.framework.TestCase;

import net.sqlcipher.database.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.smartregister.domain.Location;
import org.smartregister.domain.Setting;
import org.smartregister.reporting.view.ProgressIndicatorView;
import org.smartregister.repository.AllSettings;
import org.smartregister.reveal.BaseUnitTest;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.R;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.model.IndicatorDetails;
import org.smartregister.reveal.util.Constants.CONFIGURATION;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.IndicatorUtils;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.TestingUtils;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.view.ListTasksActivity;
import org.smartregister.util.Cache;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.smartregister.reveal.util.IndicatorUtilsTest.getCursor;

/**
 * Created by samuelgithengi on 8/13/20.
 */

public class IndicatorsCalculatorTaskTest extends BaseUnitTest {

    private IndicatorsCalculatorTask indicatorsCalculatorTask;

    private ListTasksActivity activity;

    @Mock
    private AllSettings settingsRepository;

    @Mock
    private Setting setting;

    @Mock
    private SQLiteDatabase sqLiteDatabase;

    @Before
    public void setUp() {
        Robolectric.getForegroundThreadScheduler().pause();
        org.smartregister.Context.bindtypes = new ArrayList<>();
        activity = spy(Robolectric.buildActivity(ListTasksActivity.class).create().visible().get());
        doNothing().when(activity).positionMyLocationAndLayerSwitcher();
        indicatorsCalculatorTask = new IndicatorsCalculatorTask(activity, TestingUtils.createTasks()
                .stream().map(IndicatorUtils::convertToTaskDetails).collect(Collectors.toList()));

        PreferencesUtil.getInstance().setCurrentOperationalArea("mti");
        Location jurisdiction = new Location();
        jurisdiction.setId("2980");
        Cache<Location> cache = mock(Cache.class);
        Mockito.when(cache.get(anyString(), any())).thenReturn(jurisdiction);
        Whitebox.setInternalState(Utils.class,"cache", cache);
    }


    @Test
    public void testOnPreExecuteShouldPopulateViews() {
        indicatorsCalculatorTask.execute();
        assertNotNull(Whitebox.getInternalState(indicatorsCalculatorTask, "progressIndicator"));
        assertNotNull(Whitebox.getInternalState(indicatorsCalculatorTask, "progressIndicator2"));
        assertNotNull(Whitebox.getInternalState(indicatorsCalculatorTask, "progressIndicator3"));
        assertNotNull(Whitebox.getInternalState(indicatorsCalculatorTask, "tableView"));
    }

    @Test
    public void testDoInBackgroundForZambia() {
        Country country = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", Boolean.FALSE);
        IndicatorDetails indicatorDetails = indicatorsCalculatorTask.doInBackground();
        assertEquals(6, indicatorDetails.getTotalStructures());
        assertEquals(2, indicatorDetails.getNotVisited());
        assertEquals(0, indicatorDetails.getFoundStructures());
        assertEquals(3, indicatorDetails.getSprayed());
        assertEquals(1, indicatorDetails.getNotSprayed());
        assertEquals(50, indicatorDetails.getProgress());
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, country);
    }

    @Test
    public void testDoInBackgroundForNamibia() {
        Country country = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        Whitebox.setInternalState(indicatorsCalculatorTask, "sqLiteDatabase", sqLiteDatabase);
        when(sqLiteDatabase.rawQuery(anyString(), any())).thenReturn(getCursor());

        IndicatorDetails indicatorDetails = indicatorsCalculatorTask.doInBackground();
        TestCase.assertEquals(75, indicatorDetails.getFoundStructures());
        TestCase.assertEquals(74, indicatorDetails.getSprayed());
        TestCase.assertEquals(1, indicatorDetails.getNotSprayed());
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, country);
    }

    @Test
    public void testOnPostExecuteZambia() {
        Country country = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.ZAMBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", Boolean.FALSE);
        indicatorsCalculatorTask.onPreExecute();
        indicatorsCalculatorTask.onPostExecute(indicatorsCalculatorTask.doInBackground());
        ProgressIndicatorView progressIndicator = activity.findViewById(R.id.progressIndicatorView);
        ProgressIndicatorView progressIndicator2 = activity.findViewById(R.id.progressIndicatorView2);
        ProgressIndicatorView progressIndicator3 = activity.findViewById(R.id.progressIndicatorView3);

        assertEquals("50%", progressIndicator.getTitle());
        assertEquals(activity.getString(R.string.spray_coverage), progressIndicator.getSubTitle());
        assertEquals(View.VISIBLE, progressIndicator.getVisibility());
        assertEquals("66%", progressIndicator2.getTitle());
        assertEquals(activity.getString(R.string.found_coverage), progressIndicator2.getSubTitle());
        assertEquals("75%", progressIndicator3.getTitle());
        assertEquals(activity.getString(R.string.spray_success_rate), progressIndicator3.getSubTitle());
        verify(activity).positionMyLocationAndLayerSwitcher();

        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, country);
    }

    @Test
    public void testOnPostExecuteNamibiaWithoutTargets() {
        Country country = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", Boolean.FALSE);
        Whitebox.setInternalState(indicatorsCalculatorTask, "sqLiteDatabase", sqLiteDatabase);
        when(sqLiteDatabase.rawQuery(anyString(), any())).thenReturn(getCursor());

        indicatorsCalculatorTask.onPreExecute();
        indicatorsCalculatorTask.onPostExecute(indicatorsCalculatorTask.doInBackground());
        ProgressIndicatorView progressIndicator = activity.findViewById(R.id.progressIndicatorView);
        ProgressIndicatorView progressIndicator2 = activity.findViewById(R.id.progressIndicatorView2);
        ProgressIndicatorView progressIndicator3 = activity.findViewById(R.id.progressIndicatorView3);

        assertEquals(View.GONE, progressIndicator.getVisibility());
        assertEquals("98%", progressIndicator2.getTitle());
        assertEquals(activity.getString(R.string.found_coverage), progressIndicator2.getSubTitle());
        assertEquals("100%", progressIndicator3.getTitle());
        assertEquals(activity.getString(R.string.target_coverage), progressIndicator3.getSubTitle());
        verify(activity).positionMyLocationAndLayerSwitcher();
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, country);

    }

    @Test
    public void testOnPostExecuteNamibiaWithTargets() {
        Country country = BuildConfig.BUILD_COUNTRY;
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, Country.NAMIBIA);
        Whitebox.setInternalState(BuildConfig.class, "SELECT_JURISDICTION", Boolean.FALSE);

        when(settingsRepository.getSetting(CONFIGURATION.JURISDICTION_METADATA)).thenReturn(setting);
        AllSettings contextSettingsRepository = RevealApplication.getInstance().getSettingsRepository();
        Whitebox.setInternalState(RevealApplication.getInstance().getContext(), "allSettings", settingsRepository);
        when(setting.getValue()).thenReturn("{\"settings\":[{\"settingMetadataId\":\"7148\",\"serverVersion\":0,\"description\":\"Jurisdiction Metadata for Nyaluwiro metadata id 2980\",\"label\":\"Nyaluwiro metadata metadata\",\"type\":\"Setting\",\"value\":\"120\",\"uuid\":\"1c2fc15c-732c-4ed1-84be-8d271debd442\",\"key\":\"2980\",\"settingIdentifier\":\"jurisdiction_metadata-risk\"}]}");
        Whitebox.setInternalState(indicatorsCalculatorTask, "sqLiteDatabase", sqLiteDatabase);
        when(sqLiteDatabase.rawQuery(anyString(), any())).thenReturn(getCursor());

        indicatorsCalculatorTask.onPreExecute();
        indicatorsCalculatorTask.onPostExecute(indicatorsCalculatorTask.doInBackground());
        ProgressIndicatorView progressIndicator = activity.findViewById(R.id.progressIndicatorView);
        ProgressIndicatorView progressIndicator2 = activity.findViewById(R.id.progressIndicatorView2);
        ProgressIndicatorView progressIndicator3 = activity.findViewById(R.id.progressIndicatorView3);

        assertEquals(View.GONE, progressIndicator.getVisibility());
        assertEquals("98%", progressIndicator2.getTitle());
        assertEquals(activity.getString(R.string.found_coverage), progressIndicator2.getSubTitle());
        assertEquals("61%", progressIndicator3.getTitle());
        assertEquals(activity.getString(R.string.target_coverage), progressIndicator3.getSubTitle());
        verify(activity).positionMyLocationAndLayerSwitcher();
        verify(settingsRepository).getSetting(CONFIGURATION.JURISDICTION_METADATA);
        Whitebox.setInternalState(BuildConfig.class, BuildConfig.BUILD_COUNTRY, country);
        Whitebox.setInternalState(RevealApplication.getInstance().getContext(), "allSettings", contextSettingsRepository);

    }
}
