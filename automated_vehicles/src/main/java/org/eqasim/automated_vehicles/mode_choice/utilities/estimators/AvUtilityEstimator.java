package org.eqasim.automated_vehicles.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.automated_vehicles.mode_choice.utilities.predictors.AvPredictor;
import org.eqasim.automated_vehicles.mode_choice.utilities.variables.AvVariables;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class AvUtilityEstimator implements UtilityEstimator {
	private final ModeParameters generalParameters;
	private final AvModeParameters avParameters;
	private final AvPredictor predictor;

	@Inject
	public AvUtilityEstimator(ModeParameters generalParameters, AvModeParameters avParameters, AvPredictor predictor) {
		this.generalParameters = generalParameters;
		this.avParameters = avParameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return avParameters.alpha_u;
	}

	protected double estimateTravelTimeUtility(AvVariables variables) {
		return avParameters.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateWaitingTimeUtility(AvVariables variables) {
		return avParameters.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(AvVariables variables) {
		return generalParameters.betaCost_u_MU
				* EstimatorUtils.interaction(variables.euclideanDistance_km,
						generalParameters.referenceEuclideanDistance_km, generalParameters.lambdaCostEuclideanDistance)
				* variables.cost_MU;
	}
	
	protected double estimateAccessEgressTimeUtility(AvVariables variables) {
		return avParameters.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AvVariables variables = predictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);

		return utility;
	}
}
