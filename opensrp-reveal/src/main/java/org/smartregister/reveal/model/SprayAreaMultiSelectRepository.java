package org.smartregister.reveal.model;

import com.vijay.jsonwizard.domain.MultiSelectItem;
import com.vijay.jsonwizard.interfaces.MultiSelectListRepository;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.location.helper.LocationHelper;
import org.smartregister.reveal.util.PreferencesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import timber.log.Timber;

public class SprayAreaMultiSelectRepository implements MultiSelectListRepository {
    @Override
    public List<MultiSelectItem> fetchData() {
        List<MultiSelectItem> multiSelectItems = new ArrayList<>();
        final String currentFacility = PreferencesUtil.getInstance().getCurrentFacility();
        List<String> locationHierarchy = LocationHelper.getInstance().locationNamesFromHierarchy(currentFacility).stream().filter(name -> !name.equals(currentFacility)).collect(Collectors.toList());
        JSONObject property = new JSONObject();
        try {
            property.put("presumed-id","err");
            property.put("confirmed-id","err");

        } catch (JSONException e) {
            Timber.e(e);
        }
        locationHierarchy.stream().forEach(location -> {
            MultiSelectItem item = new MultiSelectItem();
            item.setKey(location);
            item.setText(location);
            item.setOpenmrsEntityId("");
            item.setOpenmrsEntity("");
            item.setOpenmrsEntityParent("");
            item.setValue(property.toString());
            multiSelectItems.add(item);
        });
        return multiSelectItems;
    }
}
