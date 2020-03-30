package org.eqasim.projects.astra16.mode_choice.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.projects.astra16.AstraModeParameters;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraBikePredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.variables.AstraBikeVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraPersonVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraTripVariables;
import org.eqasim.switzerland.mode_choice.utilities.estimators.SwissBikeUtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraBikeUtilityEstimator extends SwissBikeUtilityEstimator {
	static public final String NAME = "AstraBikeEstimator";

	private final AstraModeParameters parameters;
	private final AstraBikePredictor predictor;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;

	@Inject
	public AstraBikeUtilityEstimator(AstraModeParameters parameters, AstraBikePredictor predictor,
			AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		super(parameters, personPredictor.delegate, predictor.delegate);

		this.parameters = parameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateTravelTimeUtility(AstraBikeVariables variables) {
		return super.estimateTravelTimeUtility(variables) //
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? parameters.astraBike.betaAgeOver60 : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? parameters.astraBike.betaWork : 0.0;
	}
	
	protected double estimatePenalty(AstraBikeVariables variables) {
		double beta = Math.log(100) / parameters.astraBike.travelTimeThreshold_min;
		return -Math.exp(beta * variables.travelTime_min);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AstraBikeVariables variables = predictor.predictVariables(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);
		// utility += estimatePenalty(variables);
		
		return utility;
	}
}
