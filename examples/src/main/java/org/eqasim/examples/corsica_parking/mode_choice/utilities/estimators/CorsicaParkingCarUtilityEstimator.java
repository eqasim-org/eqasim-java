package org.eqasim.examples.corsica_parking.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.corsica_parking.mode_choice.parameters.CorsicaParkingModeParameters;
import org.eqasim.examples.corsica_parking.mode_choice.utilities.predictors.CorsicaParkingCarPredictor;
import org.eqasim.examples.corsica_parking.mode_choice.utilities.variables.CorsicaParkingCarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class CorsicaParkingCarUtilityEstimator implements UtilityEstimator {
	private final CorsicaParkingModeParameters parameters;
	private final CorsicaParkingCarPredictor predictor;

	@Inject
	public CorsicaParkingCarUtilityEstimator(CorsicaParkingModeParameters parameters, CorsicaParkingCarPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double estimateTravelTimeUtility(CorsicaParkingCarVariables variables) {
		return parameters.car.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateParkingSearchTimeUtility(CorsicaParkingCarVariables variables) {
		return parameters.carParking.betaParkingSearchTime_u_min * variables.parkingSearchTime_min;
	}

	protected double estimateAccessEgressTimeUtility(CorsicaParkingCarVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(CorsicaParkingCarVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateParkingCostUtility(CorsicaParkingCarVariables variables) {
		return parameters.carParking.betaParkingCost_u_MU * variables.costParking_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		CorsicaParkingCarVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateParkingSearchTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateParkingCostUtility(variables);

		return utility;
	}
}
