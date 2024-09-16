package org.eqasim.ile_de_france.policies.routing;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class SumRoutingPenalty implements RoutingPenalty {
	private final List<RoutingPenalty> items = new ArrayList<>();

	public SumRoutingPenalty(List<RoutingPenalty> items) {
		this.items.addAll(items);
	}

	@Override
	public double getLinkPenalty(Link link, Person person, double time, double baseDisutility) {
		double penalty = 0.0;

		for (RoutingPenalty item : items) {
			penalty += item.getLinkPenalty(link, person, time, baseDisutility);
		}

		return penalty;
	}
}
