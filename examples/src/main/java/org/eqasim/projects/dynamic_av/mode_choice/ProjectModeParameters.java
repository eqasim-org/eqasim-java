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
		public double betaRailTravelTime_u_min = 0.0;
		public double betaBusTravelTime_u_min = 0.0;
		public double betaFeederTravelTime_u_min = 0.0;

		public double betaHeadway_u_min = 0.0;
		public double betaOvgkB_u = 0.0;
		public double betaOvgkC_u = 0.0;
		public double betaOvgkD_u = 0.0;
		public double betaOvgkNone_u = 0.0;
	}

	public ProjectPtParameters projectPt = new ProjectPtParameters();

	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public ProjectModeParameters buildPrevious() {
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

		parameters.projectPt.betaRailTravelTime_u_min = -0.0154;
		parameters.projectPt.betaBusTravelTime_u_min = -0.0299;
		parameters.projectPt.betaFeederTravelTime_u_min = -0.0582;

		return parameters;
	}

	static public ProjectModeParameters buildDefault2() { // 4 Oct
		ProjectModeParameters parameters = new ProjectModeParameters();

		// General
		parameters.betaCost_u_MU = -0.089;

		parameters.lambdaTravelTimeEuclideanDistance = 0.0;
		parameters.lambdaCostEuclideanDistance = -0.226;
		parameters.referenceEuclideanDistance_km = 39.0;

		parameters.lambdaCostHouseholdIncome = -0.825;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Walk
		parameters.walk.alpha_u = 0.564;
		parameters.walk.betaTravelTime_u_min = -0.032;

		// Bike
		parameters.bike.alpha_u = 0.216;
		parameters.bike.betaTravelTime_u_min = -0.106;

		parameters.projectBike.betaAgeOver60 = -2.683;

		// Car
		parameters.car.alpha_u = 0.150;
		parameters.car.betaTravelTime_u_min = -0.020;

		parameters.projectCar.betaWork = -1.290;

		parameters.car.constantParkingSearchPenalty_min = 4.0;
		parameters.car.constantAccessEgressWalkTime_min = 4.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.016;
		parameters.pt.betaWaitingTime_u_min = -0.011;

		parameters.projectPt.betaRailTravelTime_u_min = -0.008;
		parameters.projectPt.betaBusTravelTime_u_min = -0.014;
		parameters.projectPt.betaFeederTravelTime_u_min = -0.043;

		parameters.projectPt.betaHeadway_u_min = -0.030;
		parameters.projectPt.betaOvgkB_u = -1.784;
		parameters.projectPt.betaOvgkC_u = -1.765;
		parameters.projectPt.betaOvgkD_u = -1.100;
		parameters.projectPt.betaOvgkNone_u = -1.192;

		return parameters;
	}
	
	static public ProjectModeParameters buildDefault() { // 4 Oct
		ProjectModeParameters parameters = new ProjectModeParameters();

		// General
		parameters.betaCost_u_MU = -0.089;

		parameters.lambdaTravelTimeEuclideanDistance = 0.088;
		parameters.lambdaCostEuclideanDistance = -0.225;
		parameters.referenceEuclideanDistance_km = 39.0;

		parameters.lambdaCostHouseholdIncome = -0.826;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Walk
		parameters.walk.alpha_u = 0.504;
		parameters.walk.betaTravelTime_u_min = -0.040 - 0.1;

		// Bike
		parameters.bike.alpha_u = 0.104 - 0.2;
		parameters.bike.betaTravelTime_u_min = -0.119 - 0.1 + 0.03;

		parameters.projectBike.betaAgeOver60 = -2.645;

		// Car
		parameters.car.alpha_u = 0.122 - 0.019 * 4.0 - 0.040 * 4.0;
		parameters.car.betaTravelTime_u_min = -0.019;

		parameters.projectCar.betaWork = -1.296;

		parameters.car.constantParkingSearchPenalty_min = 0.0;
		parameters.car.constantAccessEgressWalkTime_min = 0.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.015;
		parameters.pt.betaWaitingTime_u_min = -0.011;

		parameters.projectPt.betaRailTravelTime_u_min = -0.008;
		parameters.projectPt.betaBusTravelTime_u_min = -0.014;
		parameters.projectPt.betaFeederTravelTime_u_min = -0.042;

		parameters.projectPt.betaHeadway_u_min = -0.030;
		parameters.projectPt.betaOvgkB_u = -1.771;
		parameters.projectPt.betaOvgkC_u = -1.758;
		parameters.projectPt.betaOvgkD_u = -1.093;
		parameters.projectPt.betaOvgkNone_u = -1.194;

		return parameters;
	}
}
