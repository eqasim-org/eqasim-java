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
		parameters.betaAccessTime_u_min = -0.031239;

		// Cost
		parameters.betaCost_u_MU = -0.310998;
		parameters.lambdaCostEuclideanDistance = -0.257501;
		parameters.referenceEuclideanDistance_km = 4.4;

		// Car
		parameters.car.alpha_u = -0.201465;
		parameters.car.betaTravelTime_u_min = -0.042431;

		// Car passenger
		parameters.idfCarPassenger.alpha_u = -1.713201;
		parameters.idfCarPassenger.betaDrivingPermit_u = -0.835542;
		parameters.idfCarPassenger.betaInVehicleTravelTime_u_min = -0.069976;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.417658;
		parameters.pt.betaInVehicleTime_u_min = -0.025501;
		parameters.pt.betaWaitingTime_u_min = -0.021801;

		parameters.idfPt.betaDrivingPermit_u = -0.531426;
		parameters.idfPt.onlyBus_u = -1.416309;

		// Bike
		parameters.bike.alpha_u = -2.927596;
		parameters.bike.betaTravelTime_u_min = -0.093485;

		// Walk
		parameters.walk.alpha_u = 1.685152;
		parameters.walk.betaTravelTime_u_min = -0.162285;

		return parameters;
	}
}
