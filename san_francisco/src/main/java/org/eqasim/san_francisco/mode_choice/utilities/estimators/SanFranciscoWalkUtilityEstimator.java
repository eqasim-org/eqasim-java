package org.eqasim.san_francisco.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.WalkVariables;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class SanFranciscoWalkUtilityEstimator extends WalkUtilityEstimator {
	private final SanFranciscoModeParameters parameters;
	private final SanFranciscoPersonPredictor predictor;
	private WalkPredictor walkPredictor;

	@Inject
	public SanFranciscoWalkUtilityEstimator(SanFranciscoModeParameters parameters, PersonPredictor personPredictor,
			WalkPredictor walkPredictor, SanFranciscoPersonPredictor predictor) {
		super(parameters, walkPredictor);

		this.parameters = parameters;
		this.predictor = predictor;
		this.walkPredictor = walkPredictor;
	}

	protected double estimateRegionalUtility(SanFranciscoPersonVariables variables) {
		return (variables.cityTrip) ? parameters.sfWalk.alpha_walk_city : 0.0;
	}

	protected double estimateTravelTime(WalkVariables variables) {
		return parameters.sfWalk.vot_min * variables.travelTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		WalkVariables variables_walk = walkPredictor.predictVariables(person, trip, elements);
		
		utility += estimateConstantUtility();
		utility += estimateTravelTime(variables_walk) * (parameters.sfAvgHHLIncome.avg_hhl_income / variables.hhlIncome)
				* parameters.betaCost_u_MU;
		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
