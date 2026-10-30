package org.eqasim.core.components.travel_disutility;

import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.tools.random.Normal;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class EqasimTravelDisutilityFactory implements TravelDisutilityFactory {
	private final OnlyTimeDependentTravelDisutilityFactory delegate = new OnlyTimeDependentTravelDisutilityFactory();
	private final RoutingPenalty linkPenalty;
	private final double routingDistanceUtility;
	private double sigma;
	private final long baseSeed;
	private long[] linkIdHashes;

	private synchronized long[] getLinkIdHashes() {
		if (linkIdHashes == null) {
			// Typed MATSim IDs are immutable and share their index across network
			// copies. Store only eight bytes per registered link ID, once per
			// factory, and share this read-only array with all routing workers.
			long[] hashes = new long[Id.getNumberOfIds(Link.class)];
			for (int index = 0; index < hashes.length; index++) {
				hashes[index] = Normal.hashPrefix(Id.get(index, Link.class).toString());
			}
			linkIdHashes = hashes;
		}
		return linkIdHashes;
	}

	public EqasimTravelDisutilityFactory(RoutingPenalty linkPenalty, double routingDistanceUtility) {
		this(linkPenalty, routingDistanceUtility, 0.0, 1997);
	}

	public EqasimTravelDisutilityFactory(RoutingPenalty linkPenalty, double routingDistanceUtility, double sigma, long baseSeed) {
		this.linkPenalty = linkPenalty;
		this.routingDistanceUtility = routingDistanceUtility;
		this.sigma = sigma;
		this.baseSeed = baseSeed;
	}

	public void setSigmaNoise(double value) {
		this.sigma = value;
	}

	public double getSigmaNoise() {
		return sigma;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime travelTime) {
		return new EqasimTravelDisutility(delegate.createTravelDisutility(travelTime), linkPenalty, routingDistanceUtility, sigma, baseSeed,
				sigma > 1e-6 ? getLinkIdHashes() : null);
	}
}
