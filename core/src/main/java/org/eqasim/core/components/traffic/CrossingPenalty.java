package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.network.Link;

public interface CrossingPenalty {
    double calculateCrossingPenalty(Link link, double time);

    default double calculateCrossingPenalty(Link link) {
        // Default implementation assumes no time dependency
        return calculateCrossingPenalty(link, 0.0);
    }
}
