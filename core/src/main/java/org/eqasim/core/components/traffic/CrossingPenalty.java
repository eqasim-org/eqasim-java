package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface CrossingPenalty {
    double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId);

    /** Read-only routing estimate, independent of a vehicle's simulated crossing history. */
    default double estimateCrossingPenalty(Link link, double time) {
        // A null ID bypasses history lookup and recording in IntersectionDelay.
        return calculateCrossingPenalty(link, time, null);
    }

    default double calculateCrossingPenalty(Link link) {
        // Default implementation assumes no time dependency
        return calculateCrossingPenalty(link, 0.0, null);
    }

    default double calculateCrossingPenalty(Link link, double time) {
        // Default implementation assumes no time dependency
        return calculateCrossingPenalty(link, time, null);
    }
}
