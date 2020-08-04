package org.eqasim.jakarta.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaCarodtPredictor;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaPersonPredictor;
import org.eqasim.jakarta.mode_choice.utilities.variables.CarodtVariables;
import org.eqasim.jakarta.mode_choice.utilities.variables.JakartaPersonVariables;


import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;


public class JakartaCarodtUtilityEstimator implements UtilityEstimator {
	private final JakartaModeParameters parameters;
	private final JakartaPersonPredictor predictor;
	private final JakartaCarodtPredictor CarodtPredictor;

	@Inject
	public JakartaCarodtUtilityEstimator(JakartaModeParameters parameters, PersonPredictor personPredictor,
			JakartaCarodtPredictor CarodtPredictor, JakartaPersonPredictor predictor) {
		this.CarodtPredictor = CarodtPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		JakartaPersonVariables variables = predictor.predictVariables(person, trip, elements);
		CarodtVariables variables_Carodt = CarodtPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_Carodt);
		utility += estimateAccessEgressTimeUtility(variables_Carodt);
		utility += parameters.jCarodt.alpha_age * variables.age;
		if (variables.sex == "f")
			utility += 0.0;
		else
			utility += parameters.jCarodt.alpha_sex	;
		//if (variables.hhlIncome == 0.0)
		utility += estimateMonetaryCostUtility(variables_Carodt) * EstimatorUtils.interaction(variables.hhlIncome, 
				parameters.jAvgHHLIncome.avg_hhl_income, parameters.jIncomeElasticity.lambda_income);
		//	* (parameters.jAvgHHLIncome.avg_hhl_income / 1.0);
		//else
		//	utility += estimateMonetaryCostUtility(variables_Carodt)
		//		* (parameters.jAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

		return utility;
	}


	private double estimateTravelTimeUtility(CarodtVariables variables_Carodt) {
		return parameters.jCarodt.beta_TravelTime_u_min * variables_Carodt.travelTime_min;
	}


	protected double estimateMonetaryCostUtility(CarodtVariables variables_Carodt) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables_Carodt.euclideanDistance_km, 
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance)  * variables_Carodt.cost_MU;
	}

	
	protected double estimateAccessEgressTimeUtility(CarodtVariables variables_Carodt) {
		return parameters.jCarodt.betaAccessEgressWalkTime_min * variables_Carodt.accessEgressTime_min;
	}


	protected double estimateConstantUtility() {
		return parameters.jCarodt.alpha_u;
	}

	public JakartaCarodtPredictor getCarodtPredictor() {
		return CarodtPredictor;
	}

}