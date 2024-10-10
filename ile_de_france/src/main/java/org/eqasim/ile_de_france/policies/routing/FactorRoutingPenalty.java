package org.eqasim.ile_de_france.policies.routing;

import org.eqasim.ile_de_france.policies.PolicyPersonFilter;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class FactorRoutingPenalty implements RoutingPenalty {
	private final IdSet<Link> linkIds;
	private final PolicyPersonFilter personFilter;
	private final double factor;

	public FactorRoutingPenalty(IdSet<Link> linkIds, double factor, PolicyPersonFilter personFilter) {
		this.linkIds = linkIds;
		this.factor = factor;
		this.personFilter = personFilter;
	}

	@Override
	public double getLinkPenalty(Link link, Person person, double time, double baseDisutility) {
		return linkIds.contains(link.getId()) && personFilter.applies(person.getId()) ? baseDisutility * factor : 0.0;
	}
}
