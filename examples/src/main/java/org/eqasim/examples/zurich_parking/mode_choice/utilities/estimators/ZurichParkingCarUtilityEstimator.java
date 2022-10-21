package org.eqasim.examples.zurich_parking.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.zurich_parking.mode_choice.parameters.ZurichParkingModeParameters;
import org.eqasim.examples.zurich_parking.mode_choice.utilities.predictors.ZurichParkingCarPredictor;
import org.eqasim.examples.zurich_parking.mode_choice.utilities.variables.ZurichParkingCarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class ZurichParkingCarUtilityEstimator implements UtilityEstimator {
	private final ZurichParkingModeParameters parameters;
	private final ZurichParkingCarPredictor predictor;

	@Inject
	public ZurichParkingCarUtilityEstimator(ZurichParkingModeParameters parameters, ZurichParkingCarPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double estimateTravelTimeUtility(ZurichParkingCarVariables variables) {
		return parameters.car.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateParkingSearchTimeUtility(ZurichParkingCarVariables variables) {
		return parameters.carParking.betaParkingSearchTime_u_min * variables.parkingSearchTime_min;
	}

	protected double estimateAccessEgressTimeUtility(ZurichParkingCarVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(ZurichParkingCarVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateParkingCostUtility(ZurichParkingCarVariables variables) {
		return parameters.carParking.betaParkingCost_u_MU * variables.costParking_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ZurichParkingCarVariables variables = predictor.predictVariables(person, trip, elements);

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
