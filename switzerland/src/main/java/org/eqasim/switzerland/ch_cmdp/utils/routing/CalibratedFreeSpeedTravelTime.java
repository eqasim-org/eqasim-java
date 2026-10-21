package org.eqasim.switzerland.ch_cmdp.utils.routing;

import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/** Reads persisted road calibration once, without changing bike/PT network speeds. */
final class CalibratedFreeSpeedTravelTime implements TravelTime {
    private final double[] factors;

    CalibratedFreeSpeedTravelTime(Network network) {
        int size = network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElse(-1) + 1;
        factors = new double[size];
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car") && !link.getAllowedModes().contains("truck")) continue;
            double factor = NetworkCalibrationUtils.readDoubleAttribute(link,
                    NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElse(1);
            // FreespeedAdapter ignores nonpositive/malformed factors when loading calibration.
            factors[link.getId().index()] = factor > 0 ? factor : 1;
        }
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        // Match FreespeedAdapter.applyFactors, including its minimum physical speed.
        return link.getLength() / Math.max(0.1, link.getFreespeed(time) * factors[link.getId().index()]);
    }
}
