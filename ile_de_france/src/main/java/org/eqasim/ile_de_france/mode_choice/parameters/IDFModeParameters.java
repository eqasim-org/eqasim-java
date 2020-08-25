package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFCarParameters {
		public double betaInsideUrbanArea_u;
		public double betaCrossingUrbanArea_u;
	}

	public class IDFBikeParameters {
		public double betaInsideUrbanArea_u;
	}

	public class IDFPassengerParameters {
		public double alpha_u;
		public double betaTravelTime_u_min;
		public double betaHouseholdCarAvailability_u;
		public double betaLicense_u;
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();
	public final IDFBikeParameters idfBike = new IDFBikeParameters();
	public final IDFPassengerParameters idfPassenger = new IDFPassengerParameters();

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.584;
		parameters.lambdaCostEuclideanDistance = -0.278;
		parameters.referenceEuclideanDistance_km = 5.0;

		// Car
		parameters.car.alpha_u = 1.05;
		parameters.car.betaTravelTime_u_min = -0.0687;

		parameters.car.constantAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;

		parameters.idfCar.betaInsideUrbanArea_u = -1.86;
		parameters.idfCar.betaCrossingUrbanArea_u = -1.06;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.96;
		parameters.pt.betaInVehicleTime_u_min = -0.0507;
		parameters.pt.betaWaitingTime_u_min = -0.00462;
		parameters.pt.betaAccessEgressTime_u_min = -0.037;

		// Bike
		parameters.bike.alpha_u = -2.23;
		parameters.bike.betaTravelTime_u_min = -0.0767;

		parameters.idfBike.betaInsideUrbanArea_u = 0.707;

		// Walk
		parameters.walk.alpha_u = 2.57;
		parameters.walk.betaTravelTime_u_min = -0.158;

		// Passenger
		parameters.idfPassenger.alpha_u = -2.46;
		parameters.idfPassenger.betaTravelTime_u_min = -0.125;
		parameters.idfPassenger.betaHouseholdCarAvailability_u = 2.09;
		parameters.idfPassenger.betaLicense_u = -0.969;

		return parameters;
	}
}
