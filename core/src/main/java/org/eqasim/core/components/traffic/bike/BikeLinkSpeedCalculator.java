package org.eqasim.core.components.traffic.bike;

import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface BikeLinkSpeedCalculator extends EqasimLinkSpeedCalculator {
    double maximumBikeSpeed(Vehicle vehicle, Link link, double time);
}
