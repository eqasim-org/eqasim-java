package org.eqasim.core.components.network_calibration.capacities_calibration;

import org.matsim.api.core.v01.network.Link;

import java.util.List;

public class NetworkCalibrationUtils {

    // Category 1 – Motorways / Freeways / Trunk / Expressways (highest capacity)
    public static final List<String> CATEGORY_1_HIGHWAY_TYPES = List.of(
            "motorway",
            "motorway_link",
            "trunk",
            "trunk_link"
    );

    // Category 2 – primary
    public static final List<String> CATEGORY_2_HIGHWAY_TYPES = List.of(
            "primary",
            "primary_link"
    );

    // Category 3 – Secondary
    public static final List<String> CATEGORY_3_HIGHWAY_TYPES = List.of(
            "secondary",
            "secondary_link"
    );

    // Category 4 – Tertiary
    public static final List<String> CATEGORY_4_HIGHWAY_TYPES = List.of(
            "tertiary",
            "tertiary_link"
    );

    // Category 5 – Local / Access Roads (lowest capacity)
    public static final List<String> CATEGORY_5_HIGHWAY_TYPES = List.of(
            "residential",
            "unclassified",
            "living_street",
            "service",
            "track"
    );

    public static final int UNKNOWN_CATEGORY = 0;

    public static int getCategoryFromOsmHighway(String osmHighway) {
        if (CATEGORY_1_HIGHWAY_TYPES.contains(osmHighway)) return 1;
        if (CATEGORY_2_HIGHWAY_TYPES.contains(osmHighway)) return 2;
        if (CATEGORY_3_HIGHWAY_TYPES.contains(osmHighway)) return 3;
        if (CATEGORY_4_HIGHWAY_TYPES.contains(osmHighway)) return 4;
        if (CATEGORY_5_HIGHWAY_TYPES.contains(osmHighway)) return 5;
        return UNKNOWN_CATEGORY; // unknown category
    }

    public static int getCategory(Link link) {
        if (link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            if (osmHighway != null) {
                return getCategoryFromOsmHighway(osmHighway.toString());
            }
        }
        return UNKNOWN_CATEGORY;
    }

    public static boolean isRamp(Link link) {
        if (link.getAllowedModes() != null && link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            return osmHighway != null && osmHighway.toString().contains("_link");
        }
        return false;
    }

    public static double getMinimumSpeedBasedCapacity(double length, double minSpeed, double sampleSize) {
        return 3600.0 * (minSpeed/3.6)/(length * sampleSize);
    }

}
