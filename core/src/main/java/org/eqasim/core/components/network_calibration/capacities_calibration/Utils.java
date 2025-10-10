package org.eqasim.core.components.network_calibration.capacities_calibration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.List;

public class Utils {

    // Category 1 – Motorways / Freeways / Trunk / Expressways (highest capacity)
    public static final List<String> cat1 = List.of(
            "motorway",
            "motorway_link",
            "trunk",
            "trunk_link"
    );

    // Category 2 – primary
    public static final List<String> cat2 = List.of(
            "primary",
            "primary_link"
    );

    // Category 3 – Secondary
    public static final List<String> cat3 = List.of(
            "secondary",
            "secondary_link"
    );

    // Category 4 – Tertiary
    public static final List<String> cat4 = List.of(
            "tertiary",
            "tertiary_link"
    );

    // Category 5 – Local / Access Roads (lowest capacity)
    public static final List<String> cat5 = List.of(
            "residential",
            "unclassified",
            "living_street",
            "service",
            "track"
    );

    public static final int unknownCategory = 0;

    public static int osmToCategory(String osmHighway) {
        if (cat1.contains(osmHighway)) return 1;
        if (cat2.contains(osmHighway)) return 2;
        if (cat3.contains(osmHighway)) return 3;
        if (cat4.contains(osmHighway)) return 4;
        if (cat5.contains(osmHighway)) return 5;
        return unknownCategory; // unknown category
    }

    public static int getCategory(Link link) {
        if (link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            if (osmHighway != null) {
                return osmToCategory(osmHighway.toString());
            }
        }
        return unknownCategory;
    }

    public static boolean isRamp(Link link) {
        if (link.getAllowedModes() != null && link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            return osmHighway != null && osmHighway.toString().contains("_link");
        }
        return false;
    }

    public static double getNumberOfLanes(Link link) {
        Object lanesAttr = link.getAttributes().getAttribute("osm:way:lanes");
        try {
            return lanesAttr != null ? Double.parseDouble(lanesAttr.toString()) : link.getNumberOfLanes();
        } catch (NumberFormatException e) {
            return link.getNumberOfLanes();
        }
    }

    public static double minimumSpeedBasedCapacity(double length, double minSpeed, double sampleSize) {
        return 3600.0 * (minSpeed/3.6)/(length * sampleSize);
    }

}
