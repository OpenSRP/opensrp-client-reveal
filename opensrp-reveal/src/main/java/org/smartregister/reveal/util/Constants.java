package org.smartregister.reveal.util;

import java.util.Arrays;
import java.util.List;

public interface Constants {

    String VIEW_CONFIGURATION_PREFIX = "ViewConfiguration_";

    String FILTER_TEAM_ID = "teamId";

    String JSON_FORM_PARAM_JSON = "json";

    String METADATA = "metadata";

    String DETAILS = "details";

    String ENTITY_ID = "entity_id";

    String SPRAY_EVENT = "Spray";

    String REGISTER_STRUCTURE_EVENT = "Register_Structure";

    String MOSQUITO_COLLECTION_EVENT = "mosquito_collection";

    String LARVAL_DIPPING_EVENT = "larval_dipping";

    String BEDNET_DISTRIBUTION_EVENT = "bednet_distribution";

    String BLOOD_SCREENING_EVENT = "blood_screening";

    String BEHAVIOUR_CHANGE_COMMUNICATION = "behaviour_change_communication";

    String TASK_RESET_EVENT = "reset_task";

    String STRUCTURE = "Structure";

    String FOCUS = "FOCUS";

    double MY_LOCATION_ZOOM_LEVEL = 17.5; // modifying this will also necessitate modifying the VERTICAL_OFFSET

    double VERTICAL_OFFSET = 0.0;

    double REFRESH_MAP_MINIMUM_DISTANCE = 5;

    int ANIMATE_TO_LOCATION_DURATION = 1000;

    String DIGITAL_GLOBE_CONNECT_ID = "DG_CONNECT_ID";

    String HYPHEN = "-";

    String COMMA = ",";

    String TILDE = "~";

    String UNDERSCRORE = "_";

    int STORAGE_PERMISSIONS = 1;

    String NULL_KEY = "NULL";

    int IRS_VERIFICATION_PERIOD = 30;

    String DBNAME = "drishti.db";
    String COPYDBNAME = "reveal";

    String DG_ID_PLACEHOLDER = "DIGITAL_GLOBE_ID";

    List<String> MACEPA_PROVINCES = Arrays.asList("Western", "Southern", "Lusaka");

    String ACTIONS = "actions";
    String THAILAND_SYNC_INTERVAL = "720";

    int SYNC_BACK_OFF_DELAY = 5000;

    interface CONFIGURATION {
        String LOGIN = "login";
        String GLOBAL_CONFIGS = "global_configs";
        String TEAM_CONFIGS = "team_configs";
        String JURISDICTION_METADATA = "jurisdiction_metadata-target";
        String KEY = "key";
        String VALUE = "value";
        String VALUES = "values";
        String SETTINGS = "settings";
        String LOCATION_BUFFER_RADIUS_IN_METRES = "location_buffer_radius_in_metres";
        String UPDATE_LOCATION_BUFFER_RADIUS = "update_location_buffer_radius";
        String INDEX_CASE_CIRCLE_RADIUS_IN_METRES = "index_case_circle_radius_in_metres";
        Float DEFAULT_INDEX_CASE_CIRCLE_RADIUS_IN_METRES = 1000f;
        String DRAW_OPERATIONAL_AREA_BOUNDARY_AND_LABEL = "draw_operational_area_boundary_and_label";
        Boolean DEFAULT_DRAW_OPERATIONAL_AREA_BOUNDARY_AND_LABEL = true;
        String LOCAL_SYNC_DONE = "local_sync_done";
        Float DEFAULT_GEO_JSON_CIRCLE_SIDES = 120F;
        Float METERS_PER_KILOMETER = 1000f;
        Float KILOMETERS_PER_DEGREE_OF_LONGITUDE_AT_EQUITOR = 111.320f;
        Float KILOMETERS_PER_DEGREE_OF_LATITUDE_AT_EQUITOR = 110.574f;
        String VALIDATE_FAR_STRUCTURES = "validate_far_structures";
        String RESOLVE_LOCATION_TIMEOUT_IN_SECONDS = "resolve_location_timeout_in_seconds";
        String ADMIN_PASSWORD_NOT_NEAR_STRUCTURES = "admin_password_not_near_structures";
        String DISPLAY_ADD_STRUCTURE_OUT_OF_BOUNDARY_WARNING_DIALOG = "display_add_structure_out_of_boundary_warning_dialog";
        Boolean DEFAULT_DISPLAY_ADD_STRUCTURE_OUT_OF_BOUNDARY_WARNING_DIALOG = false;
        Float OUTSIDE_OPERATIONAL_AREA_MASK_OPACITY = 0.65f;
        String SPRAY_OPERATORS = "spray_operators";
        String DATA_COLLECTORS = "data_collectors";
        String SUPERVISORS = "supervisors";
        String TEAM_LEADERS = "team_leaders";
        String FIELD_OFFICERS = "field_officers";
        String DISTRICT_MANAGERS = "district_managers";
        String HEALTH_FACILITIES = "health_facilities";
        String COMMUNITY_HEALTH_WORKERS = "community_health_workers";
        String CODE = "code";
        String NAME = "name";
        String SYNC_INTERVAL_IN_MINUTES = "sync_interval_in_minutes";


        String MDA_CORDINATORS = "mda_coordinators";
        String MDA_ENUMERATORS = "mda_enumerators";
        String MDA_COMMUNITY_HEALTH_WORKERS = "mda_community_health_workers";
        String MDA_ADHERENCE_OFFICERS = "mda_adherence_officers";
        String MDA_CATCHMENT_AREAS = "mda_catchment_areas";
        String DISPLAY_DISTANCE_SCALE = "display_distance_scale";
        String DISTRICTS = "districts";
        String WARDS = "wards";
        String COMMUNITY_DRUG_DISTRIBUTORS = "community_drug_distributors";
        String HEALTH_WORKER_SUPERVISORS = "health_worker_supervisors";
        String VILLAGES = "villages";


    }

    interface Preferences {
        String CURRENT_FACILITY = "CURRENT_FACILITY";
        String CURRENT_DISTRICT = "CURRENT_DISTRICT";
        String CURRENT_PROVINCE = "CURRENT_PROVINCE";
        String CURRENT_PLAN = "CURRENT_PLAN";
        String CURRENT_PLAN_ID = "CURRENT_PLAN_ID";
        String FACILITY_LEVEL = "FACILITY_LEVEL";
        String CURRENT_OPERATIONAL_AREA = "CURRENT_OPERATIONAL_AREA";
        String CURRENT_OPERATIONAL_AREA_ID = "CURRENT_OPERATIONAL_AREA_ID";
    }

    interface Tags {
        String COUNTRY = "Country";
        String PROVINCE = "Province";
        String REGION = "Region";
        String DISTRICT = "District";
        String SUB_DISTRICT = "Sub-district";
        String HEALTH_CENTER = "Rural Health Centre";
        String CANTON = "Canton";
        String VILLAGE = "Village";
        String OPERATIONAL_AREA = "Operational Area";
        String ZONE = "Zones";
    }

    interface Properties {
        String TASK_IDENTIFIER = "taskIdentifier";
        String TASK_BUSINESS_STATUS = "taskBusinessStatus";
        String TASK_STATUS = "taskStatus";
        String TASK_CODE = "taskCode";
        String LOCATION_UUID = "locationUUID";
        String LOCATION_VERSION = "locationVersion";
        String LOCATION_TYPE = "locationType";
        String LOCATION_PARENT = "locationParent";
        String LOCATION_ID = "location_id";
        String FEATURE_SELECT_TASK_BUSINESS_STATUS = "featureSelectTaskBusinessStatus";
        String BASE_ENTITY_ID = "baseEntityId";
        String STRUCTURE_NAME = "structure_name";
        String APP_VERSION_NAME = "appVersionName";
        String FORM_VERSION = "form_version";
        String TASK_CODE_LIST = "task_code_list";
        String FAMILY_MEMBER_NAMES = "family_member_names";
        String PLAN_IDENTIFIER = "planIdentifier";
        String LOCATION_STATUS = "status";
        String LOCATION_NAME = "name";
    }


    interface GeoJSON {
        String TYPE = "type";
        String FEATURE_COLLECTION = "FeatureCollection";
        String FEATURES = "features";
        String IS_INDEX_CASE = "is_index_case";
    }

    interface Intervention {
        String IRS = "IRS";

        String DYNAMIC_IRS = "Dynamic-IRS";

        String MOSQUITO_COLLECTION = "Mosquito Collection";

        String LARVAL_DIPPING = "Larval Dipping";

        String BCC = "BCC";

        String BEDNET_DISTRIBUTION = "Bednet Distribution";

        String BLOOD_SCREENING = "Blood Screening";

        String CASE_CONFIRMATION = "Case Confirmation";

        String REGISTER_FAMILY = "RACD Register Family";

        String FI = "FI";

        String DYNAMIC_FI = "Dynamic-FI";

        String PAOT = "PAOT";

        String MDA_DISPENSE = "MDA Dispense";

        String MDA_ADHERENCE = "MDA Adherence";

        String MDA = "MDA";

        String DYNAMIC_MDA = "Dynamic-MDA";

        String IRS_VERIFICATION = "IRS Verification";

        List<String> PERSON_INTERVENTIONS = Arrays.asList(BLOOD_SCREENING, CASE_CONFIRMATION, MDA_DISPENSE, MDA_ADHERENCE);

        List<String> IRS_INTERVENTIONS = Arrays.asList(IRS, IRS_VERIFICATION);

        List<String> FI_INTERVENTIONS = Arrays.asList(MOSQUITO_COLLECTION,
                LARVAL_DIPPING, BCC, BEDNET_DISTRIBUTION, BLOOD_SCREENING, CASE_CONFIRMATION,
                REGISTER_FAMILY, PAOT);

        List<String> MDA_INTERVENTIONS = Arrays.asList(REGISTER_FAMILY, MDA_ADHERENCE, MDA_DISPENSE);

        List<String> TASK_RESET_INTERVENTIONS = Arrays.asList(MOSQUITO_COLLECTION,
                LARVAL_DIPPING, BCC, CASE_CONFIRMATION,
                PAOT, IRS, IRS_VERIFICATION);

        String CDD_SUPERVISION = "CDD Supervision";
    }


    interface EventType {

        String CASE_CONFIRMATION_EVENT = "case_confirmation";

        String CASE_DETAILS_EVENT = "Case_Details";

        String PAOT_EVENT = "PAOT";

        String MDA_DISPENSE = "mda_dispense";

        String MDA_ADHERENCE = "mda_adherence";

        String IRS_VERIFICATION = "irs_verification";

        String IRS_LITE_VERIFICATION = "irs_lite_verification";

        String DAILY_SUMMARY_EVENT = "daily_summary";

        String IRS_FIELD_OFFICER_EVENT = "irs_field_officer";

        String IRS_SA_DECISION_EVENT = "irs_sa_decision";

        String MOBILIZATION_EVENT = "mobilization";

        String TEAM_LEADER_DOS_EVENT = "team_leader_dos";

        String VERIFICATION_EVENT = "verification";

        String CB_SPRAY_AREA_EVENT = "cb_spray_area";

        String CDD_SUPERVISOR_DAILY_SUMMARY =  "cdd_supervisor_daily_summary";

        String TABLET_ACCOUNTABILITY_EVENT =  "tablet_accountability";

        List<String> SUMMARY_EVENT_TYPES = Arrays.asList(DAILY_SUMMARY_EVENT, IRS_FIELD_OFFICER_EVENT,
                IRS_SA_DECISION_EVENT, MOBILIZATION_EVENT, TEAM_LEADER_DOS_EVENT, VERIFICATION_EVENT,TABLET_ACCOUNTABILITY_EVENT);
    }

    interface Tables {
        String MOSQUITO_COLLECTIONS_TABLE = "mosquito_collections";
        String LARVAL_DIPPINGS_TABLE = "larval_dippings";
        String PAOT_TABLE = "potential_area_of_transmission";
        String IRS_VERIFICATION_TABLE = "irs_verification";
        String CLIENT_TABLE = "client";
        String EVENT_TABLE = "event";
        String TASK_TABLE = "task";
        String STRUCTURE_TABLE = "structure";
        String EC_EVENTS_TABLE = "ec_events";
        String EC_EVENTS_SEARCH_TABLE = "ec_events_search";
        String SPRAYED_STRUCTURES = "sprayed_structures";
    }

    interface BusinessStatus {
        String NOT_VISITED = "Not Visited";
        String NOT_SPRAYED = "Not Sprayed";
        String SPRAYED = "Sprayed";
        String NOT_SPRAYABLE = "Not Sprayable";
        String COMPLETE = "Complete";
        String INCOMPLETE = "Incomplete";
        String NOT_ELIGIBLE = "Not Eligible";
        String IN_PROGRESS = "In Progress";


        //MDA status
        String FULLY_RECEIVED = "Fully Received";
        String NONE_RECEIVED = "None Received";
        String ADHERENCE_VISIT_DONE = "Adherence Visit Done";
        String PARTIALLY_RECEIVED = "Partially Received";

        // Following are for grouped structure tasks. Not synced to server
        String FAMILY_REGISTERED = "Family Registered";
        String BEDNET_DISTRIBUTED = "Bednet Distributed";
        String BLOOD_SCREENING_COMPLETE = "Blood Screening Complete";
        String PARTIALLY_SPRAYED = "Partially Sprayed";


        List<String> IRS_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, NOT_SPRAYED,
                SPRAYED, NOT_SPRAYABLE, COMPLETE, INCOMPLETE, NOT_ELIGIBLE, IN_PROGRESS);

        List<String> FI_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, FAMILY_REGISTERED, BEDNET_DISTRIBUTED,
                BLOOD_SCREENING_COMPLETE, COMPLETE, NOT_ELIGIBLE);

        List<String> MDA_BUSINESS_STATUS = Arrays.asList(NOT_VISITED, FULLY_RECEIVED, NONE_RECEIVED,
                ADHERENCE_VISIT_DONE, PARTIALLY_RECEIVED, COMPLETE, NOT_ELIGIBLE);
    }

    interface BusinessStatusWrapper {

        List<String> SPRAYED = Arrays.asList(BusinessStatus.SPRAYED, BusinessStatus.COMPLETE, BusinessStatus.PARTIALLY_SPRAYED);
        List<String> NOT_SPRAYED = Arrays.asList(BusinessStatus.NOT_SPRAYED, BusinessStatus.IN_PROGRESS, BusinessStatus.INCOMPLETE);
        List<String> NOT_ELIGIBLE = Arrays.asList(BusinessStatus.NOT_SPRAYABLE, BusinessStatus.NOT_ELIGIBLE);
        List<String> NOT_VISITED = Arrays.asList(BusinessStatus.NOT_VISITED);
    }

    interface Map {
        int MAX_SELECT_ZOOM_LEVEL = 16;
        int SELECT_JURISDICTION_MAX_SELECT_ZOOM_LEVEL = 12;
        int CLICK_SELECT_RADIUS = 24;
        String NAME_PROPERTY = "name";
        double DOWNLOAD_MAX_ZOOM = 21.0;
        double DOWNLOAD_MIN_ZOOM = 13.5;
    }

    interface JsonForm {

        String ENCOUNTER_TYPE = "encounter_type";

        String SPRAY_STATUS = "sprayStatus";

        String TRAP_SET_DATE = "trap_start";

        String TRAP_FOLLOW_UP_DATE = "trap_end";

        String BUSINESS_STATUS = "business_status";

        String STRUCTURE_TYPE = "structureType";

        String STRUCTURE = "structure";

        String HEAD_OF_HOUSEHOLD = "familyHeadName";

        String STRUCTURE_PROPERTIES_TYPE = "[structure_type]";

        String NUMBER_OF_FAMILY_MEMBERS = "[num_fam_members]";

        String NUMBER_OF_FAMILY_MEMBERS_SLEEPING_OUTDOORS = "[num_sleeps_outdoors]";

        String SPRAY_FORM = "json.form/spray_form.json";

        String MOSQUITO_COLLECTION_FORM = "json.form/mosquito_collection_form.json";

        String SPRAY_FORM_NAMIBIA = "json.form/namibia_spray_form.json";

        String SPRAY_FORM_BOTSWANA = "json.form/botswana_spray_form.json";

        String SPRAY_FORM_REFAPP = "json.form/refapp_spray_form.json";

        String SPRAY_FORM_ZAMBIA = "json.form/zambia_spray_form.json";

        String SPRAY_FORM_SENEGAL = "json.form/senegal_spray_form.json";

        String LARVAL_DIPPING_FORM = "json.form/larval_dipping_form.json";

        String ADD_STRUCTURE_FORM = "json.form/add_structure.json";

        String BEDNET_DISTRIBUTION_FORM = "json.form/bednet_distribution.json";

        String BLOOD_SCREENING_FORM = "json.form/blood_screening.json";

        String CASE_CONFIRMATION_FORM = "json.form/case_confirmation.json";

        String BEHAVIOUR_CHANGE_COMMUNICATION_FORM = "json.form/behaviour_change_communication.json";

        String PAOT_FORM = "json.form/paot.json";

        String THAILAND_LARVAL_DIPPING_FORM = "json.form/thailand_larval_dipping_form.json";

        String THAILAND_EN_LARVAL_DIPPING_FORM = "json.form/thailand_en_larval_dipping_form.json";

        String THAILAND_MOSQUITO_COLLECTION_FORM = "json.form/thailand_mosquito_collection_form.json";

        String THAILAND_EN_MOSQUITO_COLLECTION_FORM = "json.form/thailand_en_mosquito_collection_form.json";

        String THAILAND_SPRAY_FORM = "json.form/thailand_spray_form.json";

        String THAILAND_ADD_STRUCTURE_FORM = "json.form/thailand_add_structure.json";

        String THAILAND_BEDNET_DISTRIBUTION_FORM = "json.form/thailand_bednet_distribution.json";

        String THAILAND_EN_BEDNET_DISTRIBUTION_FORM = "json.form/thailand_en_bednet_distribution.json";

        String THAILAND_BLOOD_SCREENING_FORM = "json.form/thailand_blood_screening.json";

        String THAILAND_EN_BLOOD_SCREENING_FORM = "json.form/thailand_en_blood_screening.json";

        String THAILAND_CASE_CONFIRMATION_FORM = "json.form/thailand_case_confirmation.json";

        String THAILAND_BEHAVIOUR_CHANGE_COMMUNICATION_FORM = "json.form/thailand_behaviour_change_communication.json";

        String THAILAND_PAOT_FORM = "json.form/thailand_paot.json";

        String ZAMBIA_MDA_DISPENSE_FORM = "json.form/zambia_mda_dispense.json";

        String ZAMBIA_MDA_ADHERENCE_FORM = "json.form/zambia_mda_adherence.json";

        String ZAMBIA_IRS_VERIFICATION_FORM = "json.form/zambia_irs_verification.json";

        String REFAPP_MDA_DISPENSE_FORM = "json.form/refapp_mda_dispense.json";

        String REFAPP_MDA_ADHERENCE_FORM = "json.form/refapp_mda_adherence.json";

        String REFAPP_BEDNET_DISTRIBUTION_FORM = "json.form/refapp_bednet_distribution.json";

        String REFAPP_PAOT_FORM = "json.form/refapp_paot.json";

        String REFAPP_LARVAL_DIPPING_FORM = "json.form/refapp_larval_dipping_form.json";

        String REFAPP_MOSQUITO_COLLECTION_FORM = "json.form/refapp_mosquito_collection_form.json";

        String REFAPP_BLOOD_SCREENING_FORM = "json.form/refapp_blood_screening.json";

        String REFAPP_CASE_CONFIRMATION_FORM = "json.form/refapp_case_confirmation.json";

        String CDD_SUPERVISOR_DAILY_SUMMARY_FORM = "json.form/community_drug_distributor_supervisor_daily_summary_form.json";

        String JSON_FORM_FOLDER = "json.form/";

        String LOCATION_COMPONENT_ACTIVE = "my_location_active";

        String VALID_OPERATIONAL_AREA = "valid_operational_area";

        String NO_PADDING = "no_padding";

        String SHORTENED_HINT = "shortened_hint";

        String HINT = "hint";

        String TITLE = "title";

        String FAMILY_MEMBER = "familyMember";

        String STRUCTURE_NAME = "structureName";

        String PHYSICAL_TYPE = "physicalType";

        String PAOT_STATUS = "paotStatus";

        String PAOT_COMMENTS = "paotComments";

        String LAST_UPDATED_DATE = "lastUpdatedDate";

        String SELECTED_OPERATIONAL_AREA_NAME = "selectedOpAreaName";

        String NAMIBIA_ADD_STRUCTURE_FORM = "json.form/namibia_add_structure.json";

        String HOUSEHOLD_ACCESSIBLE = "householdAccessible";
        String ABLE_TO_SPRAY_FIRST = "ableToSprayFirst";
        String MOP_UP_VISIT = "mopUpVisit";
        String DISTRICT_NAME = "districtName";
        String PROVINCE_NAME = "provinceName";


        /**
         * Non-Task Related Forms
         */
        String DAILY_SUMMARY_ZAMBIA = "json.form/zambia_daily_summary.json";

        String TEAM_LEADER_DOS_ZAMBIA = "json.form/zambia_team_leader_dos.json";

        String CB_SPRAY_AREA_ZAMBIA = "json.form/zambia_cb_spray_area.json";

        String IRS_LITE_VERIFICATION = "json.form/zambia_irs_lite_verification.json";

        String IRS_SA_DECISION_ZAMBIA = "json.form/zambia_irs_sa_decision.json";

        String MOBILIZATION_FORM_ZAMBIA = "json.form/zambia_mobilization_form.json";

        String IRS_FIELD_OFFICER_ZAMBIA = "json.form/zambia_irs_field_officer.json";

        String VERIFICATION_FORM_ZAMBIA = "json.form/zambia_verification_form.json";

        String DAILY_SUMMARY_SENEGAL= "json.form/senegal_daily_summary.json";

        String TEAM_LEADER_DOS_SENEGAL = "json.form/senegal_team_leader_dos.json";

        String CB_SPRAY_AREA_SENEGAL = "json.form/senegal_cb_spray_area.json";

        String IRS_SA_DECISION_SENEGAL = "json.form/senegal_irs_sa_decision.json";

        String MOBILIZATION_FORM_SENEGAL = "json.form/senegal_mobilization_form.json";

        String IRS_FIELD_OFFICER_SENEGAL = "json.form/senegal_irs_field_officer.json";

        String VERIFICATION_FORM_SENEGAL = "json.form/senegal_verification_form.json";

        String TABLET_ACCOUNTABILITY_FORM = "json.form/tablet_accountability_form.json";

        String SPRAY_OPERATOR_CODE = "sprayop_code";

        String DATA_COLLECTOR = "datacollector";

        String DISTRICT_MANAGER = "district_manager";

        String SUPERVISOR = "supervisor";

        String TEAM_LEADER = "teamLeader";

        String FIELD_OFFICER = "fieldOfficer";

        String HFC_SEEK = "hfc_seek";

        String HFC_BELONG = "hfc_belong";

        String CHW_NAME = "chw_name";

        String COORDINATOR_NAME = "coordinator_name";

        String ADHERENCE_NAME = "adherence_name";

        String CATCHMENT_AREA = "catchment_area";

        String HEALTH_FACILITY = "health_facility";

        String DISTRICT = "district";


        String ZONE = "zone";

        String MOP_UP = "mopup";

        String GENERATED_GRP = "generated_group";

        String REPEATING_GROUP_UNIQUE_ID = "unique_id";

        String YES =  "Yes";

        String LOCATION = "location";

        String COMMUNITY_DRUG_DISTRIBUTOR_NAME = "cdd_name";

        String HEALTH_WORKER_SUPERVISOR = "health_worker_supervisor";

        String CDD_SUPERVISION_TASK_COMPLETE = "task_complete";

        String LOCATION_ZONE = "location_zone";


        String COMPOUND_STRUCTURE = "compoundStructure";

        String ROOMS_SPRAYED = "rooms_sprayed";

        String ROOMS_ELIGIBLE = "rooms_eligible";

        String COLLECTION_DATE = "collection_date";


    }

    interface DateFormat {

        String EVENT_DATE_FORMAT_Z = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

        String EVENT_DATE_FORMAT_XXX = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

        String CARD_VIEW_DATE_FORMAT = "dd MMM yyyy";
    }

    interface Action {
        String STRUCTURE_TASK_SYNCED = "reveal.STRUCTURE_TASK_SYNCED";
    }

    interface ECClientConfig {
        String NAMIBIA_EC_CLIENT_FIELDS = "ec_client_fields_namibia.json";
        String BOTSWANA_EC_CLIENT_FIELDS = "ec_client_fields_botswana.json";
        String ZAMBIA_EC_CLIENT_FIELDS = "ec_client_fields_zambia.json";
        String SENEGAL_EC_CLIENT_FIELDS = "ec_client_fields_senegal.json";
        String REFAPP_EC_CLIENT_FIELDS = "ec_client_fields_refapp.json";
        String KENYA_EC_CLIENT_FIELDS = "ec_client_fields_kenya.json";
    }



    interface StructureType {
        String RESIDENTIAL = "Residential Structure";

        String NON_RESIDENTIAL = "Non-Residential Structure";

        String MOSQUITO_COLLECTION_POINT = "Mosquito Collection Point";

        String LARVAL_BREEDING_SITE = "Larval Breeding Site";

        String POTENTIAL_AREA_OF_TRANSMISSION = "Potential Area of Transmission";
    }

    interface TaskRegister {
        String VIEW_IDENTIFIER = "task_register";

        String INTERVENTION_TYPE = "intervention_type";

        String LAST_USER_LOCATION = "last_location";

    }

    interface EventsRegister {
        String VIEW_IDENTIFIER = "event_register";

        String TABLE_NAME = "ec_events";
    }

    interface DatabaseKeys {

        String TASK_TABLE = "task";

        String SPRAYED_STRUCTURES = "sprayed_structures";

        String STRUCTURES_TABLE = "structure";

        String STRUCTURE_NAME = "structure_name";

        String STRUCTURE_ID = "structure_id";

        String ID_ = "_id";

        String ID = "id";

        String CODE = "code";

        String FOR = "for";

        String BUSINESS_STATUS = "business_status";

        String STATUS = "status";

        String REFERENCE_REASON = "reason_reference";

        String FAMILY_NAME = "family_head_name";

        String SPRAY_STATUS = "spray_status";

        String LATITUDE = "latitude";

        String LONGITUDE = "longitude";

        String NAME = "name";

        String GROUPID = "group_id";

        String PLAN_ID = "plan_id";

        String NOT_SRAYED_REASON = "not_sprayed_reason";

        String NOT_SRAYED_OTHER_REASON = "not_sprayed_other_reason";

        String OTHER = "other";

        String COMPLETED_TASK_COUNT = "completed_task_count";

        String TASK_COUNT = "task_count";

        String BASE_ENTITY_ID = "base_entity_id";

        String FIRST_NAME = "first_name";

        String LAST_NAME = "last_name";

        String GROUPED_STRUCTURE_TASK_CODE_AND_STATUS = "grouped_structure_task_code_and_status";

        String GROUPED_TASKS = "grouped_tasks";

        String LAST_UPDATED_DATE = "last_updated_date";

        String PAOT_STATUS = "paot_status";

        String PAOT_COMMENTS = "paot_comments";

        String EVENT_TASK_TABLE = "event_task";

        String EVENT_ID = "event_id";

        String TASK_ID = "task_id";

        String EVENT_DATE = "event_date";

        String EVENTS_PER_TASK = "events_per_task";

        String TRUE_STRUCTURE = "true_structure";

        String ELIGIBLE_STRUCTURE = "eligible_structure";

        String REPORT_SPRAY = "report_spray";

        String CHALK_SPRAY = "chalk_spray";

        String STICKER_SPRAY = "sticker_spray";

        String CARD_SPRAY = "card_spray";

        String SYNC_STATUS = "syncStatus";

        String TASK_SYNC_STATUS = "sync_Status";

        String STRUCTURE_SYNC_STATUS = "sync_Status";

        String VALIDATION_STATUS = "validationStatus";

        String AUTHORED_ON = "authored_on";

        String OWNER = "owner";

        String PROPERTY_TYPE = "property_type";

        String PARENT_ID = "parent_id";

        String FORM_SUBMISSION_ID = "formSubmissionId";

        String EVENT_TYPE_FIELD = "eventType";

        String CASE_CONFIRMATION_FIELD = "case_confirmation";

        String EVENT_TABLE = "event";

        String PERSON_TESTED = "person_tested";

        String LOCATION_TABLE = "location";

        String LOCATION_SYNC_STATUS = "sync_status";

        String SOP = "sop";

        String ENTITY = "entity";

        String EVENT_TYPE = "event_type";

        String SPRAYED = "sprayed";

        String FOUND = "found";

        String PROVIDER_ID = "provider_id";

        String VERSION = "version";

        Object SERVER_VERSION = "server_version";

        Object GEOJSON = "geojson";

        String DATA_COLLECTION_DATE = "data_collection_date";


        String STRUCTURE_HEAD_NAME = "structure_head_name";

        String COMPOUND_HEAD_NAME = "compound_head_name";

        String COMPOUND_STRUCTURE = "compound_structure";

        String SPRAY_DATE = "spray_date";

    }

    interface UseContextCode {
        String INTERVENTION_TYPE = "interventionType";
    }

    interface IRSVerificationStatus {
        String SPRAYED = "sprayed";
        String NOT_SPRAYED = "notSprayed";
        String NOT_FOUND_OR_VISITED = "notFoundOrVisited";
        String OTHER = "other";
    }

    interface SyncInfo {
        String SYNCED_EVENTS = "syncedEvents";
        String SYNCED_CLIENTS = "syncedClients";
        String UNSYNCED_EVENTS = "unsyncedEvents";
        String UNSYNCED_CLIENTS = "unsyncedClients";
        String VALID_EVENTS = "validEvents";
        String INVALID_EVENTS = "invalidEvents";
        String VALID_CLIENTS = "validClients";
        String INVALID_CLIENTS = "INValidClients";
        String TASK_UNPROCESSED_EVENTS = "taskUnprocessedEvents";
        String NULL_EVENT_SYNC_STATUS = "nullEventSyncStatus";
    }

    interface RequestCode {
        int REQUEST_CODE_GET_JSON = 3432;

        int REQUEST_CODE_GET_JSON_FRAGMENT = 3439;

        int REQUEST_CODE_FAMILY_PROFILE = 3576;

        int REQUEST_CODE_FILTER_TASKS = 3596;

        int REQUEST_CODE_TASK_LISTS = 3617;
    }


    interface InterventionType {
        String OPERATIONAL_AREA = "operational_area";
        String STRUCTURE = "structure";
        String FAMILY = "family";
        String PERSON = "person";

        List<String> FILTERABLE_INTERVENTION_TYPES = Arrays.asList(OPERATIONAL_AREA, STRUCTURE, FAMILY, PERSON);

    }

    interface Filter {
        String CODE = "task_code";
        String STATUS = "task_status";
        String INTERVENTION_UNIT = "intervention_unit";
        String FORM_NAME = "form_name";
        String FILTER_SORT_PARAMS = "filter_sort_params";
        String FILTER_CONFIGURATION = "filter_configuration";
    }
}
