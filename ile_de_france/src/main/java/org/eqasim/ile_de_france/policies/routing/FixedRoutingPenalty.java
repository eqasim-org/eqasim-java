package org.eqasim.ile_de_france.policies.routing;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class FixedRoutingPenalty implements RoutingPenalty {
	private final IdSet<Link> linkIds;
	private final double penalty;

	public FixedRoutingPenalty(IdSet<Link> linkIds, double penalty) {
		this.linkIds = linkIds;
		this.penalty = penalty;
	}

	@Override
	public double getLinkPenalty(Link link, Person person, double time) {
		return linkIds.contains(link.getId()) ? penalty : 0.0;
	}
}
