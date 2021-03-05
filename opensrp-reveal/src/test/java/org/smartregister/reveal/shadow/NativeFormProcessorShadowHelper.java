package org.smartregister.reveal.shadow;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.domain.Location;
import org.smartregister.reveal.util.NativeFormProcessor;

import java.util.Date;

@Implements(NativeFormProcessor.class)
public class NativeFormProcessorShadowHelper {

    private static NativeFormProcessor processorSpy;

    @Implementation
    public static NativeFormProcessor createInstance(String jsonString) throws JSONException {
        processorSpy = Mockito.spy(new NativeFormProcessor(new JSONObject(jsonString)));
        return processorSpy;
    }

    @Implementation
    public static NativeFormProcessor createInstance(JSONObject jsonObject) {
        processorSpy = Mockito.spy(new NativeFormProcessor(jsonObject));
        return processorSpy;
    }

    public static NativeFormProcessor getProcessorSpy() {
        return processorSpy;
    }

    @Implementation
    public Location getCurrentOperationalArea() {
        Location location = new Location();
        location.setServerVersion(new Date().getTime());
        return location;
    }

    @Implementation
    public Location getCurrentSelectedStructure() {
        Location location = new Location();
        location.setServerVersion(new Date().getTime());
        return location;
    }


    @Implementation
    public NativeFormProcessor closeRegistrationID(String uniqueIDKey) {// Do Nothing
        return null;
    }

    @Implementation
    public NativeFormProcessor mergeAndSaveClient() {
        // do nothing else
        return processorSpy;
    }
}
