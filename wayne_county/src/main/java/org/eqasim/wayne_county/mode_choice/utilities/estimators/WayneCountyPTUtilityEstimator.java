package org.eqasim.wayne_county.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PersonPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.wayne_county.mode_choice.parameters.WayneCountyModeParameters;
import org.eqasim.wayne_county.mode_choice.utilities.predictors.WayneCountyPersonPredictor;
import org.eqasim.wayne_county.mode_choice.utilities.variables.WayneCountyPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class WayneCountyPTUtilityEstimator extends PtUtilityEstimator {
	private final WayneCountyModeParameters parameters;
	private final WayneCountyPersonPredictor predictor;
	private final PtPredictor ptPredictor;

	@Inject
	public WayneCountyPTUtilityEstimator(WayneCountyModeParameters parameters, PersonPredictor personPredictor,
			PtPredictor ptPredictor, WayneCountyPersonPredictor predictor) {
		super(parameters, ptPredictor);
		this.ptPredictor = ptPredictor;
		this.parameters = parameters;
		this.predictor = predictor;
	}

	protected double estimateConstantUtility(WayneCountyPersonVariables variables) {
		switch (variables.hhlIncomeClass) {
		case 1:
			return parameters.wcPT.alpha_low_income;
		case 2:
			return parameters.wcPT.alpha_medium_income;
		case 3:
			return parameters.wcPT.alpha_high_income;
		default:
			return 2;
		}
	}

	protected double estimateTravelTimeUtility(PtVariables variables_pt) {
		return parameters.wcPT.beta_time_min * variables_pt.inVehicleTime_min
				+ parameters.pt.betaAccessEgressTime_u_min * variables_pt.accessEgressTime_min
				+ parameters.pt.betaWaitingTime_u_min * variables_pt.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(PtVariables ptVariables, WayneCountyPersonVariables variables) {
		double costBeta = 0;
		switch (variables.hhlIncomeClass) {
		case 1:
			costBeta = parameters.wcCost.beta_cost_low_income;
			break;
		case 2:
			costBeta = parameters.wcCost.beta_cost_medium_income;
			break;
		case 3:
			costBeta = parameters.wcCost.beta_cost_high_income;
			break;
		}
		return costBeta * ptVariables.cost_MU;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		WayneCountyPersonVariables variables = predictor.predictVariables(person, trip, elements);
		PtVariables variables_pt = ptPredictor.predict(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility(variables);
		utility += estimateTravelTimeUtility(variables_pt);
		utility += estimateMonetaryCostUtility(variables_pt);
		return utility;
	}
}
