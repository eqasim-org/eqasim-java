package org.eqasim.core.components.traffic_light.delays;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.Id;

import java.util.*;
import java.util.stream.Collectors;

public class IntersectionGroups {

    private static final double BEARING_TOLERANCE_DEGREES = 10.0;

    /**
     * Groups incoming links at a node based on their names and bearings.
     * <p>
     * If there are 2 or fewer incoming links, all are grouped together.
     * Otherwise, links are grouped if they share the same non-empty name
     * or if their bearings differ by less than or equal to BEARING_TOLERANCE_DEGREES.
     *
     * @param  inLinksList : a list of incoming links to be grouped
     * @return a list of groups, each group being a list of links
     */
    public static List<List<Link>> groupInLinks(List<Link> inLinksList) {
        // if only one inlink, it should have multiple outlink. In most cases it has two outlink. So, the outlink might be
        // treated as separate groups. this could be done by having the same link twice in the list.
        if (inLinksList.size() == 1) {
            List<List<Link>> groups = new ArrayList<>();
            groups.add(Collections.singletonList(inLinksList.getFirst()));
            groups.add(Collections.singletonList(inLinksList.getFirst()));
            return groups;
        }

        // If 2 in links, return each as a group
        if (inLinksList.size() == 2) {
            List<List<Link>> groups = new ArrayList<>();
            groups.add(Collections.singletonList(inLinksList.get(0)));
            groups.add(Collections.singletonList(inLinksList.get(1)));
            return groups;
        }

        // Extract names and bearings for all links for later comparison
        List<String> names = inLinksList.stream()
                .map(IntersectionGroups::getLinkName)
                .toList();
        // check names only if there are different names
        boolean checkNames = new HashSet<>(names).size() > 1;

        List<Double> bearings = inLinksList.stream()
                .map(IntersectionGroups::getBearing)
                .toList();

        List<List<Link>> groups = new ArrayList<>();
        boolean[] visited = new boolean[inLinksList.size()];

        // Iterate over each link to form groups
        for (int i = 0; i < inLinksList.size(); i++) {
            if (visited[i]) continue;

            List<Link> group = new ArrayList<>();
            group.add(inLinksList.get(i));
            visited[i] = true;

            // Compare with remaining unvisited links
            for (int j = i + 1; j < inLinksList.size(); j++) {
                if (visited[j]) continue;

                boolean shouldGroup = false;

                // Criterion 1: Group if names are non-null, non-empty, and equal
                if (checkNames) {
                    String nameI = names.get(i);
                    String nameJ = names.get(j);
                    if (nameI != null && !nameI.isEmpty() && nameI.equals(nameJ)) {
                        shouldGroup = true;
                    }
                }
                // Criterion 2: Group if bearings are within tolerance (nearly straight)
                if (!shouldGroup) {
                    double diff = angleDifference(bearings.get(i), bearings.get(j));
                    if (diff <= BEARING_TOLERANCE_DEGREES) {
                        shouldGroup = true;
                    }
                }

                // Add to group if any criterion is met
                if (shouldGroup) {
                    group.add(inLinksList.get(j));
                    visited[j] = true;
                }
            }

            // Add the formed group to the result
            groups.add(group);
        }

        return groups;
    }

    // Bearing from fromNode to toNode (0° = East, 90° = North)
    private static double getBearing(Link link) {
        double dx = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
        double dy = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);
        return (angleDeg + 360) % 360;
    }

    // Smallest angular difference (0 to 90 degrees)
    // two links in opposite directions have 0 degree difference (same group)
    private static double angleDifference(double a, double b) {
        double diff = Math.abs(a - b) % 180;
        return diff > 90 ? 180 - diff : diff;
    }

    private static String getLinkName(Link link) {
        return (String) link.getAttributes().getAttribute("osm:way:name");
    }
}
