package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.network.Link;

public interface CrossingPenalty {
	double calculateCrossingPenalty(Link link);
}
