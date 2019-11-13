package org.eqasim.los_angeles.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesModeParameters;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

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

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		LosAngelesPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarVariables variables_car = carPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_car);
		utility += estimateAccessEgressTimeUtility(variables_car);
		utility += estimateMonetaryCostUtility(variables_car)
				* (parameters.laAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

		return utility;
	}
}
