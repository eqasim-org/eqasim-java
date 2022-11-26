package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFCarParameters {
		public double parkingPressure_u;
	}

	public class IDFPassengerParameters {
		public double alpha_u;
		public double betaInVehicleTime_u_min;
		public double betaDrivingPermit_u;
		public double betaParkingPressure_u;
	}

	public class IDFPtParameters {
		public double betaDrivingPermit_u;
		public double betaHeadway_u_min;
		public double betaOnlyBus_u;
		public double betaTransferTime_u_min;
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();
	public final IDFPassengerParameters idfPassenger = new IDFPassengerParameters();
	public final IDFPtParameters idfPt = new IDFPtParameters();

	public double lambdaTravelTimeEuclideanDistance;

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// General
		parameters.referenceEuclideanDistance_km = 8.0;
		parameters.lambdaTravelTimeEuclideanDistance = 0.327997;

		// Cost
		parameters.lambdaCostEuclideanDistance = -0.590910;
		parameters.betaCost_u_MU = -0.189767;

		// Car
		parameters.car.alpha_u = -0.793284;
		parameters.car.betaTravelTime_u_min = -0.793284;

		parameters.idfCar.parkingPressure_u = -1.966132;

		// Passenger
		parameters.idfPassenger.alpha_u = -1.688465;
		parameters.idfPassenger.betaDrivingPermit_u = -1.853586;
		parameters.idfPassenger.betaInVehicleTime_u_min = -0.045423;
		parameters.idfPassenger.betaParkingPressure_u = -2.532017;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.012814;
		parameters.pt.betaAccessEgressTime_u_min = -0.030250;
		parameters.pt.betaLineSwitch_u = -0.338746;

		parameters.idfPt.betaDrivingPermit_u = -1.697751;
		parameters.idfPt.betaHeadway_u_min = -0.038510;
		parameters.idfPt.betaOnlyBus_u = -1.119205;
		parameters.idfPt.betaTransferTime_u_min = -0.030250;

		// Bike
		parameters.bike.alpha_u = -3.916648;
		parameters.bike.betaTravelTime_u_min = -0.022654;

		// Walk
		parameters.walk.alpha_u = 0.469545;
		parameters.walk.betaTravelTime_u_min = -0.155547;

		return parameters;
	}
}
