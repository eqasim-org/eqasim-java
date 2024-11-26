package org.eqasim.ile_de_france.policies.mode_choice;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SumUtilityPenalty implements UtilityPenalty {
	private final List<UtilityPenalty> items = new ArrayList<>();

	public SumUtilityPenalty(List<UtilityPenalty> items) {
		this.items.addAll(items);
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double penalty = 0.0;

		for (UtilityPenalty item : items) {
			penalty += item.calculatePenalty(mode, person, trip, elements);
		}

		return penalty;
	}
}
