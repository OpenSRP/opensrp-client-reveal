package org.smartregister.reveal.repository;

import android.location.Location;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.gson.GeometryGeoJson;
import com.mapbox.turf.TurfMeasurement;

import org.smartregister.repository.helper.MappingHelper;

/**
 * Created by samuelgithengi on 3/19/19.
 */
public class RevealMappingHelper implements MappingHelper {
    /**
     * Generates the center from the {@link Geometry} of a given {@link Feature} for {@link Geometry}
     * of types {@link com.mapbox.geojson.MultiPolygon}, {@link com.mapbox.geojson.Polygon} and
     * {@link com.mapbox.geojson.MultiPoint}
     *
     * @param geometry the geometry of structure
     * @return center
     */
    @Override
    public Location getCenter(@NonNull String geometry) {
        double[] bbox = TurfMeasurement.bbox(GeometryGeoJson.fromJson(geometry));
        Location center = new Location((String) null);
        center.setLongitude((bbox[2] + bbox[0]) / 2);
        center.setLatitude((bbox[3] + bbox[1]) / 2);
        return center;
    }
}
