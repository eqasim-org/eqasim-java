package org.eqasim.wayne_county.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyWalkUtilityEstimator extends WalkUtilityEstimator {
	private final WayneCountyModeParameters parameters;
	private final WayneCountyPersonPredictor predictor;
	private WalkPredictor walkPredictor;

	@Inject
	public WayneCountyWalkUtilityEstimator(WayneCountyModeParameters parameters, PersonPredictor personPredictor,
			WalkPredictor walkPredictor, WayneCountyPersonPredictor predictor) {
		super(parameters, walkPredictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.walkPredictor = walkPredictor;
	}
	
	protected double estimateConstantUtility(WayneCountyPersonVariables variables) {
		switch (variables.hhlIncomeClass) {
		case 1:
			return parameters.wcWalk.alpha_low_income;
		case 2:
			return parameters.wcWalk.alpha_medium_income;
		case 3:
			return parameters.wcWalk.alpha_high_income;
		default:
			return 2;
		}
	}

	protected double estimateTravelTime(WalkVariables variables) {
		return parameters.wcWalk.beta_time_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);
		WalkVariables variables_walk = walkPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;
		
		utility += estimateConstantUtility(variables);
		utility += estimateTravelTime(variables_walk);
		
		return utility;
	}
}
