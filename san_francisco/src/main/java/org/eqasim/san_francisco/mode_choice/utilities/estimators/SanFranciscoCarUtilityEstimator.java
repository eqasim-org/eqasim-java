package org.eqasim.san_francisco.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoCarPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SanFranciscoCarUtilityEstimator implements UtilityEstimator {
	private final SanFranciscoModeParameters parameters;
	private final SanFranciscoPersonPredictor predictor;
	private final SanFranciscoCarPredictor carPredictor;

	@Inject
	public SanFranciscoCarUtilityEstimator(SanFranciscoModeParameters parameters, PersonPredictor personPredictor,
			SanFranciscoCarPredictor carPredictor, SanFranciscoPersonPredictor predictor) {
		this.carPredictor = carPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	private double estimateConstantUtility() {
		return parameters.car.alpha_u;
	}

	protected double estimateTravelTimeUtility(CarVariables variables_car) {
		return parameters.sfCar.vot_min * variables_car.travelTime_min;
	}

	protected double estimateAccessEgressUtility(CarVariables variables_car) {
		return parameters.sfCar.vot_accessegress_min * variables_car.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(CarVariables variables) {
		return EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
				parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += (estimateTravelTimeUtility(variables_car) + estimateAccessEgressUtility(variables_car)
				+ estimateMonetaryCostUtility(variables_car))
				* (parameters.sfAvgHHLIncome.avg_hhl_income / variables.hhlIncome) * parameters.betaCost_u_MU;

		return utility;
	}

}
