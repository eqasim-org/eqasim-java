package org.eqasim.examples.corsica_drt.mode_choice.utilities;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.corsica_drt.mode_choice.parameters.CorsicaDrtModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class DrtUtilityEstimator implements UtilityEstimator {
	private final CorsicaDrtModeParameters parameters;
	private final DrtPredictor predictor;

	@Inject
	public DrtUtilityEstimator(CorsicaDrtModeParameters parameters, DrtPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.drt.alpha_u;
	}

	protected double estimateTravelTimeUtility(DrtVariables variables) {
		return parameters.drt.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateWaitingTimeUtility(DrtVariables variables) {
		return parameters.drt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(DrtVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateAccessEgressTimeUtility(DrtVariables variables) {
		return parameters.drt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		DrtVariables variables = predictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);

		return utility;
	}
}
