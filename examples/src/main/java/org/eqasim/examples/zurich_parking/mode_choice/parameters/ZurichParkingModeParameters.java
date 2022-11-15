package org.eqasim.examples.zurich_parking.mode_choice.parameters;

import org.eqasim.switzerland.mode_choice.parameters.SwissModeParameters;

public class ZurichParkingModeParameters extends SwissModeParameters {
	public class CarParkingParameters {
		public double betaParkingSearchTime_u_min;
		public double betaParkingCost_u_MU;
	}

	public final CarParkingParameters carParking = new CarParkingParameters();

	public static ZurichParkingModeParameters buildDefault() {
		ZurichParkingModeParameters parameters = new ZurichParkingModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.126;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 0.827;
		parameters.car.betaTravelTime_u_min = -0.067;

//		parameters.car.constantAccessEgressWalkTime_min = 4.0;
//		parameters.car.constantParkingSearchPenalty_min = 4.0;

		// Sebastian thesis version
		parameters.car.constantAccessEgressWalkTime_min = 5.0;
		parameters.car.constantParkingSearchPenalty_min = 6.0;

		// Car parking parameters
		parameters.carParking.betaParkingSearchTime_u_min = -0.067;
		parameters.carParking.betaParkingCost_u_MU = -0.126;

		parameters.swissCar.betaStatedPreferenceRegion1_u = -0.4;
		parameters.swissCar.betaStatedPreferenceRegion3_u = 0.4;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.019;
		parameters.pt.betaWaitingTime_u_min = -0.038;
		parameters.pt.betaAccessEgressTime_u_min = -0.08;

//		// Bike
//		parameters.bike.alpha_u = 0.344;
//		parameters.bike.betaTravelTime_u_min = -0.09;
//		parameters.bike.betaAgeOver18_u_a = -0.049;

		// Bike: Sebastian thesis version
		parameters.bike.alpha_u = -0.1;
		parameters.bike.betaTravelTime_u_min = -0.081;
		parameters.bike.betaAgeOver18_u_a = -0.049;

		parameters.swissBike.betaStatedPreferenceRegion3_u = -0.366;

//		// Walk
//		parameters.walk.alpha_u = 1.3;
//		parameters.walk.betaTravelTime_u_min = -0.141;

		// Walk: Sebastian thesis version
		parameters.walk.alpha_u = 0.63;
		parameters.walk.betaTravelTime_u_min = -0.141;

		return parameters;
	}
}
