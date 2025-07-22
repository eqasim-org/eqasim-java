package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFParisParameters {
		public double car_u;
		public double carPassenger_u;
		public double bicycle_u;
	}

	public final IDFParisParameters idfParis = new IDFParisParameters();

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
		parameters.betaAccessTime_u_min = -0.032127;

		// Cost
		parameters.betaCost_u_MU = -0.874321;
		parameters.lambdaCostEuclideanDistance = -0.237512;
		parameters.referenceEuclideanDistance_km = 4.42;

		// Car
		parameters.car.alpha_u = -0.833961;
		parameters.car.betaTravelTime_u_min = -0.047915;

		// Car passenger
		parameters.idfCarPassenger.alpha_u = -1.666851;
		parameters.idfCarPassenger.betaDrivingPermit_u = -1.430039;
		parameters.idfCarPassenger.betaInVehicleTravelTime_u_min = -0.102795;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.324087;
		parameters.pt.betaInVehicleTime_u_min = -0.025956;
		parameters.pt.betaWaitingTime_u_min = -0.073963;

		parameters.idfPt.betaDrivingPermit_u = -0.658996;
		parameters.idfPt.onlyBus_u = -1.151128;

		// Bike
		parameters.bike.alpha_u = -3.625289;
		parameters.bike.betaTravelTime_u_min = -0.058303;

		// Walk
		parameters.walk.alpha_u = 1.014904;
		parameters.walk.betaTravelTime_u_min = -0.117956;

		return parameters;
	}
}
