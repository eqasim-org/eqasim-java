package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class IDFBikeUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final WalkPredictor walkPredictor;

	@Inject
	public IDFBikeUtilityEstimator(ModeParameters parameters, WalkPredictor walkPredictor) {
		this.parameters = parameters;
		this.walkPredictor = walkPredictor; // This is meant to be walk as we don't differentiate in choice model!
	}

	protected double estimateConstantUtility() {
		return parameters.bike.alpha_u;
	}

	protected double estimateTravelTimeUtility(WalkVariables variables) {
		return parameters.bike.betaTravelTime_u_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WalkVariables walkVariables = walkPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(walkVariables);

		return utility;
	}
}
