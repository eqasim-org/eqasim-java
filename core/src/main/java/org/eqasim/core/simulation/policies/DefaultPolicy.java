package org.eqasim.core.simulation.policies;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;

public class DefaultPolicy implements Policy {
	private final RoutingPenalty routingPenalty;
	private final UtilityPenalty utilityPenalty;

	public DefaultPolicy(RoutingPenalty routingPenalty, UtilityPenalty utilityPenalty) {
		this.routingPenalty = routingPenalty;
		this.utilityPenalty = utilityPenalty;
	}

	@Override
	public RoutingPenalty getRoutingPenalty() {
		return routingPenalty;
	}

	@Override
	public UtilityPenalty getUtilityPenalty() {
		return utilityPenalty;
	}

}
