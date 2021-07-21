package org.smartregister.reveal.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.dao.AbstractDao;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;
import org.smartregister.util.Cache;
import org.smartregister.util.PropertiesConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StructureDao extends AbstractDao {

    protected static Cache<Map<String, List<Location>>> cache = new Cache<>();
    protected static String CACHED_STRUCTURES = "CACHED_STRUCTURES";

    public static Map<String, List<Location>> getStructuresByParent() {
        Map<String, List<Location>> result = new HashMap<>();

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HHmm")
                .registerTypeAdapter(LocationProperty.class, new PropertiesConverter()).create();

        String sql = "select l.name , s.geojson from structure s inner join location l on s.parent_id = l._id";

        DataMap<Void> dataMap = cursor -> {
            String parent_id = getCursorValue(cursor, "name");
            if (StringUtils.isNotBlank(parent_id)) {
                List<Location> locations = result.get(parent_id);
                if (locations == null)
                    locations = new ArrayList<>();


                String geoJson = getCursorValue(cursor, "geojson");
                Location location = gson.fromJson(geoJson, Location.class);
                locations.add(location);

                result.put(parent_id, locations);
            }
            return null;
        };

        readData(sql, dataMap);

        return result;
    }

    public static Map<String, List<Location>> getCachedStructures() {
        return cache.get(CACHED_STRUCTURES, StructureDao::getStructuresByParent);
    }

    public static void resetLocationCache() {
        if (cache != null)
            cache.evict(CACHED_STRUCTURES);
    }
}
