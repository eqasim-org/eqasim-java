package org.eqasim.core.simulation.policies;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;

public interface Policy {
	RoutingPenalty getRoutingPenalty();

	UtilityPenalty getUtilityPenalty();
}
