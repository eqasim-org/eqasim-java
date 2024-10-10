package org.eqasim.ile_de_france.policies;

import org.eqasim.ile_de_france.policies.mode_choice.UtilityPenalty;
import org.eqasim.ile_de_france.policies.routing.RoutingPenalty;

public interface Policy {
	RoutingPenalty getRoutingPenalty();

	UtilityPenalty getUtilityPenalty();
}
