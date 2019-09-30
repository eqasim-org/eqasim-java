package org.eqasim.projects.dynamic_av.mode_choice;

import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class ProjectModeParameters extends SwissModeParameters {
	static public class ProjectBaseModeParameters {
		public double betaAgeOver60 = 0.0;
		public double betaWork = 0.0;
	}

	public ProjectBaseModeParameters projectWalk = new ProjectBaseModeParameters();
	public ProjectBaseModeParameters projectBike = new ProjectBaseModeParameters();
	public ProjectBaseModeParameters projectCar = new ProjectBaseModeParameters();
	public ProjectBaseModeParameters projectAv = new ProjectBaseModeParameters();

	public class ProjectPtParameters {
		public double betaRailTravelTime = 0.0;
		public double betaBusTravelTime = 0.0;
		public double betaFeederTravelTime = 0.0;

		public double betaHeadway_min = 0.0;
	}

	public ProjectPtParameters projectPt = new ProjectPtParameters();

	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public ProjectModeParameters buildDefault() {
		ProjectModeParameters parameters = new ProjectModeParameters();

		// General
		parameters.betaCost_u_MU = -0.0959;

		parameters.lambdaTravelTimeEuclideanDistance = -0.105;
		parameters.lambdaCostEuclideanDistance = -0.34;
		parameters.referenceEuclideanDistance_km = 39.0;

		parameters.lambdaCostHouseholdIncome = -0.321;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Walk
		parameters.walk.alpha_u = -0.195 + 0.9;
		parameters.walk.betaTravelTime_u_min = -0.036 - 0.02;

		parameters.projectWalk.betaAgeOver60 = 2.28;
		parameters.projectWalk.betaWork = 1.13;

		// Bike
		parameters.bike.alpha_u = 0.629 - 0.4;
		parameters.bike.betaTravelTime_u_min = -0.0638 + 0.02;

		parameters.projectBike.betaAgeOver60 = -2.39;
		parameters.projectBike.betaWork = -0.454;

		// Car
		parameters.car.alpha_u = 0.490; // 0.629;
		parameters.car.betaTravelTime_u_min = -0.0291; // -0.0638;

		parameters.projectCar.betaAgeOver60 = 0.258;
		parameters.projectCar.betaWork = -1.06;
		
		parameters.car.constantParkingSearchPenalty_min = 4.0;
		parameters.car.constantAccessEgressWalkTime_min = 4.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.0751;
		parameters.pt.betaLineSwitch_u = -0.195;
		parameters.pt.betaWaitingTime_u_min = -0.0126;

		parameters.projectPt.betaRailTravelTime = -0.0154;
		parameters.projectPt.betaBusTravelTime = -0.0299;
		parameters.projectPt.betaFeederTravelTime = -0.0582;

		return parameters;
	}
}
