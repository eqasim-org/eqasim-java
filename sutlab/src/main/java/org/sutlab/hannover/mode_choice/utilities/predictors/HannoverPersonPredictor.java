package org.sutlab.hannover.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.sutlab.hannover.mode_choice.utilities.variables.HannoverPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class HannoverPersonPredictor extends CachedVariablePredictor<HannoverPersonVariables> {
	@Override
	protected HannoverPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = HannoverPredictorUtils.hasSubscription(person);
		boolean hasDrivingPermit = HannoverPredictorUtils.hasDrivingLicense(person);
		return new HannoverPersonVariables(hasSubscription, hasDrivingPermit);
	}
}