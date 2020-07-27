package org.eqasim.san_francisco.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.san_francisco.mode_choice.parameters.SanFranciscoModeParameters;
import org.eqasim.san_francisco.mode_choice.utilities.predictors.SanFranciscoPersonPredictor;
import org.eqasim.san_francisco.mode_choice.utilities.variables.SanFranciscoPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SanFranciscoPTUtilityEstimator extends PtUtilityEstimator {
	private final SanFranciscoModeParameters parameters;
	private final SanFranciscoPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public SanFranciscoPTUtilityEstimator(SanFranciscoModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, SanFranciscoPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateRegionalUtility(SanFranciscoPersonVariables variables) {
		double utility_city = (variables.cityTrip) ? parameters.sfPT.alpha_pt_city : 0.0;

		return utility_city;
	}

	
	@Override
	protected double estimateMonetaryCostUtility(PtVariables variables) {
		return EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
				parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}
	
	protected double estimateTravelTimeUtility(PtVariables variables_pt) {
		return parameters.sfPT.travelTime_min
				* (variables_pt.inVehicleTime_min + variables_pt.accessEgressTime_min + variables_pt.waitingTime_min);
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SanFranciscoPersonVariables variables = predictor.predictVariables(person, trip, elements);
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;
		utility += estimateConstantUtility();
		utility += estimateTravelTimeUtility(variables_pt);
		if (variables.hhlIncome == 0.0)
			utility += parameters.betaCost_u_MU * estimateMonetaryCostUtility(variables_pt) * Math.pow(
					(10.000 / parameters.sfAvgHHLIncome.avg_hhl_income), parameters.sfIncomeElasticity.lambda_income);
		else
			utility += parameters.betaCost_u_MU * estimateMonetaryCostUtility(variables_pt)
					* Math.pow((variables.hhlIncome / parameters.sfAvgHHLIncome.avg_hhl_income),
							parameters.sfIncomeElasticity.lambda_income);

		utility += estimateRegionalUtility(variables);

		return utility;
	}
}
