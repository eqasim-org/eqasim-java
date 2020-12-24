package org.eqasim.los_angeles.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class LosAngelesPersonPredictor extends CachedVariablePredictor<LosAngelesPersonVariables> {
	@Override
	protected LosAngelesPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean hasSubscription = LosAngelesPredictorUtils.hasSubscription(person);
		boolean cityTrip = LosAngelesPredictorUtils.startsEndsinCity(trip);
		boolean orangeTrip =LosAngelesPredictorUtils.startsEndsinOrangeCounty(trip);
		double hhlIncome = LosAngelesPredictorUtils.hhlIncome(person);
		return new LosAngelesPersonVariables(hasSubscription, cityTrip, orangeTrip, hhlIncome);
	}
}
