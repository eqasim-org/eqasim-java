package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

public class SaoPauloWalkUtilityEstimator extends WalkUtilityEstimator {
	
	private final SaoPauloModeParameters parameters;
	private final SaoPauloPersonPredictor predictor;

	@Inject
	public SaoPauloWalkUtilityEstimator(SaoPauloModeParameters parameters, PersonPredictor personPredictor,
			WalkPredictor walkPredictor, SaoPauloPersonPredictor predictor) {
		super(parameters, walkPredictor);

		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateRegionalUtility(SaoPauloPersonVariables variables) {
		return (variables.cityTrip) ? parameters.spWalk.alpha_walk_city : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;
		double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord());
		if (distance > 4 * 5280)
			utility += -100;
		utility += super.estimateUtility(person, trip, elements);
		utility += estimateRegionalUtility(variables);

		return utility;
	}

}
