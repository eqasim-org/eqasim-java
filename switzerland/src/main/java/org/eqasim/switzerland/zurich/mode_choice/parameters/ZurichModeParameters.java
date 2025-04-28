package org.eqasim.switzerland.zurich.mode_choice.parameters;

import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;

public class ZurichModeParameters extends SwissModeParameters {
	static public class ZurichBaseModeParameters {
		public double betaAgeOver60 = 0.0;
		public double betaWork = 0.0;
		public double betaCity = 0.0;

		public double travelTimeThreshold_min = 0.0;
	}

	public ZurichBaseModeParameters ZurichWalk = new ZurichBaseModeParameters();
	public ZurichBaseModeParameters ZurichBike = new ZurichBaseModeParameters();
	public ZurichBaseModeParameters ZurichCar = new ZurichBaseModeParameters();
	public ZurichBaseModeParameters ZurichAv = new ZurichBaseModeParameters();

	public class ZurichPtParameters {
		public double betaRailTravelTime_u_min = 0.0;
		public double betaBusTravelTime_u_min = 0.0;
		public double betaFeederTravelTime_u_min = 0.0;

		public double betaHeadway_u_min = 0.0;
		public double betaOvgkB_u = 0.0;
		public double betaOvgkC_u = 0.0;
		public double betaOvgkD_u = 0.0;
		public double betaOvgkNone_u = 0.0;
	}

	public ZurichPtParameters ZurichPt = new ZurichPtParameters();

	public double lambdaTravelTimeEuclideanDistance = 0.0;
	public double lambdaCostHouseholdIncome = 0.0;
	public double referenceHouseholdIncome_MU = 0.0;

	static public ZurichModeParameters buildFrom6Feb2020() {
		ZurichModeParameters parameters = new ZurichModeParameters();

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

		parameters.ZurichPt.betaFeederTravelTime_u_min = -0.0452;
		parameters.ZurichPt.betaBusTravelTime_u_min = -0.0124;
		parameters.ZurichPt.betaRailTravelTime_u_min = -0.0072;
		parameters.ZurichPt.betaHeadway_u_min = -0.0301;

		parameters.ZurichPt.betaOvgkB_u = -1.7436;
		parameters.ZurichPt.betaOvgkC_u = -1.6413;
		parameters.ZurichPt.betaOvgkD_u = -0.9649;
		parameters.ZurichPt.betaOvgkNone_u = -1.0889;

		// Bicycle
		parameters.bike.alpha_u = 0.1522;
		parameters.bike.betaTravelTime_u_min = -0.1258;

		parameters.ZurichBike.betaAgeOver60 = -2.6588;

		// Car
		parameters.car.alpha_u = -0.6; // Original from fb model: 0.2235;
		parameters.car.betaTravelTime_u_min = -0.0192;

		parameters.ZurichCar.betaWork = -1.1606;
		parameters.ZurichCar.betaCity = -0.4590;

		// Walking
		parameters.walk.alpha_u = 0.5903;
		parameters.walk.betaTravelTime_u_min = -0.0457;

		parameters.ZurichWalk.travelTimeThreshold_min = 120.0;

		return parameters;
	}
}