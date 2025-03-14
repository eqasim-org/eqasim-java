package org.eqasim.bavaria.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.bavaria.mode_choice.utilities.variables.BavariaPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class BavariaPersonPredictor extends CachedVariablePredictor<BavariaPersonVariables> {
	@Override
	protected BavariaPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = BavariaPredictorUtils.hasSubscription(person);
		boolean hasDrivingPermit = BavariaPredictorUtils.hasDrivingLicense(person);
		boolean isParisResident = BavariaPredictorUtils.isParisResident(person);
		return new BavariaPersonVariables(hasSubscription, hasDrivingPermit, isParisResident);
	}
}
