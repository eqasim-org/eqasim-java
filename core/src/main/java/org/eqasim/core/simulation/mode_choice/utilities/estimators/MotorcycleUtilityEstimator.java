package org.eqasim.core.simulation.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.MotorcyclePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.MotorcycleVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class MotorcycleUtilityEstimator implements UtilityEstimator {
	private final ModeParameters parameters;
	private final MotorcyclePredictor predictor;

	@Inject
	public MotorcycleUtilityEstimator(ModeParameters parameters, MotorcyclePredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.motorcycle.alpha_u;
	}

	protected double estimateTravelTimeUtility(MotorcycleVariables variables) {
		return parameters.motorcycle.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateAccessEgressTimeUtility(MotorcycleVariables variables) {
		return parameters.walk.betaTravelTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(MotorcycleVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		MotorcycleVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);

		return utility;
	}
}
