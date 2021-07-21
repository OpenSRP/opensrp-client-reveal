package org.smartregister.reveal.util;

import org.junit.Assert;
import org.junit.Test;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.jsonmapping.Location;
import org.smartregister.domain.jsonmapping.util.TreeNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationHierarchyUtilTest {

    @Test
    public void testGrowTree() {

        Map<String, TreeNode<String, Location>> map = new HashMap<>();

        Location location = new Location();
        TreeNode<String, Location> treeNode = new TreeNode<>(null, "Country", location, null);
        map.put("country", treeNode);

        Map<String, List<org.smartregister.domain.Location>> newData = new HashMap<>();
        org.smartregister.domain.Location child = new org.smartregister.domain.Location();
        child.setId("province");
        LocationProperty property = new LocationProperty();
        property.setName("Province");
        child.setProperties(property);
        List<org.smartregister.domain.Location> children = new ArrayList<>();
        children.add(child);

        newData.put("Country", children);

        LocationHierarchyUtil.growTree(map, newData);

        Assert.assertEquals(map.get("country").getChildren().size(), 1);
    }
}
