package org.sutlab.seville.mode_choice.utilities.predictors;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville.mode_choice.utilities.variables.SevillePersonVariables;

import java.util.List;

public class SevillePersonPredictor extends CachedVariablePredictor<SevillePersonVariables> {
	@Override
	protected SevillePersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
											 List<? extends PlanElement> elements) {
		boolean hasSubscription = SevillePredictorUtils.hasSubscription(person);
		boolean hasDrivingPermit = SevillePredictorUtils.hasDrivingLicense(person);
		return new SevillePersonVariables(hasSubscription, hasDrivingPermit);
	}
}