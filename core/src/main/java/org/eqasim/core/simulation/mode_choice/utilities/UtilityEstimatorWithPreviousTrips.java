package org.eqasim.core.simulation.mode_choice.utilities;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public interface UtilityEstimatorWithPreviousTrips extends UtilityEstimator {
	double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements,
			List<TripCandidate> previousTrips);
}
