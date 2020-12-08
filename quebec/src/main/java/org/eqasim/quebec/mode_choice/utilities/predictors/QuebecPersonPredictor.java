package org.eqasim.quebec.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.quebec.mode_choice.utilities.variables.QuebecPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class QuebecPersonPredictor extends CachedVariablePredictor<QuebecPersonVariables> {
	@Override
	protected QuebecPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = QuebecPredictorUtils.hasSubscription(person);
	
		return new QuebecPersonVariables(hasSubscription);
	}
}
