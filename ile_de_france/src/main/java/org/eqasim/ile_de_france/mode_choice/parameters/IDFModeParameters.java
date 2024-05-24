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
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();
	public final IDFPassengerParameters idfPassenger = new IDFPassengerParameters();
	public final IDFPtParameters idfPt = new IDFPtParameters();

	public double lambdaTravelTimeEuclideanDistance;

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// General
		parameters.referenceEuclideanDistance_km = 4.357986817139488;
		parameters.lambdaTravelTimeEuclideanDistance = 0.271024;

		// Cost
		parameters.lambdaCostEuclideanDistance = 0.0;
		parameters.betaCost_u_MU = -0.233866;

		// Car
		parameters.car.alpha_u = 0.225276;
		parameters.car.betaTravelTime_u_min = -0.030288;

		parameters.idfCar.parkingPressure_u = -2.240005;

		// Passenger
		parameters.idfPassenger.alpha_u = -1.015447;
		parameters.idfPassenger.betaDrivingPermit_u = -1.155232;
		parameters.idfPassenger.betaInVehicleTime_u_min = -0.043256;
		parameters.idfPassenger.betaParkingPressure_u = -2.601795;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.010603;
		parameters.pt.betaAccessEgressTime_u_min = -0.027987;
		parameters.pt.betaLineSwitch_u = -0.330642;
		parameters.pt.betaWaitingTime_u_min = -0.010603; // = IVT

		parameters.idfPt.betaDrivingPermit_u = -0.788438;
		parameters.idfPt.betaHeadway_u_min = 0.0;
		parameters.idfPt.betaOnlyBus_u = -1.338417;

		// Bike
		parameters.bike.alpha_u = -3.023839;
		parameters.bike.betaTravelTime_u_min = -0.021709;

		// Walk
		parameters.walk.alpha_u = 1.379326;
		parameters.walk.betaTravelTime_u_min = -0.150140;

		return parameters;
	}
}
