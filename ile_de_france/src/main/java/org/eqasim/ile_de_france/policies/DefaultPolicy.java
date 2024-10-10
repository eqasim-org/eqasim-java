package org.eqasim.ile_de_france.policies;

import org.eqasim.ile_de_france.policies.mode_choice.UtilityPenalty;
import org.eqasim.ile_de_france.policies.routing.RoutingPenalty;

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
