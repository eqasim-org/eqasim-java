package org.eqasim.core.simulation.policies.utility;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class SumPenalty implements UtilityPenalty {
	private final List<UtilityPenalty> items = new ArrayList<>();

	public SumPenalty(List<UtilityPenalty> items) {
		this.items.addAll(items);
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		double penalty = 0.0;

		for (UtilityPenalty item : items) {
			penalty += item.calculatePenalty(mode, person, trip, previousTrips, elements);
		}

		return penalty;
	}
}
