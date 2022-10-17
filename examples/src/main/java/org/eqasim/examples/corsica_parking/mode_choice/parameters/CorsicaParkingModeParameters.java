package org.eqasim.examples.corsica_parking.mode_choice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;

public class CorsicaParkingModeParameters extends IDFModeParameters {
	public class CarParkingParameters {
		public double betaParkingSearchTime_u_min;
		public double betaParkingCost_u_MU;
	}

	public final CarParkingParameters carParking = new CarParkingParameters();

	public static CorsicaParkingModeParameters buildDefault() {
		CorsicaParkingModeParameters parameters = new CorsicaParkingModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.206;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 1.35;
		parameters.car.betaTravelTime_u_min = -0.06;

		parameters.idfCar.betaInsideUrbanArea = -0.5;
		parameters.idfCar.betaCrossingUrbanArea = -1.0;

		// Car parking parameters
		parameters.carParking.betaParkingSearchTime_u_min = -0.06;
		parameters.carParking.betaParkingCost_u_MU = -0.206;

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

		return parameters;
	}
}
