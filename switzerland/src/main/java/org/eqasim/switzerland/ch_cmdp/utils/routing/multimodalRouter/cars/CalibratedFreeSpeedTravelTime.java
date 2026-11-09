package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.cars;

import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/** Applies persisted speed calibration once to the owned road network, then uses ordinary freespeed. */
public final class CalibratedFreeSpeedTravelTime implements TravelTime {
    private static final String APPLIED = "multimodalRouter:roadSpeedsCalibrated";

    public CalibratedFreeSpeedTravelTime(Network network) {
        if (Boolean.TRUE.equals(network.getAttributes().getAttribute(APPLIED))) return;
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car") && !link.getAllowedModes().contains("truck")) continue;
            double factor = NetworkCalibrationUtils.readDoubleAttribute(link,
                    NetworkCalibrationUtils.SPEED_FACTOR_ATTRIBUTE).orElse(1);
            // Match FreespeedAdapter: malformed/nonpositive factors fall back to one.
            link.setFreespeed(Math.max(0.1, link.getFreespeed() * (factor > 0 ? factor : 1)));
        }
        network.getAttributes().putAttribute(APPLIED, true);
    }

    @Override
    public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return link.getLength() / link.getFreespeed(time);
    }
}
