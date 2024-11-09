package org.eqasim.ile_de_france.policies.routing;

import org.eqasim.ile_de_france.policies.PolicyPersonFilter;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class FixedRoutingPenalty implements RoutingPenalty {
	private final IdSet<Link> linkIds;
	private final double penalty;
	private final PolicyPersonFilter personFilter;

	public FixedRoutingPenalty(IdSet<Link> linkIds, double penalty, PolicyPersonFilter personFilter) {
		this.linkIds = linkIds;
		this.penalty = penalty;
		this.personFilter = personFilter;
	}

	@Override
	public double getLinkPenalty(Link link, Person person, double time, double baseDisutility) {
		return linkIds.contains(link.getId()) && personFilter.applies(person.getId()) ? penalty : 0.0;
	}
}
