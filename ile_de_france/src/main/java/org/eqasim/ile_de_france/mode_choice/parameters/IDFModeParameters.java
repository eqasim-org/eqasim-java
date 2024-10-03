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
		parameters.betaAccessTime_u_min = -0.0318;

		// Cost
		parameters.betaCost_u_MU = -0.425;
		parameters.lambdaCostEuclideanDistance = -0.268;
		parameters.referenceEuclideanDistance_km = 4.4;

		// Car
		parameters.car.alpha_u = -0.613;
		parameters.car.betaTravelTime_u_min = -0.0285;

		// Car passenger
		parameters.idfCarPassenger.alpha_u = -2.04;
		parameters.idfCarPassenger.betaDrivingPermit_u = -1.16;
		parameters.idfCarPassenger.betaInVehicleTravelTime_u_min = -0.0605;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.457;
		parameters.pt.betaInVehicleTime_u_min = -0.0195;
		parameters.pt.betaWaitingTime_u_min = -0.0195;
		parameters.pt.betaAccessEgressTime_u_min = parameters.betaAccessTime_u_min;

		parameters.idfPt.betaDrivingPermit_u = -0.722;
		parameters.idfPt.onlyBus_u = -1.43;

		// Bike
		parameters.bike.alpha_u = -3.39;
		parameters.bike.betaTravelTime_u_min = -0.0823;

		// Walk
		parameters.walk.alpha_u = 1.24;
		parameters.walk.betaTravelTime_u_min = -0.158;

		return parameters;
	}
}
