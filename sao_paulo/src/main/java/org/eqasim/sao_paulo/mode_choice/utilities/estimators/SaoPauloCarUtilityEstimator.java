package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SaoPauloCarUtilityEstimator extends CarUtilityEstimator {
	private final SaoPauloModeParameters parameters;
	private final SaoPauloPersonPredictor predictor;
	private final CarPredictor carPredictor;

	@Inject
	public SaoPauloCarUtilityEstimator(SaoPauloModeParameters parameters, PersonPredictor personPredictor,
			CarPredictor carPredictor, SaoPauloPersonPredictor predictor) {
		super(parameters, carPredictor);
		this.carPredictor = carPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateRegionalUtility(SaoPauloPersonVariables variables) {
		return (variables.cityTrip) ? parameters.spCar.alpha_car_city : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_car);
		utility += estimateRegionalUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables_car);
		if (variables.hhlIncome == 0.0)
			utility += estimateMonetaryCostUtility(variables_car) * (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);
		else
			utility += estimateMonetaryCostUtility(variables_car)
					* (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);

		return utility;
	}

}
