package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloTaxiPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.TaxiVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;


public class SaoPauloTaxiUtilityEstimator implements UtilityEstimator {
	private final SaoPauloModeParameters parameters;
	private final SaoPauloPersonPredictor predictor;
	private final SaoPauloTaxiPredictor taxiPredictor;

	@Inject
	public SaoPauloTaxiUtilityEstimator(SaoPauloModeParameters parameters, PersonPredictor personPredictor,
			SaoPauloTaxiPredictor taxiPredictor, SaoPauloPersonPredictor predictor) {
		this.taxiPredictor = taxiPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);
		TaxiVariables variables_taxi = taxiPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_taxi);
		utility += estimateAccessEgressTimeUtility(variables_taxi);
		if (variables.hhlIncome == 0.0)
			utility += estimateMonetaryCostUtility(variables_taxi)
			* (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);
		else
			utility += estimateMonetaryCostUtility(variables_taxi)
				* (parameters.spAvgHHLIncome.avg_hhl_income / parameters.spAvgHHLIncome.avg_hhl_income);

		return utility;
	}


	private double estimateTravelTimeUtility(TaxiVariables variables_taxi) {
		return parameters.spTaxi.beta_TravelTime_u_min * variables_taxi.travelTime_min;
	}


	protected double estimateMonetaryCostUtility(TaxiVariables variables_taxi) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables_taxi.euclideanDistance_km, 
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables_taxi.cost_MU;
	}


	protected double estimateAccessEgressTimeUtility(TaxiVariables variables_taxi) {
		return parameters.spTaxi.betaAccessEgressWalkTime_min * variables_taxi.accessEgressTime_min;
	}


	protected double estimateConstantUtility() {
		return parameters.spTaxi.alpha_u;
	}

}
