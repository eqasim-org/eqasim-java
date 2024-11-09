package org.eqasim.ile_de_france.policies.routing;

import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class PolicyTravelDisutilityFactory implements TravelDisutilityFactory {
	private final OnlyTimeDependentTravelDisutilityFactory delegate = new OnlyTimeDependentTravelDisutilityFactory();
	private final RoutingPenalty linkPenalty;

	public PolicyTravelDisutilityFactory(RoutingPenalty linkPenalty) {
		this.linkPenalty = linkPenalty;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime travelTime) {
		return new PolicyTravelDisutility(delegate.createTravelDisutility(travelTime), linkPenalty);
	}
}
