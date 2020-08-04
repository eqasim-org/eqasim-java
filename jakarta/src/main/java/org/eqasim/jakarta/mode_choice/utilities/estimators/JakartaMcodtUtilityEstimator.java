package org.eqasim.jakarta.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.jakarta.mode_choice.parameters.JakartaModeParameters;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaMcodtPredictor;
import org.eqasim.jakarta.mode_choice.utilities.predictors.JakartaPersonPredictor;
import org.eqasim.jakarta.mode_choice.utilities.variables.JakartaPersonVariables;
import org.eqasim.jakarta.mode_choice.utilities.variables.McodtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;


public class JakartaMcodtUtilityEstimator implements UtilityEstimator {
	private final JakartaModeParameters parameters;
	private final JakartaPersonPredictor predictor;
	private final JakartaMcodtPredictor mcodtPredictor;

	@Inject
	public JakartaMcodtUtilityEstimator(JakartaModeParameters parameters, PersonPredictor personPredictor,
			JakartaMcodtPredictor mcodtPredictor, JakartaPersonPredictor predictor) {
		this.mcodtPredictor = mcodtPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		JakartaPersonVariables variables = predictor.predictVariables(person, trip, elements);
		McodtVariables variables_mcodt = mcodtPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_mcodt);
		utility += estimateAccessEgressTimeUtility(variables_mcodt);
		utility += parameters.jMcodt.alpha_age * variables.age;
		if (variables.sex == "f")
			utility += 0.0;
		else
			utility += parameters.jMcodt.alpha_sex	;
		//if (variables.hhlIncome == 0.0)
		//	utility += estimateMonetaryCostUtility(variables_mcodt)
		utility += estimateMonetaryCostUtility(variables_mcodt) * EstimatorUtils.interaction(variables.hhlIncome, 
				parameters.jAvgHHLIncome.avg_hhl_income, parameters.jIncomeElasticity.lambda_income);
		//	* (parameters.jAvgHHLIncome.avg_hhl_income / 1.0);
		//else
		//	utility += estimateMonetaryCostUtility(variables_mcodt)
		//		* (parameters.jAvgHHLIncome.avg_hhl_income / variables.hhlIncome);

		return utility;
	}


	private double estimateTravelTimeUtility(McodtVariables variables_mcodt) {
		return parameters.jMcodt.beta_TravelTime_u_min * variables_mcodt.travelTime_min;
	}


	protected double estimateMonetaryCostUtility(McodtVariables variables_mcodt) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables_mcodt.euclideanDistance_km, 
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables_mcodt.cost_MU;
	}


	protected double estimateAccessEgressTimeUtility(McodtVariables variables_mcodt) {
		return parameters.jMcodt.betaAccessEgressWalkTime_min * variables_mcodt.accessEgressTime_min;
	}


	protected double estimateConstantUtility() {
		return parameters.jMcodt.alpha_u;
	}

	public JakartaMcodtPredictor getMcodtPredictor() {
		return mcodtPredictor;
	}

}