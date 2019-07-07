package org.eqasim.switzerland.mode_choice.costs;

import org.eqasim.switzerland.mode_choice.parameters.CostParameters;
import org.eqasim.switzerland.mode_choice.utilities.variables.PersonVariables;

public class PtCostModel {
	private final CostParameters costParameters;

	public PtCostModel(CostParameters costParameters) {
		this.costParameters = costParameters;
	}

	public double calculate_CHF(PersonVariables generalVariables, double inVehicleDistance_km, double homeDistance) {
		if (generalVariables.hasGeneralSubscription) {
			return 0.0;
		}

		if (generalVariables.hasRegionalSubscription) {
			if (homeDistance <= costParameters.ptRegionalRadius_km) {
				return 0.0;
			}
		}

		double fullCost = Math.max(costParameters.ptCostMinimum_CHF,
				costParameters.ptCostPerKm_CHF * inVehicleDistance_km);

		if (generalVariables.hasHalbtaxSubscription) {
			return fullCost * 0.5;
		} else {
			return fullCost;
		}
	}
}
