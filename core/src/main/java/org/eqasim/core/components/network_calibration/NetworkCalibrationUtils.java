package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.List;

public class NetworkCalibrationUtils {
    private static final Logger logger = LogManager.getLogger(NetworkCalibrationUtils.class);
    private static boolean separateUrban = false;

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

    public static List<Integer> getAllCategories() {
        if (separateUrban) {
            return List.of(1, 2, 3, 4, 5, 11, 12, 13, 14, 15);
        } else {
            return List.of(1, 2, 3, 4, 5);
        }
    }

    public static int getCategoryFromOsmHighway(String osmHighway, Link link) {
        int baseCategory;
        if (CATEGORY_1_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 1;
        } else if (CATEGORY_2_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 2;
        } else if (CATEGORY_3_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 3;
        } else if (CATEGORY_4_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 4;
        } else if (CATEGORY_5_HIGHWAY_TYPES.contains(osmHighway)) {
            if (link.getNumberOfLanes() > 1 || link.getFreespeed() > 45 / 3.6) {
                baseCategory = 4; // treat as tertiary if more than 1 lane or freespeed > 45 km/h
            } else {
                baseCategory = 5;
            }
        } else {
            return UNKNOWN_CATEGORY; // unknown category
        }

    if (separateUrban && isUrbanLink(link)) {
        return baseCategory + 10; // Urban categories are 11-15
    }
    return baseCategory;
}

    public static void setSeparateUrban(boolean separateUrbanFlag) {
        logger.info("setSeparateUrban flag: {}", separateUrbanFlag);
        separateUrban = separateUrbanFlag;
    }

    public static boolean isUrbanLink(Link link) {
        Object municipalityTypeObj = link.getAttributes().getAttribute("municipalityType");
        if (municipalityTypeObj instanceof String municipalityType) {
            return municipalityType.equalsIgnoreCase("urban") || municipalityType.equalsIgnoreCase("urbancore");
        }
        return false;
    }

    public static int getCategory(Link link) {
        if (link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            if (osmHighway != null) {
                return getCategoryFromOsmHighway(osmHighway.toString(), link);
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

    public static boolean isTrunk(Link link) {
        if (link.getAllowedModes() != null && link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            return osmHighway != null && osmHighway.toString().contains("trunk");
        }
        return false;
    }

    public static double getMinimumSpeedBasedCapacity(double length, double minSpeed, double sampleSize) {
        return Math.min(3600.0 * (minSpeed/3.6)/(length * sampleSize), 1800 * 3.0); // max 3 times the theoretical capacity
    }

    public static void adjustNetworkCapacities(Network network, double minimumCapacity, double maximumCapacity,
                                               double sampleSize, boolean correctForSampleSize, double minSpeed) {
        logger.info("Adjusting network capacities with minCapacity={}, maxCapacity={}, sampleSize={}, correctForSampleSize={}, minSpeed={}",
                minimumCapacity, maximumCapacity, sampleSize, correctForSampleSize, minSpeed);

        List<Integer> linkCategories = List.of(1,2,3,4,5);
        for (Link link : network.getLinks().values()) {
            int category = getCategory(link);
            if (linkCategories.contains(category)) {
                double capacity = link.getCapacity();
                double numberOfLanes = link.getNumberOfLanes();
                double length = link.getLength();
                // get the min and max capacities for that link
                double minCapacity = correctForSampleSize ?
                        getMinimumSpeedBasedCapacity(length, minSpeed, sampleSize) * numberOfLanes :
                        minimumCapacity * numberOfLanes;
                double maxCapacity = maximumCapacity * numberOfLanes;
                // adjust the capacity if needed
                if (capacity < minCapacity) {
                    link.setCapacity(minCapacity);
                } else if (capacity > maxCapacity) {
                    link.setCapacity(maxCapacity);
                }
            }
        }
    }

}
