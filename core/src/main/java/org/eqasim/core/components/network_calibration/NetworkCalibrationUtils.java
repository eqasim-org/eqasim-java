package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import java.util.OptionalDouble;

/**
 * Utility class for network calibration operations.
 */
public class NetworkCalibrationUtils {
    private static final Logger logger = LogManager.getLogger(NetworkCalibrationUtils.class);
    public static final String SPEED_FACTOR_ATTRIBUTE = "speedFactor";
    public static final String PENALTY_ATTRIBUTE = "penalty";

    public static OptionalDouble readDoubleAttribute(Link link, String attributeName) {
        Object rawValue = link.getAttributes().getAttribute(attributeName);
        if (rawValue == null) {
            return OptionalDouble.empty();
        }

        if (rawValue instanceof Number number) {
            double value = number.doubleValue();
            return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
        }

        if (rawValue instanceof String text) {
            try {
                double value = Double.parseDouble(text.trim());
                return Double.isFinite(value) ? OptionalDouble.of(value) : OptionalDouble.empty();
            } catch (NumberFormatException ignored) {
                return OptionalDouble.empty();
            }
        }

        return OptionalDouble.empty();
    }

    public static void writeDoubleAttribute(Link link, String attributeName, double value) {
        if (!Double.isFinite(value)) {
            logger.warn("Skipping non-finite value for attribute {} on link {}", attributeName, link.getId());
            return;
        }
        link.getAttributes().putAttribute(attributeName, value);
    }

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
}
