package org.eqasim.core.components.travel_disutility;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class EqasimTravelDisutility implements TravelDisutility {
	private final TravelDisutility delegate;
	private final RoutingPenalty penalty;
	private final double routingDistanceUtility;

	public EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty, double routingDistanceUtility) {
		this.delegate = delegate;
		this.penalty = penalty;
		this.routingDistanceUtility = routingDistanceUtility;
	}

	public EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty) {
		this(delegate, penalty, 0.0);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double disutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
		disutility += penalty.getLinkPenalty(link, person, time, disutility);
		disutility += routingDistanceUtility * (link.getLength() / 10.0); // 10.0 is for the scale, to be in the same order as travel time
		return disutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return delegate.getLinkMinimumTravelDisutility(link);
	}
}
