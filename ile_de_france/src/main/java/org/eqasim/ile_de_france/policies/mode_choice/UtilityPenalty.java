package org.eqasim.ile_de_france.policies.mode_choice;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public interface UtilityPenalty {
	double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements);
}
