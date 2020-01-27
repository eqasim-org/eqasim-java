package org.eqasim.sao_paulo.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloTaxiPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.TaxiVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SaoPauloTaxiUtilityEstimator {
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

	
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);
		TaxiVariables variables_taxi = taxiPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_taxi);
		utility += estimateAccessEgressTimeUtility(variables_taxi);
		utility += estimateMonetaryCostUtility(variables_taxi)
				* (parameters.spAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

		return utility;
	}


	private double estimateMonetaryCostUtility(TaxiVariables variables_taxi) {
		// TODO Auto-generated method stub
		return 0;
	}


	private double estimateAccessEgressTimeUtility(TaxiVariables variables_taxi) {
		// TODO Auto-generated method stub
		return 0;
	}


	private double estimateTravelTimeUtility(TaxiVariables variables_taxi) {
		// TODO Auto-generated method stub
		return 0;
	}


	private double estimateConstantUtility() {
		// TODO Auto-generated method stub
		return 0;
	}

}
