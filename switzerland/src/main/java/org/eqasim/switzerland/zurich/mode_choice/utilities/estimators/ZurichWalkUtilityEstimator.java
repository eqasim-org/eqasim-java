package org.eqasim.switzerland.zurich.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.switzerland.zurich.mode_choice.parameters.ZurichModeParameters;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichTripPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.predictors.ZurichWalkPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPersonVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichTripVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class ZurichWalkUtilityEstimator extends WalkUtilityEstimator {
	static public final String NAME = "ZurichWalkEstimator";

	private final ZurichModeParameters parameters;
	private final ZurichWalkPredictor predictor;
	private final ZurichPersonPredictor personPredictor;
	private final ZurichTripPredictor tripPredictor;

	@Inject
	public ZurichWalkUtilityEstimator(ZurichModeParameters parameters, ZurichWalkPredictor predictor,
			ZurichPersonPredictor personPredictor, ZurichTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(ZurichWalkVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(ZurichPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.ZurichWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(ZurichTripVariables variables) {
		return variables.isWork ? parameters.ZurichWalk.betaWork : 0.0;
	}

	protected double estimatePenalty(ZurichWalkVariables variables) {
		double beta = Math.log(100) / parameters.ZurichWalk.travelTimeThreshold_min;
		return -Math.exp(beta * variables.travelTime_min) + 1.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		ZurichWalkVariables variables = predictor.predictVariables(person, trip, elements);
		ZurichPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		ZurichTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		utility += estimatePenalty(variables);

		return utility;
	}
}