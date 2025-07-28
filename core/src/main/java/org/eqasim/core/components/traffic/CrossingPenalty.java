package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface CrossingPenalty {
    double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId);

    default double calculateCrossingPenalty(Link link) {
        // Default implementation assumes no time dependency
        return calculateCrossingPenalty(link, 0.0, null);
    }
}
