package org.eqasim.los_angeles.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.los_angeles.mode_choice.parameters.LosAngelesModeParameters;
import org.eqasim.los_angeles.mode_choice.utilities.predictors.LosAngelesPersonPredictor;
import org.eqasim.los_angeles.mode_choice.utilities.variables.LosAngelesPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class LosAngelesWalkUtilityEstimator extends WalkUtilityEstimator {
	private final LosAngelesModeParameters parameters;
	private final LosAngelesPersonPredictor predictor;
	private WalkPredictor walkPredictor;

	@Inject
	public LosAngelesWalkUtilityEstimator(LosAngelesModeParameters parameters, PersonPredictor personPredictor,
			WalkPredictor walkPredictor, LosAngelesPersonPredictor predictor) {
		super(parameters, walkPredictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.walkPredictor = walkPredictor;
	}

	protected double estimateRegionalUtility(LosAngelesPersonVariables variables) {
		return (variables.cityTrip) ? parameters.laWalk.alpha_walk_city : 0.0;
	}

	protected double estimateTravelTime(WalkVariables variables) {
		return parameters.laWalk.vot_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		LosAngelesPersonVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		WalkVariables variables_walk = walkPredictor.predictVariables(person, trip, elements);
		if (variables_walk.travelTime_min > 40)
			return -100;
		utility += estimateConstantUtility();
		utility += estimateTravelTime(variables_walk) * (parameters.laAvgHHLIncome.avg_hhl_income / variables.hhlIncome)
				* parameters.betaCost_u_MU;
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
