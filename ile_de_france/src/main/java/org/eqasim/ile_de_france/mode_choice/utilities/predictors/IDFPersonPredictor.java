package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFPersonPredictor extends CachedVariablePredictor<IDFPersonVariables> {
	@Override
	protected IDFPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = IDFPredictorUtils.hasSubscription(person);
		return new IDFPersonVariables(hasSubscription);
	}
}
