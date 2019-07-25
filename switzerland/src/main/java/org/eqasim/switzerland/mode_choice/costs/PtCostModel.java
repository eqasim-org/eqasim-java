package org.eqasim.switzerland.mode_choice.costs;

import org.eqasim.switzerland.mode_choice.parameters.CostParameters;
import org.eqasim.switzerland.mode_choice.utilities.variables.PersonVariables;

public class PtCostModel {
	private final CostParameters costParameters;

	public PtCostModel(CostParameters costParameters) {
		this.costParameters = costParameters;
	}

	public enum ModelType {
		ASTRA2016, ASTRA2018
	}

	private double getStandardCostPerKm_CHF(double crowflyDistance_km) {
		if (crowflyDistance_km <= 1.0) {
			return 4.51;
		} else if (crowflyDistance_km <= 2.0) {
			return 1.799;
		} else if (crowflyDistance_km <= 5.0) {
			return 1.31;
		} else if (crowflyDistance_km <= 10.0) {
			return 0.878;
		} else if (crowflyDistance_km <= 20.0) {
			return 0.685;
		} else if (crowflyDistance_km <= 30.0) {
			return 0.619;
		} else if (crowflyDistance_km <= 40.0) {
			return 0.676;
		} else if (crowflyDistance_km <= 50.0) {
			return 0.655;
		} else {
			return 0.5898;
		}
	}

	private double getZVVCostPerKm_CHF(double crowflyDistance_km) {
		if (crowflyDistance_km <= 30.0) {
			return 0.0;
		} else if (crowflyDistance_km <= 40.0) {
			return 0.437;
		} else if (crowflyDistance_km <= 50.0) {
			return 0.564;
		} else {
			return 0.491;
		}
	}

	public double calculate_CHF(PersonVariables generalVariables, double inVehicleDistance_km, double homeDistance,
			double crowflyDistance_km) {
		if (costParameters.useASTRA2018 >= 1.0) {
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
		} else {
			if (generalVariables.hasGeneralSubscription) {
				return 0.0;
			} else if (generalVariables.hasHalbtaxSubscription) {
				return Math.max(0.5 * getStandardCostPerKm_CHF(crowflyDistance_km) * crowflyDistance_km, 2.3);
			} else if (generalVariables.hasRegionalSubscription) {
				return getZVVCostPerKm_CHF(crowflyDistance_km) * crowflyDistance_km;
			} else {
				return Math.max(getStandardCostPerKm_CHF(crowflyDistance_km) * crowflyDistance_km, 2.7);
			}
		}
	}
}
