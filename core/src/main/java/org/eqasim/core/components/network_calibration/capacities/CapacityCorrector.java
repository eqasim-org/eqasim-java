package org.eqasim.core.components.network_calibration.capacities;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Applies one-time capacity corrections on startup when enabled in config.
 */
public class CapacityCorrector {
    private static final Logger logger = LogManager.getLogger(CapacityCorrector.class);

    @Inject
    public CapacityCorrector(Network network,
                             NetworkCalibrationConfigGroup config,
                             EqasimConfigGroup eqasimConfig,
                             LinkCategorizer categorizer) {
        if (!config.isActivated() || !config.getCorrectCapacities()) {
            return;
        }

        double sampleSize = Math.max(0.001, eqasimConfig.getSampleSize());
        adjustNetworkCapacities(
                network,
                config.getMinCapacity(),
                config.getMaxCapacity(),
                sampleSize,
                config.getCorrectCapacities(),
                config.getMinSpeed(),
                categorizer
        );
    }

    public static void adjustNetworkCapacities(Network network, double minimumCapacity, double maximumCapacity,
                                               double sampleSize, boolean correctForSampleSize, double minSpeed,
                                               LinkCategorizer categorizer) {
        logger.info("Adjusting network capacities with minCapacity={}, maxCapacity={}, sampleSize={}, correctForSampleSize={}, minSpeed={}",
                minimumCapacity, maximumCapacity, sampleSize, correctForSampleSize, minSpeed);

        int adjusted = 0;
        for (Link link : network.getLinks().values()) {
            if (categorizer.isOutsideLink(link)) {
                continue;
            }

            double capacity = link.getCapacity();
            double numberOfLanes = Math.max(1.0, link.getNumberOfLanes());
            double length = link.getLength();

            double maxCapacity = maximumCapacity * numberOfLanes;
            double minCapacity = minimumCapacity * numberOfLanes;
            if (correctForSampleSize) {
                minCapacity = Math.max(minCapacity,getMinimumSpeedBasedCapacity(length, minSpeed, sampleSize) * numberOfLanes);
            }

            if (capacity < minCapacity) {
                link.setCapacity(minCapacity);
                adjusted++;
            } else if (capacity > maxCapacity) {
                link.setCapacity(maxCapacity);
                adjusted++;
            }
        }

        logger.info("Adjusted capacities on {} links.", adjusted);
    }

    private static double getMinimumSpeedBasedCapacity(double lengthMeters, double minSpeedKmH, double sampleSize) {
        if (lengthMeters <= 0.0 || minSpeedKmH <= 0.0) {
            return 0.0;
        }

        return Math.min(3600.0 * (minSpeedKmH / 3.6) / (lengthMeters * sampleSize), 1800.0 * 3.0);
    }
}
