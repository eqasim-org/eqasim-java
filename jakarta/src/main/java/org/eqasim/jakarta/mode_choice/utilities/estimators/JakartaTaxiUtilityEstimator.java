package org.eqasim.jakarta.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaPersonPredictor;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaTaxiPredictor;
import org.eqasim.jakarta.mode_choice.utilities.variables.JakartaPersonVariables;
import org.eqasim.jakarta.mode_choice.utilities.variables.TaxiVariables;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;


public class JakartaTaxiUtilityEstimator implements UtilityEstimator {
	private final JakartaModeParameters parameters;
	private final JakartaPersonPredictor predictor;
	private final JakartaTaxiPredictor taxiPredictor;

	@Inject
	public JakartaTaxiUtilityEstimator(JakartaModeParameters parameters, PersonPredictor personPredictor,
			JakartaTaxiPredictor taxiPredictor, JakartaPersonPredictor predictor) {
		this.taxiPredictor = taxiPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		JakartaPersonVariables variables = predictor.predictVariables(person, trip, elements);
		TaxiVariables variables_taxi = taxiPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_taxi);
		utility += estimateAccessEgressTimeUtility(variables_taxi);
		if (variables.hhlIncome == 0.0)
			utility += estimateMonetaryCostUtility(variables_taxi)
			* (parameters.spAvgHHLIncome.avg_hhl_income / 1.0);
		else
			utility += estimateMonetaryCostUtility(variables_taxi)
				* (parameters.spAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

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
