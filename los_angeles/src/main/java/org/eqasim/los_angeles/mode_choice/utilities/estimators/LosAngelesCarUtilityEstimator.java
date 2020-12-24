package org.eqasim.los_angeles.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesModeParameters;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class LosAngelesCarUtilityEstimator extends CarUtilityEstimator {
	private final LosAngelesModeParameters parameters;
	private final LosAngelesPersonPredictor predictor;
	private final CarPredictor carPredictor;

	@Inject
	public LosAngelesCarUtilityEstimator(LosAngelesModeParameters parameters, PersonPredictor personPredictor,
			CarPredictor carPredictor, LosAngelesPersonPredictor predictor) {
		super(parameters, carPredictor);
		this.carPredictor = carPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateTravelTime(CarVariables variables_car) {
		return parameters.laCar.vot_min * variables_car.travelTime_min;
	}

	@Override
	protected double estimateMonetaryCostUtility(CarVariables variables) {
		return EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
				parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		LosAngelesPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += (estimateTravelTime(variables_car) + estimateMonetaryCostUtility(variables_car))
				* (parameters.laAvgHHLIncome.avg_hhl_income / variables.hhlIncome) * parameters.betaCost_u_MU;

		return utility;
	}
}
