package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class CarUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final CarPredictor predictor;

	@Inject
	public CarUtilityEstimator(ModeParameters parameters, CarPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarVariables variables) {
		return parameters.car.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(CarVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(CarVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CarVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);

		return utility;
	}
}
