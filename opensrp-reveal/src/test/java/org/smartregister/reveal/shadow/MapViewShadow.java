package org.smartregister.reveal.shadow;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowViewGroup;

/**
 * Created by samuelgithengi on 1/23/20.
 */
@Implements(MapView.class)
public class MapViewShadow extends ShadowViewGroup {

    @Implementation
    public void __constructor__(@NonNull Context context, @Nullable AttributeSet attrs) {
        // Do nothing
    }

    @Implementation
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //Do nothing
    }

    @Implementation
    public void getMapAsync(final @NonNull OnMapReadyCallback callback) {
        //Do nothing
    }

    @Implementation
    public void onStart() {
        //Do nothing
    }

}
