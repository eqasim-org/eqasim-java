package org.eqasim.examples.corsica_carpooling.mode_choice.utilities;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.corsica_carpooling.mode_choice.parameters.CarpoolingModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class CarpoolingUtilityEstimator implements UtilityEstimator {
	private final CarpoolingModeParameters parameters;
	private final CarpoolingPredictor predictor;

	@Inject
	public CarpoolingUtilityEstimator(CarpoolingModeParameters parameters, CarpoolingPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.carpooling.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarpoolingVariables variables) {
		return parameters.carpooling.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateMonetaryCostUtility(CarpoolingVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarpoolingVariables variables = predictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);

		return utility;
	}
}
