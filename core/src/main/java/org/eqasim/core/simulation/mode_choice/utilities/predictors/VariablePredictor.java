package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public interface VariablePredictor<T extends BaseVariables> {
	T predictVariables(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements);
}
