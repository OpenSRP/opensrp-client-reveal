package org.smartregister.reveal.shadow;

import org.mockito.Mockito;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;

@Implements(SyncStatusBroadcastReceiver.class)
public class SyncStatusBroadcastReceiverShadowHelper {

    private static SyncStatusBroadcastReceiver receiver = Mockito.mock(SyncStatusBroadcastReceiver.class);

    @Implementation
    public static SyncStatusBroadcastReceiver getInstance() {
        return receiver;
    }
}
