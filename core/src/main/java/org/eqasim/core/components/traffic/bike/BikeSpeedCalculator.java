package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface BikeSpeedCalculator {
    default double getMaximumBikeVelocity(Vehicle vehicle, Link link, double time){
        return 4.2; // default maximum bike speed in m/s (approximately 15 km/h)
    }
}
