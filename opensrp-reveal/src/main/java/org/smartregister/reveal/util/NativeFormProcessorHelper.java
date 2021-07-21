package org.smartregister.reveal.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.domain.Location;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.reveal.application.RevealApplication;
import org.smartregister.reveal.sync.RevealClientProcessor;
import org.smartregister.util.NativeFormProcessor;

/**
 * Factory class to generate native forms processor
 */
public class NativeFormProcessorHelper {

    public static NativeFormProcessor createInstance(String jsonString) throws JSONException {
        return NativeFormProcessor.createInstance(jsonString, FamilyLibrary.getInstance().getDatabaseVersion(), RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext()));
    }

    public static NativeFormProcessor createInstance(JSONObject jsonObject) {
        return NativeFormProcessor.createInstance(jsonObject, FamilyLibrary.getInstance().getDatabaseVersion(), RevealClientProcessor.getInstance(RevealApplication.getInstance().getApplicationContext()));
    }

    public static Location getCurrentOperationalArea() {
        return Utils.getOperationalAreaLocation(PreferencesUtil.getInstance().getCurrentOperationalArea());
    }

    public static Location getCurrentSelectedStructure() {
        return Utils.getStructureByName(PreferencesUtil.getInstance().getCurrentStructure());
    }
}
