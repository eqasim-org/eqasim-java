package org.eqasim.core.simulation.mode_choice.cost;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public interface CostModel {
	double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements);
}
