package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters;

import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;

public class CorsicaSharingParameters extends IDFModeParameters {
	public class CorsicaShareParameters{
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
		public double betaParkingTime_u_min = 0.0;
		public double betaAccess_Time = 0.0;
		public double betaEgress_Time=0.0;
		public double betaDetour_Time=0.0;
	}

	public CorsicaShareParameters bikeSharing = new CorsicaShareParameters();

	public static CorsicaSharingParameters buildDefault() {
		// This is a copy & paste

		CorsicaSharingParameters parameters = new CorsicaSharingParameters();

		// Cost
		parameters.betaCost_u_MU = -0.206;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 1.35;
		parameters.car.betaTravelTime_u_min = -0.06;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		parameters.idfCar.betaInsideUrbanArea = -0.5;
		parameters.idfCar.betaCrossingUrbanArea = -1.0;

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

		// BikeShare
		parameters.bikeSharing.alpha_u = 0.0;
		parameters.bikeSharing.betaParkingTime_u_min = -0.0484;
		parameters.bikeSharing.betaTravelTime_u_min = -0.017;
		parameters.bikeSharing.betaAccess_Time = -0.004;
		parameters.bikeSharing.betaEgress_Time = -0.003;
		parameters.bikeSharing.betaDetour_Time=-0.003;
		return parameters;
	}
}
