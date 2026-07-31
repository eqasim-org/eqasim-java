package org.eqasim.core.components.travel_disutility;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class EqasimTravelDisutilityFactory implements TravelDisutilityFactory {
	private final OnlyTimeDependentTravelDisutilityFactory delegate = new OnlyTimeDependentTravelDisutilityFactory();
	private final RoutingPenalty linkPenalty;
	private final double routingDistanceUtility;
	private final double sigma;

	public EqasimTravelDisutilityFactory(RoutingPenalty linkPenalty, double routingDistanceUtility) {
		this(linkPenalty, routingDistanceUtility, 0.0);
	}

	public EqasimTravelDisutilityFactory(RoutingPenalty linkPenalty, double routingDistanceUtility, double sigma) {
		this.linkPenalty = linkPenalty;
		this.routingDistanceUtility = routingDistanceUtility;
		this.sigma = sigma;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime travelTime) {
		return new EqasimTravelDisutility(delegate.createTravelDisutility(travelTime), linkPenalty, routingDistanceUtility, sigma);
	}
}
