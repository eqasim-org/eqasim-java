package org.eqasim.wayne_county.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyCarUtilityEstimator extends CarUtilityEstimator {
	private final WayneCountyModeParameters parameters;
	private final WayneCountyPersonPredictor predictor;
	private final CarPredictor carPredictor;

	@Inject
	public WayneCountyCarUtilityEstimator(WayneCountyModeParameters parameters, PersonPredictor personPredictor,
			CarPredictor carPredictor, WayneCountyPersonPredictor predictor) {
		super(parameters, carPredictor);
		this.carPredictor = carPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	protected double estimateTravelTimeUtility(CarVariables variables_car) {
		return parameters.wcCar.beta_time_min * variables_car.travelTime_min;
	}

	protected double estimateMonetaryCostUtility(CarVariables carVariables, WayneCountyPersonVariables variables) {
		double costBeta = 0;
		switch (variables.hhlIncomeClass) {
		case 1:
			costBeta = parameters.wcCost.beta_cost_low_income;
			break;
		case 2:
			costBeta = parameters.wcCost.beta_cost_medium_income;
			break;
		case 3:
			costBeta = parameters.wcCost.beta_cost_high_income;
			break;
		}
		return costBeta * carVariables.cost_MU;
	}

	protected double estimateConstantUtility(WayneCountyPersonVariables variables) {
		switch (variables.hhlIncomeClass) {
		case 1:
			return parameters.wcCar.alpha_low_income;
		case 2:
			return parameters.wcCar.alpha_medium_income;
		case 3:
			return parameters.wcCar.alpha_high_income;
		default:
			return 2;
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility(variables);
		utility += estimateTravelTimeUtility(variables_car);
		utility += estimateMonetaryCostUtility(variables_car, variables);

		return utility;
	}
}
