package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.List;

/**
 * Utility class for network calibration operations.
 */
public class NetworkCalibrationUtils {
    private static final Logger logger = LogManager.getLogger(NetworkCalibrationUtils.class);

    /**
     * Checks if a link is a ramp (contains "_link" in highway type).
     */
    public static boolean isRamp(Link link) {
        if (link.getAllowedModes() != null && link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            return osmHighway != null && osmHighway.toString().contains("_link");
        }
        return false;
    }

    /**
     * Checks if a link is a trunk road (contains "trunk" in highway type).
     */
    public static boolean isTrunk(Link link) {
        if (link.getAllowedModes() != null && link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            return osmHighway != null && osmHighway.toString().contains("trunk");
        }
        return false;
    }

    /**
     * Calculates the minimum speed-based capacity for a link.
     */
    public static double getMinimumSpeedBasedCapacity(double length, double minSpeed, double sampleSize) {
        return Math.min(3600.0 * (minSpeed / 3.6) / (length * sampleSize), 1800 * 3.0); // max 3 times the theoretical capacity
    }

    /**
     * Adjusts network capacities to be within specified bounds.
     */
    public static void adjustNetworkCapacities(Network network, double minimumCapacity, double maximumCapacity,
                                               double sampleSize, boolean correctForSampleSize, double minSpeed,
                                               LinkCategorizer categorizer) {
        logger.info("Adjusting network capacities with minCapacity={}, maxCapacity={}, sampleSize={}, correctForSampleSize={}, minSpeed={}",
                minimumCapacity, maximumCapacity, sampleSize, correctForSampleSize, minSpeed);

        for (Link link : network.getLinks().values()) {
            if (!categorizer.isOutsideLink(link)) {
                if (categorizer.getCategory(link)!=LinkCategorizer.UNKNOWN_CATEGORY) {
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
}
