package org.smartregister.reveal.shadow;

import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.domain.Location;
import org.smartregister.reveal.util.NativeFormProcessorHelper;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.NativeFormProcessor;

import java.util.Date;

@Implements(NativeFormProcessorHelper.class)
public class NativeFormProcessorShadowHelper {

    private static NativeFormProcessor processorSpy;
    private static Location location = new Location();
    private static Location structure = new Location();

    @Implementation
    public static NativeFormProcessor createInstance(String jsonString) throws JSONException {
        processorSpy = Mockito.spy(new NativeFormProcessor(new JSONObject(jsonString), 1, Mockito.mock(ClientProcessorForJava.class)));
        Mockito.doReturn(getSyncHelper()).when(processorSpy).getSyncHelper();
        return processorSpy;
    }

    @Implementation
    public static NativeFormProcessor createInstance(JSONObject jsonObject) {
        processorSpy = Mockito.spy(new NativeFormProcessor(jsonObject, 1, Mockito.mock(ClientProcessorForJava.class)));
        Mockito.doReturn(getSyncHelper()).when(processorSpy).getSyncHelper();
        return processorSpy;
    }

    public static NativeFormProcessor getProcessorSpy() {
        return processorSpy;
    }

    @Implementation
    public static Location getCurrentOperationalArea() {
        location.setServerVersion(new Date().getTime());
        return location;
    }

    @Implementation
    public static Location getCurrentSelectedStructure() {
        structure.setServerVersion(new Date().getTime());
        return structure;
    }

    /**
     * Mock sync helper to return a fake saved client
     *
     * @return
     */
    private static ECSyncHelper getSyncHelper() {
        ECSyncHelper syncHelper = Mockito.mock(ECSyncHelper.class);
        Mockito.doReturn(new JSONObject()).when(syncHelper).getClient(Mockito.anyString());
        Mockito.doReturn(new JSONObject()).when(syncHelper).getClient(null);
        return syncHelper;
    }


}
