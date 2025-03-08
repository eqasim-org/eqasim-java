package org.eqasim.core.simulation.policies.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public interface RoutingPenalty {
	double getLinkPenalty(Link link, Person person, double time, double baseDisutility);
}
