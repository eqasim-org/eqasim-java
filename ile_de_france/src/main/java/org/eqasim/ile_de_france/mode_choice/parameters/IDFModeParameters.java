package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFCarPassengerParameters {
		public double alpha_u;
		public double betaInVehicleTravelTime_u_min;
		public double betaDrivingPermit_u;
	}

	public final IDFCarPassengerParameters idfCarPassenger = new IDFCarPassengerParameters();

	public class IDFPtParameters {
		public double betaDrivingPermit_u;
		public double onlyBus_u;
	}

	public final IDFPtParameters idfPt = new IDFPtParameters();

	public double betaAccessTime_u_min;

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Access
		parameters.betaAccessTime_u_min = -0.0312;

		// Cost
		parameters.betaCost_u_MU = -0.311;
		parameters.lambdaCostEuclideanDistance = -0.258;
		parameters.referenceEuclideanDistance_km = 4.4;

		// Car
		parameters.car.alpha_u = -0.201;
		parameters.car.betaTravelTime_u_min = -0.0424;

		// Car passenger
		parameters.idfCarPassenger.alpha_u = -1.71;
		parameters.idfCarPassenger.betaDrivingPermit_u = -0.835;
		parameters.idfCarPassenger.betaInVehicleTravelTime_u_min = -0.07;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.418;
		parameters.pt.betaInVehicleTime_u_min = -0.0255;
		parameters.pt.betaWaitingTime_u_min = -0.0218;

		parameters.idfPt.betaDrivingPermit_u = -0.531;
		parameters.idfPt.onlyBus_u = -1.42;

		// Bike
		parameters.bike.alpha_u = -2.93;
		parameters.bike.betaTravelTime_u_min = -0.0935;

		// Walk
		parameters.walk.alpha_u = 1.69;
		parameters.walk.betaTravelTime_u_min = -0.162;

		return parameters;
	}
}
