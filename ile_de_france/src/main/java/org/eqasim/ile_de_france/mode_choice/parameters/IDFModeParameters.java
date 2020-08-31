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
		public double betaInsideUrbanArea_u;
		public double betaCrossingUrbanArea_u;
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();
	public final IDFBikeParameters idfBike = new IDFBikeParameters();
	public final IDFPassengerParameters idfPassenger = new IDFPassengerParameters();

	public static IDFModeParameters buildHERE() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.572;
		parameters.lambdaCostEuclideanDistance = -0.289;
		parameters.referenceEuclideanDistance_km = 5.0;

		// Car
		parameters.car.alpha_u = 1.22;
		parameters.car.betaTravelTime_u_min = -0.0595;

		parameters.car.constantAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;

		parameters.idfCar.betaInsideUrbanArea_u = -2.11;
		parameters.idfCar.betaCrossingUrbanArea_u = -1.27;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.953;
		parameters.pt.betaInVehicleTime_u_min = -0.043;
		parameters.pt.betaWaitingTime_u_min = -0.00324;
		parameters.pt.betaAccessEgressTime_u_min = -0.0314;

		// Bike
		parameters.bike.alpha_u = -2.04;
		parameters.bike.betaTravelTime_u_min = -0.0737;

		parameters.idfBike.betaInsideUrbanArea_u = 0.522;

		// Walk
		parameters.walk.alpha_u = 2.72;
		parameters.walk.betaTravelTime_u_min = -0.158;

		// Passenger
		parameters.idfPassenger.alpha_u = -2.08;
		parameters.idfPassenger.betaTravelTime_u_min = -0.113;
		parameters.idfPassenger.betaHouseholdCarAvailability_u = 1.95;
		parameters.idfPassenger.betaLicense_u = -0.848;
		parameters.idfPassenger.betaInsideUrbanArea_u = -1.7;
		parameters.idfPassenger.betaCrossingUrbanArea_u = -0.643;

		return parameters;
	}
	
	public static IDFModeParameters buildBing() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.489;
		parameters.lambdaCostEuclideanDistance = -0.322;
		parameters.referenceEuclideanDistance_km = 5.0;

		// Car
		parameters.car.alpha_u = 1.66;
		parameters.car.betaTravelTime_u_min = -0.0411;

		parameters.car.constantAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;

		parameters.idfCar.betaInsideUrbanArea_u = -2.26;
		parameters.idfCar.betaCrossingUrbanArea_u = -1.17;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.998;
		parameters.pt.betaInVehicleTime_u_min = -0.0261;
		parameters.pt.betaWaitingTime_u_min = -0.0;
		parameters.pt.betaAccessEgressTime_u_min = -0.0221;

		// Bike
		parameters.bike.alpha_u = -1.66;
		parameters.bike.betaTravelTime_u_min = -0.0656;

		parameters.idfBike.betaInsideUrbanArea_u = 0.386;

		// Walk
		parameters.walk.alpha_u = 2.94;
		parameters.walk.betaTravelTime_u_min = -0.156;

		// Passenger
		parameters.idfPassenger.alpha_u = -1.51;
		parameters.idfPassenger.betaTravelTime_u_min = -0.0877;
		parameters.idfPassenger.betaHouseholdCarAvailability_u = 1.85;
		parameters.idfPassenger.betaLicense_u = -0.901;
		parameters.idfPassenger.betaInsideUrbanArea_u = -1.73;
		parameters.idfPassenger.betaCrossingUrbanArea_u = -0.362;

		return parameters;
	}
}
