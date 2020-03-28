package org.eqasim.projects.astra16.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.WalkUtilityEstimator;
import org.eqasim.projects.astra16.AstraModeParameters;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.predictors.AstraWalkPredictor;
import org.eqasim.projects.astra16.mode_choice.utilities.variables.AstraPersonVariables;
import org.eqasim.projects.astra16.mode_choice.utilities.variables.AstraTripVariables;
import org.eqasim.projects.astra16.mode_choice.utilities.variables.AstraWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraWalkUtilityEstimator extends WalkUtilityEstimator {
	static public final String NAME = "AstraWalkEstimator";

	private final AstraModeParameters parameters;
	private final AstraWalkPredictor predictor;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;

	@Inject
	public AstraWalkUtilityEstimator(AstraModeParameters parameters, AstraWalkPredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		super(parameters, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(AstraWalkVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraWalk.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraWalk.betaWork : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AstraWalkVariables variables = predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		if (variables.travelTime_min > 30.0) {
			utility -= 100.0;
		}

		return utility;
	}
}
