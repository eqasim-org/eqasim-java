package org.eqasim.jakarta.mode_choice.utilities.predictors;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.jakarta.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaPersonPredictor extends CachedVariablePredictor<SaoPauloPersonVariables> {
	@Override
	protected SaoPauloPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		SaoPauloModeParameters parameters = SaoPauloModeParameters.buildDefault();
		boolean hasSubscription = JakartaPredictorUtils.hasSubscription(person);
		boolean cityTrip = JakartaPredictorUtils.startsEndsinCity(trip);
		double hhlIncome = JakartaPredictorUtils.hhlIncome(person, parameters);
		return new SaoPauloPersonVariables(hasSubscription, cityTrip ,hhlIncome);
	}
	
}
