package org.eqasim.switzerland.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SwissCarUtilityEstimator extends CarUtilityEstimator {
	private final SwissModeParameters parameters;
	private final SwissPersonPredictor predictor;

	@Inject
	public SwissCarUtilityEstimator(SwissModeParameters parameters, CarPredictor carPredictor,
			SwissPersonPredictor personPredictor) {
		super(parameters, carPredictor);

		this.predictor = personPredictor;
		this.parameters = parameters;
	}

	protected double estimateRegionalUtility(SwissPersonVariables variables) {
		if (variables.statedPreferenceRegion == 1) {
			return parameters.swissCar.betaStatedPreferenceRegion1_u;
		} else if (variables.statedPreferenceRegion == 3) {
			return parameters.swissCar.betaStatedPreferenceRegion3_u;
		} else {
			return 0.0;
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
