package org.smartregister.reveal.repository;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.AllConstants;
import org.smartregister.configurableviews.repository.ConfigurableViewsRepository;
import org.smartregister.domain.db.EventClient;
import org.smartregister.job.PullUniqueIdsServiceJob;
import org.smartregister.repository.BaseRepository;
import org.smartregister.repository.CampaignRepository;
import org.smartregister.repository.ClientFormRepository;
import org.smartregister.repository.ClientRelationshipRepository;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.EventClientRepository.client_column;
import org.smartregister.repository.EventClientRepository.event_column;
import org.smartregister.repository.LocationRepository;
import org.smartregister.repository.ManifestRepository;
import org.smartregister.repository.PlanDefinitionRepository;
import org.smartregister.repository.PlanDefinitionSearchRepository;
import org.smartregister.repository.Repository;
import org.smartregister.repository.SettingsRepository;
import org.smartregister.repository.StructureRepository;
import org.smartregister.repository.TaskRepository;
import org.smartregister.repository.UniqueIdRepository;
import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.reveal.util.Constants;
import org.smartregister.reveal.util.Constants.DatabaseKeys;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.FamilyConstants.EventType;
import org.smartregister.reveal.util.Utils;
import org.smartregister.util.DatabaseMigrationUtils;
import org.smartregister.util.RecreateECUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import timber.log.Timber;

import static org.smartregister.repository.EventClientRepository.Table.event;
import static org.smartregister.repository.EventClientRepository.event_column.baseEntityId;
import static org.smartregister.repository.EventClientRepository.event_column.eventType;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.BASE_ENTITY_ID;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.EVENT_TASK_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.LOCATION_TABLE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.PROPERTY_TYPE;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAYED_STRUCTURES;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.SPRAY_STATUS;
import static org.smartregister.reveal.util.Constants.DatabaseKeys.STRUCTURE_ID;
import static org.smartregister.reveal.util.Constants.EventType.PAOT_EVENT;
import static org.smartregister.reveal.util.Constants.LARVAL_DIPPING_EVENT;
import static org.smartregister.reveal.util.Constants.MOSQUITO_COLLECTION_EVENT;
import static org.smartregister.reveal.util.Constants.REGISTER_STRUCTURE_EVENT;
import static org.smartregister.reveal.util.Constants.SPRAY_EVENT;
import static org.smartregister.reveal.util.Constants.STRUCTURE;
import static org.smartregister.reveal.util.Constants.StructureType.RESIDENTIAL;
import static org.smartregister.reveal.util.Constants.Tables.CLIENT_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.EVENT_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.LARVAL_DIPPINGS_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.MOSQUITO_COLLECTIONS_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.PAOT_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.STRUCTURE_TABLE;
import static org.smartregister.reveal.util.Constants.Tables.TASK_TABLE;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY;
import static org.smartregister.reveal.util.FamilyConstants.TABLE_NAME.FAMILY_MEMBER;
import static org.smartregister.util.DatabaseMigrationUtils.isColumnExists;


public class RevealRepository extends Repository {

    protected SQLiteDatabase readableDatabase;
    protected SQLiteDatabase writableDatabase;


    public RevealRepository(Context context, org.smartregister.Context openSRPContext) {
        super(context, AllConstants.DATABASE_NAME, BuildConfig.DATABASE_VERSION, openSRPContext.session(), RevealApplication.createCommonFtsObject(), openSRPContext.sharedRepositoriesArray());
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        super.onCreate(database);
        ConfigurableViewsRepository.createTable(database);
        EventClientRepository.createTable(database, EventClientRepository.Table.client, client_column.values());
        EventClientRepository.createTable(database, event, event_column.values());

        CampaignRepository.createTable(database);
        TaskRepository.createTable(database);
        LocationRepository.createTable(database);
        StructureRepository.createTable(database);

        onUpgrade(database, 1, BuildConfig.DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Timber.w("Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");

        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            switch (upgradeTo) {
                case 2:
                    upgradeToVersion2(db);
                    break;
                case 3:
                    upgradeToVersion3(db);
                    break;
                case 4:
                    upgradeToVersion4(db);
                    break;
                case 5:
                    upgradeToVersion5(db);
                    break;
                case 6:
                    upgradeToVersion6(db);
                    break;
                case 7:
                    upgradeToVersion7(db);
                    break;
                case 8:
                    upgradeToVersion8(db);
                    break;
                case 9:
                    upgradeToVersion9(db);
                    break;
                case 10:
                    upgradeToVersion10(db);
                    break;
                case 11:
                    upgradeToVersion11(db);
                    break;
                case 12:
                    upgradeToVersion12(db);
                    break;
                case 13:
                    upgradeToVersion13(db);
                    break;
                case 14:
                    upgradeToVersion14(db);
                    break;
                default:
                    break;
            }
            upgradeTo++;
        }
    }

    private void upgradeToVersion2(SQLiteDatabase db) {
        SettingsRepository.onUpgrade(db);

        UniqueIdRepository.createTable(db);

        if (!isColumnExists(db, SPRAYED_STRUCTURES, DatabaseKeys.STRUCTURE_NAME)) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR ", SPRAYED_STRUCTURES, DatabaseKeys.STRUCTURE_NAME));
        }

        DatabaseMigrationUtils.createAddedECTables(db,
                new HashSet<>(Arrays.asList(FAMILY, FAMILY_MEMBER)),
                RevealApplication.createCommonFtsObject());

        //client process family events after 5 seconds so that get calls to getDatabase return
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                EventClientRepository ecRepository = RevealApplication.getInstance().getContext().getEventClientRepository();
                List<EventClient> eventClientList = ecRepository.fetchEventClientsByEventTypes(
                        Arrays.asList(EventType.FAMILY_REGISTRATION, EventType.FAMILY_MEMBER_REGISTRATION,
                                EventType.UPDATE_FAMILY_REGISTRATION, EventType.UPDATE_FAMILY_MEMBER_REGISTRATION));
                RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext()).processClient(eventClientList);
            }
        }, 5000);

        PullUniqueIdsServiceJob.scheduleJobImmediately(PullUniqueIdsServiceJob.TAG);

        PlanDefinitionRepository.createTable(db);
        PlanDefinitionSearchRepository.createTable(db);
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
        String createFamilyMemberIndex = String.format("CREATE INDEX family_member_index ON %s (\n" +
                "    %s COLLATE NOCASE, %s COLLATE NOCASE\n" +
                ");", FAMILY_MEMBER, STRUCTURE_ID, BASE_ENTITY_ID);

        String createFamilyResidenceIndex = String.format("CREATE INDEX family_residence_index ON %s (\n" +
                "    %s COLLATE NOCASE \n" +
                ");", FAMILY, STRUCTURE_ID);

        db.execSQL(createFamilyMemberIndex);
        db.execSQL(createFamilyResidenceIndex);
    }

    private void upgradeToVersion4(SQLiteDatabase db) {
        RecreateECUtil util = new RecreateECUtil();
        //recreate spray events
        String query = String.format("select * from %s where %s=? and %s is not null and %s not in (select %s from %s where %s=?) ", SPRAYED_STRUCTURES, PROPERTY_TYPE, SPRAY_STATUS, BASE_ENTITY_ID, baseEntityId, event.name(), eventType);
        String[] params = new String[]{RESIDENTIAL, SPRAY_EVENT};
        Utils.recreateEventAndClients(query, params, db, Utils.getFormTag(), SPRAYED_STRUCTURES, SPRAY_EVENT, STRUCTURE, util);


        //recreate family events and clients
        query = String.format("select * from %s where %s not in (select %s from %s ) ", FAMILY, BASE_ENTITY_ID, baseEntityId, event.name());
        Utils.recreateEventAndClients(query, new String[]{}, db, Utils.getFormTag(), FAMILY, EventType.FAMILY_REGISTRATION, FAMILY, util);


        //recreate family member events and clients
        query = String.format("select * from %s where %s not in (select %s from %s ) ", FAMILY_MEMBER, BASE_ENTITY_ID, baseEntityId, event.name());
        Utils.recreateEventAndClients(query, new String[]{}, db, Utils.getFormTag(), FAMILY_MEMBER, EventType.FAMILY_MEMBER_REGISTRATION, FAMILY_MEMBER, util);


        //recreate larval dipping events and clients
        query = String.format("select * from %s where %s not in (select %s from %s ) ", LARVAL_DIPPINGS_TABLE, BASE_ENTITY_ID, baseEntityId, event.name());
        Utils.recreateEventAndClients(query, new String[]{}, db, Utils.getFormTag(), LARVAL_DIPPINGS_TABLE, LARVAL_DIPPING_EVENT, STRUCTURE, util);

        //recreate mosquito collection events and clients
        query = String.format("select * from %s where %s not in (select %s from %s ) ", MOSQUITO_COLLECTIONS_TABLE, BASE_ENTITY_ID, baseEntityId, event.name());
        Utils.recreateEventAndClients(query, new String[]{}, db, Utils.getFormTag(), MOSQUITO_COLLECTIONS_TABLE, MOSQUITO_COLLECTION_EVENT, STRUCTURE, util);


        //recreate poat events and clients
        query = String.format("select * from %s where %s not in (select %s from %s ) ", PAOT_TABLE, BASE_ENTITY_ID, baseEntityId, event.name());
        Utils.recreateEventAndClients(query, new String[]{}, db, Utils.getFormTag(), PAOT_TABLE, PAOT_EVENT, STRUCTURE, util);
    }

    private void upgradeToVersion5(SQLiteDatabase db) {
        if ((BuildConfig.BUILD_COUNTRY == Country.THAILAND || BuildConfig.BUILD_COUNTRY == Country.THAILAND_EN || BuildConfig.BUILD_COUNTRY == Country.REFAPP) && !isColumnExists(db, EVENT_TASK_TABLE, DatabaseKeys.PERSON_TESTED)) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR ", EVENT_TASK_TABLE, DatabaseKeys.PERSON_TESTED));
        }
    }

    private void upgradeToVersion6(SQLiteDatabase db) {
        ClientFormRepository.createTable(db);
        ManifestRepository.createTable(db);
    }

    private void upgradeToVersion7(SQLiteDatabase db) {
        if (!isColumnExists(db, LOCATION_TABLE, DatabaseKeys.LOCATION_SYNC_STATUS)) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s VARCHAR DEFAULT %s ", LOCATION_TABLE, DatabaseKeys.LOCATION_SYNC_STATUS, BaseRepository.TYPE_Synced));
        }
    }

    private void upgradeToVersion8(SQLiteDatabase db) {
        // replace whitespaces with underscores in event type field
        db.execSQL(String.format("UPDATE %s set %s = REPLACE(%s, ' ', '_') ", EVENT_TABLE, DatabaseKeys.EVENT_TYPE_FIELD, DatabaseKeys.EVENT_TYPE_FIELD));

        if (!ManifestRepository.isVersionColumnExist(db)) {
            ManifestRepository.addVersionColumn(db);
        }

        DatabaseMigrationUtils.createAddedECTables(db,
                new HashSet<>(Arrays.asList(Constants.EventsRegister.TABLE_NAME)),
                RevealApplication.createCommonFtsObject());

        EventClientRepository.createTable(db,
                EventClientRepository.Table.foreignEvent,
                event_column.values());
        EventClientRepository.createTable(db,
                EventClientRepository.Table.foreignClient,
                client_column.values());
    }

    private void upgradeToVersion9(SQLiteDatabase db) {
        ClientRelationshipRepository.createTable(db);
        EventClientRepository.createAdditionalColumns(db);
        EventClientRepository.addEventLocationId(db);
    }

    private void upgradeToVersion10(SQLiteDatabase db) {
        if (BuildConfig.BUILD_COUNTRY != Country.ZAMBIA
                || BuildConfig.BUILD_COUNTRY != Country.SENEGAL) {
            return;
        }
        db.delete(Constants.Tables.EC_EVENTS_TABLE, String.format(" %s=?", DatabaseKeys.EVENT_TYPE), new String[]{SPRAY_EVENT});
        db.delete(Constants.Tables.EC_EVENTS_SEARCH_TABLE, String.format("%s=?", DatabaseKeys.EVENT_TYPE), new String[]{SPRAY_EVENT});

        clientProcessEvents(Collections.singletonList(SPRAY_EVENT));

    }

    private void upgradeToVersion11(SQLiteDatabase db) {
        if (BuildConfig.BUILD_COUNTRY != Country.NAMIBIA && BuildConfig.BUILD_COUNTRY != Country.REFAPP) {
            return;
        }
        db.delete(SPRAYED_STRUCTURES, null, null);

        clientProcessEvents(Arrays.asList(SPRAY_EVENT, REGISTER_STRUCTURE_EVENT));
    }

    private void upgradeToVersion12(SQLiteDatabase db) {
        TaskRepository.updatePriorityToEnumAndAddRestrictions(db);
    }

    private void upgradeToVersion13(SQLiteDatabase db) {
        db.execSQL(String.format("UPDATE %s set %s = ? WHERE %s=? ", EVENT_TABLE, DatabaseKeys.SYNC_STATUS, DatabaseKeys.SYNC_STATUS), new String[]{BaseRepository.TYPE_Unsynced, BaseRepository.TYPE_Task_Unprocessed});
    }


    private void upgradeToVersion14(SQLiteDatabase db) {
        db.execSQL(String.format("UPDATE %s set %s = ? WHERE %s IS NULL ", EVENT_TABLE, event_column.syncStatus, event_column.eventId), new String[]{BaseRepository.TYPE_Unsynced});
        db.execSQL(String.format("UPDATE %s set %s = ? WHERE %s like ?  OR %s not like ?", CLIENT_TABLE, client_column.syncStatus, client_column.json, client_column.json), new String[]{BaseRepository.TYPE_Unsynced, "%serverVersion\":0%", "%serverVersion%"});
        db.execSQL(String.format("UPDATE %s set %s = ? WHERE %s IS NULL OR %s = 0 ", TASK_TABLE, DatabaseKeys.TASK_SYNC_STATUS, DatabaseKeys.SERVER_VERSION, DatabaseKeys.SERVER_VERSION), new String[]{BaseRepository.TYPE_Created});
        db.execSQL(String.format("UPDATE %s set %s = ? WHERE %s like ?  OR %s not like ?", STRUCTURE_TABLE, DatabaseKeys.TASK_SYNC_STATUS, DatabaseKeys.GEOJSON, DatabaseKeys.GEOJSON), new String[]{BaseRepository.TYPE_Created, "%serverVersion\":0%", "%serverVersion%"});
    }

    private void clientProcessEvents(List<String> eventTypes) {
        //client process events after 5 seconds so that get calls to getDatabase return
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                EventClientRepository ecRepository = RevealApplication.getInstance().getContext().getEventClientRepository();
                List<EventClient> eventClientList = ecRepository.fetchEventClientsByEventTypes(
                        eventTypes);
                RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext()).processClient(eventClientList);
            }
        }, 5000);

    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        return getReadableDatabase(RevealApplication.getInstance().getPassword());
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return getWritableDatabase(RevealApplication.getInstance().getPassword());
    }

    @Override
    public synchronized SQLiteDatabase getReadableDatabase(String password) {
        if (StringUtils.isBlank(password)) {
            throw new IllegalStateException("Password is blank");
        }
        try {
            if (readableDatabase == null || !readableDatabase.isOpen()) {
                readableDatabase = super.getReadableDatabase(password);
            }
            return readableDatabase;
        } catch (Exception e) {
            Timber.e(e, "Database Error. ");
            return null;
        }

    }

    @Override
    public synchronized SQLiteDatabase getWritableDatabase(String password) {
        if (StringUtils.isBlank(password)) {
            throw new IllegalStateException("Password is blank");
        } else if (writableDatabase == null || !writableDatabase.isOpen()) {
            writableDatabase = super.getWritableDatabase(password);
        }
        return writableDatabase;
    }

    @Override
    public synchronized void close() {
        if (readableDatabase != null) {
            readableDatabase.close();
        }

        if (writableDatabase != null) {
            writableDatabase.close();
        }
        super.close();
    }
}
