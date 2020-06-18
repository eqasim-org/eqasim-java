package org.eqasim.jakarta.mode_choice.utilities.predictors;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.variables.JakartaPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class JakartaPersonPredictor extends CachedVariablePredictor<JakartaPersonVariables> {
	@Override
	protected JakartaPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		JakartaModeParameters parameters = JakartaModeParameters.buildDefault();
		boolean hasSubscription = JakartaPredictorUtils.hasSubscription(person);
		boolean cityTrip = JakartaPredictorUtils.startsEndsinCity(trip);
		double hhlIncome = JakartaPredictorUtils.hhlIncome(person, parameters);
		return new JakartaPersonVariables(hasSubscription, cityTrip ,hhlIncome);
	}
	
}
