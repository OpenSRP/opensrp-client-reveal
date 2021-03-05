package org.smartregister.reveal.util;

import org.smartregister.domain.jsonmapping.Location;
import org.smartregister.domain.jsonmapping.util.TreeNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.smartregister.reveal.util.Constants.Tags.SCHOOL;

public class LocationHierarchyUtil {

    public static void growTree(Map<String, TreeNode<String, Location>> map, Map<String, List<org.smartregister.domain.Location>> listMap) {
        if (listMap != null && map != null && listMap.size() > 0 && map.size() > 0) {
            for (Map.Entry<String, TreeNode<String, Location>> child : map.entrySet()) {
                growTreeNode(child.getValue(), listMap);
            }
        }
    }

    private static void growTreeNode(TreeNode<String, Location> node, Map<String, List<org.smartregister.domain.Location>> listMap) {
        if (node.getChildren() != null && node.getChildren().size() > 0) {
            for (Map.Entry<String, TreeNode<String, Location>> child : node.getChildren().entrySet()) {
                growTreeNode(child.getValue(), listMap);
            }
        } else {
            // check child and add node
            List<org.smartregister.domain.Location> structures = listMap.get(node.getLabel());
            if (structures != null) {

                for (org.smartregister.domain.Location oldLocation : structures) {

                    Location location = new Location();
                    location.setName(oldLocation.getProperties().getName());
                    location.setAttributes(Collections.unmodifiableMap(oldLocation.getProperties().getCustomProperties()));

                    Set<String> tag = new HashSet<>();
                    tag.add(SCHOOL);
                    location.setTags(tag);

                    TreeNode<String, Location> newNode = new TreeNode<String, Location>(
                            oldLocation.getId(),
                            oldLocation.getProperties().getName(),
                            location,
                            node.getId()
                    );
                    node.addChild(newNode);
                }

            }
        }
    }
}
