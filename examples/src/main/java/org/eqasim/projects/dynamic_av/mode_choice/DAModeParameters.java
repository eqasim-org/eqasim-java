package org.eqasim.projects.dynamic_av.mode_choice;

import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class DAModeParameters extends SwissModeParameters {
	public class DaModeParameters {
		public double betaAgeOver60 = 0.0;
		public double betaWork = 0.0;
	}

	public DaModeParameters daWalk = new DaModeParameters();
	public DaModeParameters daBike = new DaModeParameters();
	public DaModeParameters daCar = new DaModeParameters();

	public class DAPtParameters {
		public double betaRailTravelTime = 0.0;
		public double betaBusTravelTime = 0.0;
		public double betaFeederTravelTime = 0.0;

		public double betaHeadway_min = 0.0;
	}

	public DAPtParameters daPt = new DAPtParameters();

	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public DAModeParameters buildDefault() {
		DAModeParameters parameters = new DAModeParameters();

		// General
		parameters.betaCost_u_MU = -0.0959;

		parameters.lambdaTravelTimeEuclideanDistance = -0.105;
		parameters.lambdaCostEuclideanDistance = -0.34;
		parameters.referenceEuclideanDistance_km = 39.0;

		parameters.lambdaCostHouseholdIncome = -0.321;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Walk
		parameters.walk.alpha_u = -0.195;
		parameters.walk.betaTravelTime_u_min = -0.036;

		parameters.daWalk.betaAgeOver60 = 2.28;
		parameters.daWalk.betaWork = 1.13;

		// Bike
		parameters.bike.alpha_u = 0.629;
		parameters.bike.betaTravelTime_u_min = -0.0638;

		parameters.daBike.betaAgeOver60 = -2.39;
		parameters.daBike.betaWork = -0.454;

		// Car
		parameters.car.alpha_u = 0.490; // 0.629;
		parameters.car.betaTravelTime_u_min = -0.0291; // -0.0638;

		parameters.daCar.betaAgeOver60 = 0.258;
		parameters.daCar.betaWork = -1.06;
		
		// parameters.car.constantParkingSearchPenalty_min = 4.0;
		// parameters.car.constantAccessEgressWalkTime_min = 4.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.0751;
		parameters.pt.betaLineSwitch_u = -0.195;
		parameters.pt.betaWaitingTime_u_min = -0.0126;

		parameters.daPt.betaRailTravelTime = -0.0154;
		parameters.daPt.betaBusTravelTime = -0.0299;
		parameters.daPt.betaFeederTravelTime = -0.0582;

		return parameters;
	}
}
