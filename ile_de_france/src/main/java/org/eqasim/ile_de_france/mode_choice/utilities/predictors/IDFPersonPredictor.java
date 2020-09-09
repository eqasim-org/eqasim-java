package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class IDFPersonPredictor extends CachedVariablePredictor<IDFPersonVariables> {
	@Override
	protected IDFPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = IDFPredictorUtils.hasSubscription(person);
		boolean hasLicense = IDFPredictorUtils.hasLicense(person);
		boolean householdCarAvailability = IDFPredictorUtils.getHouseholdCarAvailability(person);
		boolean householdBikeAvailability = IDFPredictorUtils.getHouseholdBikeAvailability(person);

		return new IDFPersonVariables(hasSubscription, hasLicense, householdCarAvailability, householdBikeAvailability);
	}
}
