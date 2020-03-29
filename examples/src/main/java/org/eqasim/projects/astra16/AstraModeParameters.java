package org.eqasim.projects.astra16;

import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class AstraModeParameters extends SwissModeParameters {
	static public class AstraBaseModeParameters {
		public double betaAgeOver60 = 0.0;
		public double betaWork = 0.0;
		public double betaCity = 0.0;
	}

	public AstraBaseModeParameters astraWalk = new AstraBaseModeParameters();
	public AstraBaseModeParameters astraBike = new AstraBaseModeParameters();
	public AstraBaseModeParameters astraCar = new AstraBaseModeParameters();
	public AstraBaseModeParameters astraAv = new AstraBaseModeParameters();

	public class AstraPtParameters {
		public double betaRailTravelTime_u_min = 0.0;
		public double betaBusTravelTime_u_min = 0.0;
		public double betaFeederTravelTime_u_min = 0.0;

		public double betaHeadway_u_min = 0.0;
		public double betaOvgkB_u = 0.0;
		public double betaOvgkC_u = 0.0;
		public double betaOvgkD_u = 0.0;
		public double betaOvgkNone_u = 0.0;
	}

	public AstraPtParameters astraPt = new AstraPtParameters();

	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public AstraModeParameters buildPrevious() {
		AstraModeParameters parameters = new AstraModeParameters();

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

		parameters.astraWalk.betaAgeOver60 = 2.28;
		parameters.astraWalk.betaWork = 1.13;

		// Bike
		parameters.bike.alpha_u = 0.629 - 0.4;
		parameters.bike.betaTravelTime_u_min = -0.0638 + 0.02;

		parameters.astraBike.betaAgeOver60 = -2.39;
		parameters.astraBike.betaWork = -0.454;

		// Car
		parameters.car.alpha_u = 0.490; // 0.629;
		parameters.car.betaTravelTime_u_min = -0.0291; // -0.0638;

		parameters.astraCar.betaAgeOver60 = 0.258;
		parameters.astraCar.betaWork = -1.06;

		parameters.car.constantParkingSearchPenalty_min = 4.0;
		parameters.car.constantAccessEgressWalkTime_min = 4.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.0751;
		parameters.pt.betaLineSwitch_u = -0.195;
		parameters.pt.betaWaitingTime_u_min = -0.0126;

		parameters.astraPt.betaRailTravelTime_u_min = -0.0154;
		parameters.astraPt.betaBusTravelTime_u_min = -0.0299;
		parameters.astraPt.betaFeederTravelTime_u_min = -0.0582;

		return parameters;
	}

	static public AstraModeParameters buildDefault2() { // 4 Oct
		AstraModeParameters parameters = new AstraModeParameters();

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

		parameters.astraBike.betaAgeOver60 = -2.683;

		// Car
		parameters.car.alpha_u = 0.150;
		parameters.car.betaTravelTime_u_min = -0.020;

		parameters.astraCar.betaWork = -1.290;

		parameters.car.constantParkingSearchPenalty_min = 4.0;
		parameters.car.constantAccessEgressWalkTime_min = 4.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.016;
		parameters.pt.betaWaitingTime_u_min = -0.011;

		parameters.astraPt.betaRailTravelTime_u_min = -0.008;
		parameters.astraPt.betaBusTravelTime_u_min = -0.014;
		parameters.astraPt.betaFeederTravelTime_u_min = -0.043;

		parameters.astraPt.betaHeadway_u_min = -0.030;
		parameters.astraPt.betaOvgkB_u = -1.784;
		parameters.astraPt.betaOvgkC_u = -1.765;
		parameters.astraPt.betaOvgkD_u = -1.100;
		parameters.astraPt.betaOvgkNone_u = -1.192;

		return parameters;
	}

	static public AstraModeParameters buildDefault() { // 4 Oct
		AstraModeParameters parameters = new AstraModeParameters();

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

		parameters.astraBike.betaAgeOver60 = -2.645;

		// Car
		parameters.car.alpha_u = 0.122 - 0.019 * 4.0 - 0.040 * 4.0;
		parameters.car.betaTravelTime_u_min = -0.019;

		parameters.astraCar.betaWork = -1.296;

		// parameters.car.constantParkingSearchPenalty_min = 4.0;
		// parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;
		parameters.car.constantAccessEgressWalkTime_min = 0.0;

		// PT
		parameters.pt.betaAccessEgressTime_u_min = -0.015;
		parameters.pt.betaWaitingTime_u_min = -0.011;

		parameters.astraPt.betaRailTravelTime_u_min = -0.008;
		parameters.astraPt.betaBusTravelTime_u_min = -0.014;
		parameters.astraPt.betaFeederTravelTime_u_min = -0.042;

		parameters.astraPt.betaHeadway_u_min = -0.030;
		parameters.astraPt.betaOvgkB_u = -1.771;
		parameters.astraPt.betaOvgkC_u = -1.758;
		parameters.astraPt.betaOvgkD_u = -1.093;
		parameters.astraPt.betaOvgkNone_u = -1.194;

		return parameters;
	}

	static public AstraModeParameters buildFrom6Feb2020() {
		AstraModeParameters parameters = new AstraModeParameters();

		// General
		parameters.betaCost_u_MU = -0.0888;

		parameters.lambdaCostHouseholdIncome = -0.8169;
		parameters.lambdaCostEuclideanDistance = -0.2209;
		parameters.lambdaTravelTimeEuclideanDistance = 0.1147;

		parameters.referenceEuclideanDistance_km = 39.0;
		parameters.referenceHouseholdIncome_MU = 12260.0;

		// Public transport
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaWaitingTime_u_min = -0.0124;
		parameters.pt.betaAccessEgressTime_u_min = -0.0142;

		parameters.astraPt.betaFeederTravelTime_u_min = -0.0452;
		parameters.astraPt.betaBusTravelTime_u_min = -0.0124;
		parameters.astraPt.betaRailTravelTime_u_min = -0.0072;
		parameters.astraPt.betaHeadway_u_min = -0.0301;

		parameters.astraPt.betaOvgkB_u = -1.7436;
		parameters.astraPt.betaOvgkC_u = -1.6413;
		parameters.astraPt.betaOvgkD_u = -0.9649;
		parameters.astraPt.betaOvgkNone_u = -1.0889;

		// Bicycle
		parameters.bike.alpha_u = 0.1522;
		parameters.bike.betaTravelTime_u_min = -0.1258;

		parameters.astraBike.betaAgeOver60 = -2.6588;

		// Car
		parameters.car.alpha_u = 0.2235;
		parameters.car.betaTravelTime_u_min = -0.0192;

		parameters.astraCar.betaWork = -1.1606;
		parameters.astraCar.betaCity = -0.4590;

		// Walking
		parameters.walk.alpha_u = 0.5903;
		parameters.walk.betaTravelTime_u_min = -0.0457;

		return parameters;
	}
}
