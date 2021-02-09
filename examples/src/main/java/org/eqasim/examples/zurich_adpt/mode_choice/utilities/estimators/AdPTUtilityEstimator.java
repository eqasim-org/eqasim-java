package org.eqasim.examples.zurich_adpt.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.examples.zurich_adpt.mode_choice.mode_parameters.AdPTModeParameters;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.predictors.AdPTPredictor;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.variables.AdPTVariables;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraPersonPredictor;
import org.eqasim.projects.astra16.mode_choice.predictors.AstraTripPredictor;
import org.eqasim.projects.astra16.mode_choice.variables.AstraPersonVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AdPTUtilityEstimator implements UtilityEstimator {
	private final ModeParameters generalParameters;
	private final AdPTModeParameters adptParameters;
	private final AdPTPredictor predictor;
	private final AstraPersonPredictor personPredictor;
	private final AstraTripPredictor tripPredictor;

	@Inject
	public AdPTUtilityEstimator(ModeParameters generalParameters, AdPTModeParameters avParameters,
			AdPTPredictor predictor, AstraPersonPredictor personPredictor, AstraTripPredictor tripPredictor) {
		this.generalParameters = generalParameters;
		this.adptParameters = avParameters;
		this.predictor = predictor;
		this.personPredictor = personPredictor;
		this.tripPredictor = tripPredictor;
	}

	protected double estimateConstantUtility() {
		return adptParameters.alpha_u;
	}

	protected double estimateTravelTimeUtility(AdPTVariables variables) {
		return adptParameters.betaTravelTime_u_min * variables.travelTime_min;
	}

	protected double estimateWaitingTimeUtility(AdPTVariables variables) {
		return adptParameters.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(AdPTVariables variables) {
		return generalParameters.betaCost_u_MU
				* EstimatorUtils.interaction(variables.euclideanInVehicleDistance_km,
						generalParameters.referenceEuclideanDistance_km, generalParameters.lambdaCostEuclideanDistance)
				* variables.cost_MU;
	}

	protected double estimateAccessEgressTimeUtility(AdPTVariables variables) {
		return adptParameters.betaAccessEgressTime_min * (variables.accessTime_min + variables.egressTime_min);
	}

	protected double estimateAgeUtility(AstraPersonVariables variables) {
		return variables.age_a >= 60 ? adptParameters.betaAgeOver60_u : 0.0;
	}

	protected double estimateWorkUtility(AstraTripVariables variables) {
		return variables.isWork ? adptParameters.betaWork_u : 0.0;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		AdPTVariables variables = predictor.predict(person, trip, elements);
		AstraPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		AstraTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables);
		utility += estimateWaitingTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);
		utility += estimateAccessEgressTimeUtility(variables);

		utility += estimateAgeUtility(personVariables);
		utility += estimateWorkUtility(tripVariables);

		return utility;
	}

}
