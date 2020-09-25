package org.eqasim.sao_paulo.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloPersonPredictor extends CachedVariablePredictor<SaoPauloPersonVariables> {
	@Override
	protected SaoPauloPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		SaoPauloModeParameters parameters = SaoPauloModeParameters.buildDefault();
		boolean hasSubscription = SaoPauloPredictorUtils.hasSubscription(person);
		boolean cityTrip = SaoPauloPredictorUtils.startsEndsinCity(trip);
		double hhlIncome = SaoPauloPredictorUtils.hhlIncome(person, parameters);
		return new SaoPauloPersonVariables(hasSubscription, cityTrip ,hhlIncome);
	}
	
}
