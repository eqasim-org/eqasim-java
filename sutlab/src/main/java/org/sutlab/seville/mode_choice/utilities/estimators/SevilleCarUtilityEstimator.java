package org.sutlab.seville.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.CarUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.CarVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville.mode_choice.parameters.SevilleModeParameters;

import java.util.List;

public class SevilleCarUtilityEstimator extends CarUtilityEstimator {
	private final SevilleModeParameters parameters;
	private final CarPredictor predictor;

	@Inject
	public SevilleCarUtilityEstimator(SevilleModeParameters parameters, CarPredictor predictor) {
		super(parameters, predictor);

		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateAccessEgressTimeUtility(CarVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
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