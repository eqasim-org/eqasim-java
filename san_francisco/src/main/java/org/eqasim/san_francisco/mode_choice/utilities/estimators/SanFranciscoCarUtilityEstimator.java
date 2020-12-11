package org.eqasim.san_francisco.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SanFranciscoCarUtilityEstimator extends CarUtilityEstimator {
	private final SanFranciscoModeParameters parameters;
	private final SanFranciscoPersonPredictor predictor;
	private final CarPredictor carPredictor;

	@Inject
	public SanFranciscoCarUtilityEstimator(SanFranciscoModeParameters parameters, PersonPredictor personPredictor,
			CarPredictor carPredictor, SanFranciscoPersonPredictor predictor) {
		super(parameters, carPredictor);
		this.carPredictor = carPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	protected double estimateMonetaryCostUtility(CarVariables variables) {
		return EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
				parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateRegionalUtility(SanFranciscoPersonVariables variables) {
		return (variables.cityTrip) ? parameters.sfCar.alpha_car_city : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_car);
		utility += estimateRegionalUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables_car);
		if (variables.hhlIncome == 0.0)
			utility += parameters.betaCost_u_MU * estimateMonetaryCostUtility(variables_car) * Math.pow(
					(10.000 / parameters.sfAvgHHLIncome.avg_hhl_income), parameters.sfIncomeElasticity.lambda_income);
		else
			utility += parameters.betaCost_u_MU * estimateMonetaryCostUtility(variables_car)
					* Math.pow((variables.hhlIncome / parameters.sfAvgHHLIncome.avg_hhl_income),
							parameters.sfIncomeElasticity.lambda_income);

		return utility;
	}
}