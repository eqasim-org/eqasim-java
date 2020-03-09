package org.eqasim.wayne_county.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyPersonPredictor extends CachedVariablePredictor<WayneCountyPersonVariables> {
	@Override
	protected WayneCountyPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		int hhlIncomeClass = WayneCountyPredictorUtils.hhlIncomeClass(person);
		return new WayneCountyPersonVariables(hhlIncomeClass);
	}
}
