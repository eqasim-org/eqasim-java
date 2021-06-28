package org.eqasim.examples.corsica_drt.mode_choice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;

public class CorsicaDrtModeParameters extends IDFModeParameters {
	public class ParisDrtParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double betaAccessEgressTime_u_min = 0.0;
	}

	public ParisDrtParameters drt = new ParisDrtParameters();

	public static CorsicaDrtModeParameters buildDefault() {
		// This is a copy & paste

		CorsicaDrtModeParameters parameters = new CorsicaDrtModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.206;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 1.35;
		parameters.car.betaTravelTime_u_min = -0.06;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		parameters.idfCar.betaInsideUrbanArea = -0.5;
		parameters.idfCar.betaCrossingUrbanArea = -1.0;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.017;
		parameters.pt.betaWaitingTime_u_min = -0.0484;
		parameters.pt.betaAccessEgressTime_u_min = -0.0804;

		// Bike
		parameters.bike.alpha_u = -2.0;
		parameters.bike.betaTravelTime_u_min = -0.05;
		parameters.bike.betaAgeOver18_u_a = -0.0496;

		parameters.idfBike.betaInsideUrbanArea = 1.5;

		// Walk
		parameters.walk.alpha_u = 1.43;
		parameters.walk.betaTravelTime_u_min = -0.15;

		// DRT (adapted from public transport)
		parameters.drt.alpha_u = 0.0;
		parameters.drt.betaWaitingTime_u_min = -0.0484;
		parameters.drt.betaTravelTime_u_min = -0.017;
		parameters.drt.betaAccessEgressTime_u_min = -0.0804;

		return parameters;
	}
}
