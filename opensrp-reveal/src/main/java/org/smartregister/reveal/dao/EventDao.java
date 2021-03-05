package org.smartregister.reveal.dao;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.dao.AbstractDao;

import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

public class EventDao extends AbstractDao {

    public static EventDao getInstance() {
        return new EventDao();
    }

    public JSONObject getLastEvent(String baseEntityID, String eventType) {
        AtomicReference<JSONObject> jsonObject = new AtomicReference<>();

        String sql = "select json from event where baseEntityId = '" + baseEntityID + "' and eventType = '" + eventType + "'  order by dateCreated desc limit 1";

        DataMap<Void> dataMap = cursor -> {
            String json = getCursorValue(cursor, "json");
            try {
                jsonObject.set(new JSONObject(json));
            } catch (JSONException e) {
                Timber.e(e);
            }
            return null;
        };

        readData(sql, dataMap);

        return jsonObject.get();
    }
}
