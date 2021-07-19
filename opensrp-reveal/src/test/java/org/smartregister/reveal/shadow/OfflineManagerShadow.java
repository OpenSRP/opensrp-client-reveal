package org.smartregister.reveal.shadow;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mapbox.mapboxsdk.offline.OfflineManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;

/**
 * Created by Richard Kareko on 1/30/20.
 */

@Implements(OfflineManager.class)
public class OfflineManagerShadow {

    @Implementation
    public static synchronized OfflineManager getInstance(@NonNull Context context){
        return mock(OfflineManager.class);
    }
}
