package org.smartregister.reveal.application;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.evernote.android.job.JobManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.mapboxsdk.Mapbox;
import com.vijay.jsonwizard.NativeFormLibrary;
import com.vijay.jsonwizard.activities.JsonWizardFormActivity;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.P2POptions;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.configurableviews.helper.JsonSpecHelper;
import org.smartregister.domain.Setting;
import org.smartregister.dto.UserAssignmentDTO;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.activity.FamilyWizardFormActivity;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.family.util.DBConstants;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.receiver.ValidateAssignmentReceiver;
import org.smartregister.repository.AllSettings;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.LocationRepository;
import org.smartregister.repository.LocationTagRepository;
import org.smartregister.repository.PlanDefinitionRepository;
import org.smartregister.repository.PlanDefinitionSearchRepository;
import org.smartregister.repository.Repository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.activity.LoginActivity;
import org.smartregister.reveal.job.RevealJobCreator;
import org.smartregister.reveal.repository.RevealRepository;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.AppExecutors;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.DatabaseKeys;
import org.smartregister.reveal.util.Constants.EventsRegister;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.PreferencesUtil;
import org.smartregister.reveal.util.RevealSyncConfiguration;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.view.FamilyProfileActivity;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.DrishtiSyncScheduler;
import org.smartregister.util.LangUtils;
import org.smartregister.view.activity.DrishtiApplication;
import org.smartregister.view.receiver.TimeChangedBroadcastReceiver;

import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;
import io.ona.kujaku.KujakuLibrary;
import io.ona.kujaku.data.realm.RealmDatabase;
import timber.log.Timber;

import static org.smartregister.reveal.util.Constants.CONFIGURATION.GLOBAL_CONFIGS;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.KEY;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.SETTINGS;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.TEAM_CONFIGS;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.VALUE;
import static org.smartregister.reveal.util.Constants.CONFIGURATION.VALUES;
import static org.smartregister.reveal.util.FamilyConstants.CONFIGURATION;
import static org.smartregister.reveal.util.FamilyConstants.EventType;
import static org.smartregister.reveal.util.FamilyConstants.JSON_FORM;
import static org.smartregister.reveal.util.FamilyConstants.RELATIONSHIP;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME;

public class RevealApplication extends DrishtiApplication implements TimeChangedBroadcastReceiver.OnTimeChangedListener, ValidateAssignmentReceiver.UserAssignmentListener {

    private JsonSpecHelper jsonSpecHelper;
    private char[] password;

    private PlanDefinitionSearchRepository planDefinitionSearchRepository;

    private Map<String, Object> serverConfigs;

    private static CommonFtsObject commonFtsObject;

    private AppExecutors appExecutors;

    private boolean refreshMapOnEventSaved;

    private boolean myLocationComponentEnabled;

    private FeatureCollection featureCollection;

    private FamilyMetadata metadata;

    private RealmDatabase realmDatabase;

    private Feature operationalArea;

    private boolean synced;

    public static synchronized RevealApplication getInstance() {
        return (RevealApplication) mInstance;
    }

    public static JsonSpecHelper getJsonSpecHelper() {
        return getInstance().jsonSpecHelper;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        context = Context.getInstance();
        context.updateApplicationContext(getApplicationContext());
        context.updateCommonFtsObject(createCommonFtsObject());
        // Initialize Modules
        Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());
        P2POptions p2POptions = new P2POptions(true);
        CoreLibrary.init(context, new RevealSyncConfiguration(), BuildConfig.BUILD_TIMESTAMP, p2POptions);
        forceRemoteLoginForInConsistentUsername();
        if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.NAMIBIA_EC_CLIENT_FIELDS);
        } else if (BuildConfig.BUILD_COUNTRY == Country.BOTSWANA) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.BOTSWANA_EC_CLIENT_FIELDS);
        } else if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.ZAMBIA_EC_CLIENT_FIELDS);
        } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.REFAPP_EC_CLIENT_FIELDS);
        } else if (BuildConfig.BUILD_COUNTRY == Country.NTD_SCHOOL || BuildConfig.BUILD_COUNTRY == Country.NTD_COMMUNITY) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.NTD_EC_CLIENT_FIELDS);
        } else if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL) {
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.SENEGAL_EC_CLIENT_FIELDS);
        } else if(BuildConfig.BUILD_COUNTRY == Country.KENYA){
            CoreLibrary.getInstance().setEcClientFieldsFile(Constants.ECClientConfig.KENYA_EC_CLIENT_FIELDS);
        }
        ConfigurableViewsLibrary.init(context);
        FamilyLibrary.init(context, getMetadata(), BuildConfig.VERSION_CODE, BuildConfig.DATABASE_VERSION);

        LocationHelper.init(Utils.ALLOWED_LEVELS, Utils.DEFAULT_LOCATION_LEVEL);

        SyncStatusBroadcastReceiver.init(this);

        jsonSpecHelper = new JsonSpecHelper(this);
        serverConfigs = new HashMap<>();

        Mapbox.getInstance(getApplicationContext(), BuildConfig.MAPBOX_SDK_ACCESS_TOKEN);
        KujakuLibrary.init(getApplicationContext());

        //init Job Manager
        JobManager.create(this).addJobCreator(new RevealJobCreator());

        if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
            LangUtils.saveLanguage(getApplicationContext(), "th");
        } else {
            LangUtils.saveLanguage(getApplicationContext(), "en");
        }
        NativeFormLibrary.getInstance().setClientFormDao(CoreLibrary.getInstance().context().getClientFormRepository());

        ValidateAssignmentReceiver.init(this);
        ValidateAssignmentReceiver.getInstance().addListener(this);

    }

    /**
     * Removes the username and forces a remote login in case the username did not match the openmrs username
     */
    private void forceRemoteLoginForInConsistentUsername() {
        AllSharedPreferences allSharedPreferences = context.allSharedPreferences();
        String provider = allSharedPreferences.fetchRegisteredANM();
        if (StringUtils.isNotBlank(provider) && StringUtils.isBlank(allSharedPreferences.fetchDefaultTeamId(allSharedPreferences.fetchRegisteredANM()))) {
            allSharedPreferences.updateANMUserName(null);
            allSharedPreferences.saveForceRemoteLogin(true, provider);
        }
    }

    @Override
    public Repository getRepository() {
        try {
            if (repository == null) {
                repository = new RevealRepository(getInstance().getApplicationContext(), context);
            }
        } catch (UnsatisfiedLinkError e) {
            Timber.e(e, "Error on getRepository: ");

        }
        return repository;
    }

    @Override
    public void logoutCurrentUser() {
        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        getApplicationContext().startActivity(intent);
        context.userService().logoutSession();
    }

    public Context getContext() {
        return context;
    }

    protected void cleanUpSyncState() {
        try {
            DrishtiSyncScheduler.stop(getApplicationContext());
            context.allSharedPreferences().saveIsSyncInProgress(false);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void onTerminate() {
        Timber.e("Application is terminating. Stopping Sync scheduler and resetting isSyncInProgress setting.");
        cleanUpSyncState();
        TimeChangedBroadcastReceiver.destroy(this);
        SyncStatusBroadcastReceiver.destroy(this);
        ValidateAssignmentReceiver.destroy(this);
        super.onTerminate();
    }

    @Override
    public void onTimeChanged() {
        context.userService().forceRemoteLogin(context.allSharedPreferences().fetchRegisteredANM());
        logoutCurrentUser();
    }

    @Override
    public void onTimeZoneChanged() {
        context.userService().forceRemoteLogin(context.allSharedPreferences().fetchRegisteredANM());
        logoutCurrentUser();
    }

    public TaskRepository getTaskRepository() {
        return CoreLibrary.getInstance().context().getTaskRepository();
    }

    public StructureRepository getStructureRepository() {
        return CoreLibrary.getInstance().context().getStructureRepository();
    }

    public LocationRepository getLocationRepository() {
        return CoreLibrary.getInstance().context().getLocationRepository();
    }

    public LocationTagRepository getLocationTagRepository() {
        return CoreLibrary.getInstance().context().getLocationTagRepository();
    }

    public AllSettings getSettingsRepository() {
        return getInstance().getContext().allSettings();
    }

    public void processServerConfigs() {
        populateConfigs(getSettingsRepository().getSetting(GLOBAL_CONFIGS));
        populateConfigs(getSettingsRepository().getSetting(TEAM_CONFIGS));
    }

    private void populateConfigs(@NonNull Setting setting) {
        if (setting == null) {
            return;
        }
        try {
            JSONArray settingsArray = new JSONObject(setting.getValue()).getJSONArray(SETTINGS);
            for (int i = 0; i < settingsArray.length(); i++) {
                JSONObject jsonObject = settingsArray.getJSONObject(i);
                String value = jsonObject.optString(VALUE, null);
                String key = jsonObject.optString(KEY, null);
                JSONArray values = jsonObject.optJSONArray(VALUES);
                if (value != null && key != null) {
                    serverConfigs.put(key, value);
                } else if (values != null && key != null) {
                    serverConfigs.put(key, values);
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    public Map<String, Object> getServerConfigs() {
        return serverConfigs;
    }

    public FamilyMetadata getMetadata() {

        if (metadata != null) {
            return metadata;
        }

        metadata = new FamilyMetadata(FamilyWizardFormActivity.class, JsonWizardFormActivity.class, FamilyProfileActivity.class, CONFIGURATION.UNIQUE_ID_KEY, true);

        if (BuildConfig.BUILD_COUNTRY == Country.THAILAND) {
            metadata.updateFamilyRegister(JSON_FORM.THAILAND_FAMILY_REGISTER, TABLE_NAME.FAMILY, EventType.FAMILY_REGISTRATION, EventType.UPDATE_FAMILY_REGISTRATION, CONFIGURATION.FAMILY_REGISTER, RELATIONSHIP.FAMILY_HEAD, RELATIONSHIP.PRIMARY_CAREGIVER);
            metadata.updateFamilyMemberRegister(JSON_FORM.THAILAND_FAMILY_MEMBER_REGISTER, TABLE_NAME.FAMILY_MEMBER, EventType.FAMILY_MEMBER_REGISTRATION, EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, CONFIGURATION.FAMILY_MEMBER_REGISTER, RELATIONSHIP.FAMILY);
        } else if (BuildConfig.BUILD_COUNTRY == Country.ZAMBIA) {
            metadata.updateFamilyRegister(JSON_FORM.ZAMBIA_FAMILY_REGISTER, TABLE_NAME.FAMILY, EventType.FAMILY_REGISTRATION, EventType.UPDATE_FAMILY_REGISTRATION, CONFIGURATION.FAMILY_REGISTER, RELATIONSHIP.FAMILY_HEAD, RELATIONSHIP.PRIMARY_CAREGIVER);
            metadata.updateFamilyMemberRegister(JSON_FORM.ZAMBIA_FAMILY_MEMBER_REGISTER, TABLE_NAME.FAMILY_MEMBER, EventType.FAMILY_MEMBER_REGISTRATION, EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, CONFIGURATION.FAMILY_MEMBER_REGISTER, RELATIONSHIP.FAMILY);
        } else if (BuildConfig.BUILD_COUNTRY == Country.REFAPP) {
            metadata.updateFamilyRegister(JSON_FORM.REFAPP_FAMILY_REGISTER, TABLE_NAME.FAMILY, EventType.FAMILY_REGISTRATION, EventType.UPDATE_FAMILY_REGISTRATION, CONFIGURATION.FAMILY_REGISTER, RELATIONSHIP.FAMILY_HEAD, RELATIONSHIP.PRIMARY_CAREGIVER);
            metadata.updateFamilyMemberRegister(JSON_FORM.REFAPP_FAMILY_MEMBER_REGISTER, TABLE_NAME.FAMILY_MEMBER, EventType.FAMILY_MEMBER_REGISTRATION, EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, CONFIGURATION.FAMILY_MEMBER_REGISTER, RELATIONSHIP.FAMILY);
        } else {
            metadata.updateFamilyRegister(JSON_FORM.FAMILY_REGISTER, TABLE_NAME.FAMILY, EventType.FAMILY_REGISTRATION, EventType.UPDATE_FAMILY_REGISTRATION, CONFIGURATION.FAMILY_REGISTER, RELATIONSHIP.FAMILY_HEAD, RELATIONSHIP.PRIMARY_CAREGIVER);
            metadata.updateFamilyMemberRegister(JSON_FORM.FAMILY_MEMBER_REGISTER, TABLE_NAME.FAMILY_MEMBER, EventType.FAMILY_MEMBER_REGISTRATION, EventType.UPDATE_FAMILY_MEMBER_REGISTRATION, CONFIGURATION.FAMILY_MEMBER_REGISTER, RELATIONSHIP.FAMILY);
        }
        metadata.updateFamilyDueRegister(TABLE_NAME.FAMILY_MEMBER, 20, true);
        metadata.updateFamilyActivityRegister(TABLE_NAME.FAMILY_MEMBER, Integer.MAX_VALUE, false);
        metadata.updateFamilyOtherMemberRegister(TABLE_NAME.FAMILY_MEMBER, Integer.MAX_VALUE, false);
        return metadata;
    }

    public static CommonFtsObject createCommonFtsObject() {
        if (commonFtsObject == null) {
            commonFtsObject = new CommonFtsObject(getFtsTables());
            for (String ftsTable : commonFtsObject.getTables()) {
                commonFtsObject.updateSearchFields(ftsTable, getFtsSearchFields(ftsTable));
                commonFtsObject.updateSortFields(ftsTable, getFtsSortFields(ftsTable));
            }
        }
        return commonFtsObject;
    }

    private static String[] getFtsTables() {
        return new String[]{TABLE_NAME.FAMILY, TABLE_NAME.FAMILY_MEMBER, EventsRegister.TABLE_NAME};
    }

    private static String[] getFtsSearchFields(String tableName) {
        if (tableName.equals(TABLE_NAME.FAMILY)) {
            return new String[]{DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.VILLAGE_TOWN, DBConstants.KEY.FIRST_NAME,
                    DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID};
        } else if (tableName.equals(TABLE_NAME.FAMILY_MEMBER)) {
            return new String[]{DBConstants.KEY.BASE_ENTITY_ID, DBConstants.KEY.FIRST_NAME, DBConstants.KEY.MIDDLE_NAME,
                    DBConstants.KEY.LAST_NAME, DBConstants.KEY.UNIQUE_ID};
        } else if (tableName.equals(EventsRegister.TABLE_NAME)) {
            return new String[]{DatabaseKeys.EVENT_DATE, DatabaseKeys.EVENT_TYPE, DatabaseKeys.SOP,
                    DatabaseKeys.ENTITY, DatabaseKeys.STATUS};
        }
        return null;
    }

    private static String[] getFtsSortFields(String tableName) {
        if (tableName.equals(TABLE_NAME.FAMILY)) {
            return new String[]{DBConstants.KEY.LAST_INTERACTED_WITH, DBConstants.KEY.DATE_REMOVED};
        } else if (tableName.equals(TABLE_NAME.FAMILY_MEMBER)) {
            return new String[]{DBConstants.KEY.DOB, DBConstants.KEY.DOD, DBConstants.KEY
                    .LAST_INTERACTED_WITH, DBConstants.KEY.DATE_REMOVED};
        } else if (tableName.equals(EventsRegister.TABLE_NAME)) {
            return new String[]{DatabaseKeys.PROVIDER_ID, DatabaseKeys.EVENT_DATE,
                    DatabaseKeys.EVENT_TYPE, DatabaseKeys.STATUS, DatabaseKeys.SOP};
        }
        return null;
    }

    public AppExecutors getAppExecutors() {
        if (appExecutors == null) {
            appExecutors = new AppExecutors();
        }
        return appExecutors;
    }

    public PlanDefinitionRepository getPlanDefinitionRepository() {
        return CoreLibrary.getInstance().context().getPlanDefinitionRepository();
    }

    public PlanDefinitionSearchRepository getPlanDefinitionSearchRepository() {
        if (planDefinitionSearchRepository == null) {
            planDefinitionSearchRepository = new PlanDefinitionSearchRepository();
        }
        return planDefinitionSearchRepository;
    }

    public RealmDatabase getRealmDatabase(android.content.Context context) {
        if (realmDatabase == null) {
            realmDatabase = RealmDatabase.init(context);
        }
        return realmDatabase;
    }


    @NonNull
    @Override
    public ClientProcessorForJava getClientProcessor() {
        return RevealClientProcessor.getInstance(this);
    }

    public boolean isRefreshMapOnEventSaved() {
        return refreshMapOnEventSaved;
    }

    public void setRefreshMapOnEventSaved(boolean refreshMapOnEventSaved) {
        this.refreshMapOnEventSaved = refreshMapOnEventSaved;
    }

    public boolean isMyLocationComponentEnabled() {
        return myLocationComponentEnabled;
    }

    public void setMyLocationComponentEnabled(boolean myLocationComponentEnabled) {
        this.myLocationComponentEnabled = myLocationComponentEnabled;
    }

    public FeatureCollection getFeatureCollection() {
        return featureCollection;
    }

    public void setFeatureCollection(FeatureCollection featureCollection) {
        this.featureCollection = featureCollection;
    }

    public Feature getOperationalArea() {
        return operationalArea;
    }

    public void setOperationalArea(Feature operationalArea) {
        this.operationalArea = operationalArea;
    }

    public boolean getSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    @Override
    public void onUserAssignmentRevoked(UserAssignmentDTO userAssignmentDTO) {
        PreferencesUtil preferencesUtil = PreferencesUtil.getInstance();
        if (userAssignmentDTO.getJurisdictions().contains(preferencesUtil.getCurrentOperationalAreaId())) {
            preferencesUtil.setCurrentOperationalArea(null);
        }
        if (userAssignmentDTO.getPlans().contains(preferencesUtil.getCurrentPlanId())) {
            preferencesUtil.setCurrentPlan(null);
            preferencesUtil.setCurrentPlanId(null);
        }
        getContext().anmLocationController().evict();
    }
}
