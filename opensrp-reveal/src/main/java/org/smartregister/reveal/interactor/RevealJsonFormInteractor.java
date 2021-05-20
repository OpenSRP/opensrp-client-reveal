package org.smartregister.reveal.interactor;

import com.vijay.jsonwizard.interactors.JsonFormInteractor;

import org.smartregister.reveal.BuildConfig;
import org.smartregister.reveal.util.Country;
import org.smartregister.reveal.util.Utils;
import org.smartregister.reveal.widget.GeoWidgetFactory;
import org.smartregister.reveal.widget.RevealEditTextFactory;
import org.smartregister.reveal.widget.RevealLabelFactory;
import org.smartregister.reveal.widget.RevealRadioButtonFactory;
import org.smartregister.reveal.widget.RevealRepeatingGroupFactory;
import org.smartregister.reveal.widget.RevealToasterNotesFactory;

import static com.vijay.jsonwizard.constants.JsonFormConstants.EDIT_TEXT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.LABEL;
import static com.vijay.jsonwizard.constants.JsonFormConstants.NATIVE_RADIO_BUTTON;
import static com.vijay.jsonwizard.constants.JsonFormConstants.REPEATING_GROUP;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TOASTER_NOTES;

/**
 * Created by samuelgithengi on 12/13/18.
 */
public class RevealJsonFormInteractor extends JsonFormInteractor {


    private static final RevealJsonFormInteractor INSTANCE = new RevealJsonFormInteractor();

    private static final String GEOWIDGET = "geowidget";

    public static JsonFormInteractor getInstance() {
        return INSTANCE;
    }

    @Override
    protected void registerWidgets() {
        super.registerWidgets();

        if (BuildConfig.BUILD_COUNTRY == Country.NAMIBIA) {
            map.put(GEOWIDGET, new GeoWidgetFactory());
        } else {
            map.put(GEOWIDGET, new GeoWidgetFactory(false));
        }

        map.put(EDIT_TEXT, new RevealEditTextFactory());
        map.put(NATIVE_RADIO_BUTTON, new RevealRadioButtonFactory());
        map.put(LABEL, new RevealLabelFactory());
        map.put(TOASTER_NOTES, new RevealToasterNotesFactory());

        if (BuildConfig.BUILD_COUNTRY == Country.SENEGAL){
            map.put(REPEATING_GROUP, new RevealRepeatingGroupFactory());
        }

    }

}
