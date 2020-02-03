package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloTaxiPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.TaxiVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class TaxiUtilityEstimator implements UtilityEstimator {
	private final SaoPauloModeParameters parameters;
	private final SaoPauloTaxiPredictor predictor;
	
	@Inject
	public TaxiUtilityEstimator(SaoPauloModeParameters parameters, SaoPauloTaxiPredictor predictor) {
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility() {
		return parameters.spTaxi.alpha_u;
	}

	protected double estimateTravelTimeTimeUtility(TaxiVariables variables) {
		return parameters.spTaxi.beta_TravelTime_u_min * variables.travelTime_min;
	}
	
	protected double estimateAccessEgressTimeUtility(TaxiVariables variables) {
		return parameters.spTaxi.betaAccessEgressWalkTime_min * variables.accessEgressTime_min;
	}

	protected double estimateMonetaryCostUtility(TaxiVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		TaxiVariables variables = predictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(variables);
		utility += estimateTravelTimeTimeUtility(variables);
		utility += estimateMonetaryCostUtility(variables);

		return utility;
	}

}
