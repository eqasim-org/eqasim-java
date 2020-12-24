package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class WalkUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final WalkPredictor predictor;

	@Inject
	public WalkUtilityEstimator(ModeParameters parameters, WalkPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.walk.alpha_u;
	}

	protected double estimateTravelTimeUtility(WalkVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WalkVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);

		return utility;
	}
}
