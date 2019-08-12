package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WalkEstimator implements UtilityEstimator {
	private final WalkPredictor predictor;
	private final ModeParameters parameters;

	@Inject
	public WalkEstimator(WalkPredictor predictor, ModeParameters parameters) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double predictConstantUtility() {
		return parameters.walk.alpha_u;
	}

	protected double predictTravelTimeUtility(WalkVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		WalkVariables variables = predictor.predictVariables(elements);

		utility += predictConstantUtility();
		utility += predictTravelTimeUtility(variables);

		return utility;
	}
}
