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
		parameters.lambdaTravelTimeEuclideanDistance = 0.327182;

		// Cost
		parameters.lambdaCostEuclideanDistance = -0.564067;
		parameters.betaCost_u_MU = -0.206368;

		// Car
		parameters.car.alpha_u = -0.183793;
		parameters.car.betaTravelTime_u_min = -0.035702;

		parameters.idfCar.parkingPressure_u = -2.129954;

		// Passenger
		parameters.idfPassenger.alpha_u = -1.642390;
		parameters.idfPassenger.betaDrivingPermit_u = -1.190392;
		parameters.idfPassenger.betaInVehicleTime_u_min = -0.047935;
		parameters.idfPassenger.betaParkingPressure_u = -2.417705;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.012811;
		parameters.pt.betaAccessEgressTime_u_min = -0.029910;
		parameters.pt.betaLineSwitch_u = -0.351704;

		parameters.idfPt.betaDrivingPermit_u = -0.902849;
		parameters.idfPt.betaHeadway_u_min = -0.040625;
		parameters.idfPt.betaOnlyBus_u = -1.125458;
		parameters.idfPt.betaTransferTime_u_min = -0.029910;

		// Bike
		parameters.bike.alpha_u = -3.587379;
		parameters.bike.betaTravelTime_u_min = -0.021655;

		// Walk
		parameters.walk.alpha_u = 0.814819;
		parameters.walk.betaTravelTime_u_min = -0.150038;

		return parameters;
	}
}
