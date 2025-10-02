package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFBicycleUtilityEstimator extends BikeUtilityEstimator {
	@Inject
	public IDFBicycleUtilityEstimator(IDFModeParameters parameters, PersonPredictor personPredictor,
			BikePredictor predictor) {
		super(parameters, personPredictor, predictor);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);

		return utility;
	}
}
