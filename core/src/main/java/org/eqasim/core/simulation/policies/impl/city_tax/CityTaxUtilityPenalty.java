package org.eqasim.core.simulation.policies.impl.city_tax;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.routes.NetworkRoute;

public class CityTaxUtilityPenalty implements UtilityPenalty {
	private final ModeParameters parameters;
	private final IdSet<Link> taxedLinkIds;
	private final double enterTax_EUR;
	private final PolicyPersonFilter personFilter;

	public CityTaxUtilityPenalty(IdSet<Link> taxedLinkIds, ModeParameters parameters, double enterTax_EUR,
			PolicyPersonFilter personFilter) {
		this.taxedLinkIds = taxedLinkIds;
		this.parameters = parameters;
		this.enterTax_EUR = enterTax_EUR;
		this.personFilter = personFilter;
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		if (mode.equals(TransportMode.car) && personFilter.applies(person.getId())) {
			return -parameters.betaCost_u_MU * estimateTax_EUR(elements);
		} else {
			return 0.0;
		}
	}

	private double estimateTax_EUR(List<? extends PlanElement> elements) {
		double routeTax_EUR = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg) {
				if (leg.getRoute() instanceof NetworkRoute route) {
					for (Id<Link> linkId : route.getLinkIds()) {
						if (taxedLinkIds.contains(linkId)) {
							routeTax_EUR += enterTax_EUR;
						}
					}
				}
			}
		}

		return routeTax_EUR;
	}
}
