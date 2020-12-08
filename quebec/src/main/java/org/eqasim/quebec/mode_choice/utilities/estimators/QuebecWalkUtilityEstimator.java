package org.eqasim.quebec.mode_choice.utilities.estimators;

import java.util.List;


import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.quebec.mode_choice.parameters.QuebecModeParameters;
import org.eqasim.quebec.mode_choice.utilities.predictors.QuebecPersonPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class QuebecWalkUtilityEstimator extends WalkUtilityEstimator {
	private final QuebecModeParameters parameters;
	private final WalkPredictor WalkPredictor;

	@Inject
	public QuebecWalkUtilityEstimator(QuebecModeParameters parameters, PersonPredictor personPredictor,
			WalkPredictor WalkPredictor, QuebecPersonPredictor predictor) {
		super(parameters, WalkPredictor);
		this.WalkPredictor = WalkPredictor;
		this.parameters = parameters;
	}
	protected double estimateTravelTime(WalkVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.travelTime_min;
	}
	

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double utility = 0.0;

		WalkVariables variables_walk = WalkPredictor.predictVariables(person, trip, elements);
		if (variables_walk.travelTime_min > 60)
			return -100;
		utility += estimateConstantUtility();
		utility += estimateTravelTime(variables_walk) ;


		return utility;
	}
}
