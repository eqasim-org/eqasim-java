package org.eqasim.switzerland.mode_choice.utilities.estimators;

import org.eqasim.switzerland.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.switzerland.mode_choice.utilities.variables.PtVariables;

public class PtEstimator {
	private final ModeChoiceParameters parameters;

	public PtEstimator(ModeChoiceParameters parameters) {
		this.parameters = parameters;
	}

	public double estimateUtility(PtVariables variables) {
		double utility = 0.0;

		utility += parameters.pt.alpha;

		utility += parameters.pt.betaAccessEgressTime * variables.accessEgressTime_min;
		utility += parameters.pt.betaInVehicleTime * variables.inVehicleTime_min;
		utility += parameters.pt.betaWaitingTime * variables.waitingTime_min;
		utility += parameters.pt.betaLineSwitch * variables.numberOfLineSwitches;

		utility += parameters.betaCost //
				* Math.pow(Math.max(variables.crowflyDistance_km, 0.001) / parameters.referenceCrowflyDistance_km,
						parameters.lambdaCostCrowflyDistance) //
				* variables.cost_CHF;

		return utility;
	}
}
