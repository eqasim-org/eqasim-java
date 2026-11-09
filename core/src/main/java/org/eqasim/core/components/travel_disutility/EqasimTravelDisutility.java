package org.eqasim.core.components.travel_disutility;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.tools.random.Normal;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class EqasimTravelDisutility implements TravelDisutility {
	private final TravelDisutility delegate;
	private final RoutingPenalty penalty;
	private final double routingDistanceUtility;
	private final double sigma;
	private final boolean useRandomness;
	private final long baseSeed;
	private final long[] linkIdHashes;

	public EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty, double routingDistanceUtility, double sigma, long baseSeed) {
		this(delegate, penalty, routingDistanceUtility, sigma, baseSeed, null);
	}

	EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty,
			double routingDistanceUtility, double sigma, long baseSeed, long[] linkIdHashes) {
		this.linkIdHashes = linkIdHashes;
		this.delegate = delegate;
		this.penalty = penalty;
		this.routingDistanceUtility = routingDistanceUtility;
		this.sigma = sigma;
		this.useRandomness = sigma>1e-6;
		this.baseSeed = baseSeed;
	}

	public EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty, double routingDistanceUtility) {
		this(delegate, penalty, routingDistanceUtility, 0.0, 1997);
	}

	public EqasimTravelDisutility(TravelDisutility delegate, RoutingPenalty penalty) {
		this(delegate, penalty, 0.0, 0.0, 1997);
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double disutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
		disutility += penalty.getLinkPenalty(link, person, time, disutility);
		disutility += routingDistanceUtility * (link.getLength() / 10.0); // 10.0 is for the scale, to be in the same order as travel time
		if (useRandomness){
			// New IDs created after the factory snapshot use the same exact
			// hash path without caching; no network or person state is retained.
			int index = link.getId().index();
			double draw = linkIdHashes != null && index < linkIdHashes.length
					? Normal.getWithPrefix(linkIdHashes[index], person.getId().toString(), baseSeed)
					: Normal.get(link.getId().toString(), person.getId().toString(), baseSeed);
			double normalNoise = sigma * draw;
			disutility *= Math.max(1.0 + normalNoise, 1e-3); // we need to keep it positive, otherwise it doesn't make sense
		}
		return disutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return delegate.getLinkMinimumTravelDisutility(link)
				+ routingDistanceUtility * (link.getLength() / 10.0);
	}
}
