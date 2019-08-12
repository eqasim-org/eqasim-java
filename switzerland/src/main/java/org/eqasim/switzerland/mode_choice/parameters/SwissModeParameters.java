package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SwissModeParameters extends ModeParameters {
	public class BikeParameters extends ModeParameters.BikeParameters {
		public double betaStatedPreferenceRegion3_u = 0.0;
	}

	public class CarParameters extends ModeParameters.CarParameters {
		public double betaStatedPreferenceRegion1_u = 0.0;
		public double betaStatedPreferenceRegion3_u = 0.0;
	}

	public final CarParameters car = new CarParameters();
	public final BikeParameters bike = new BikeParameters();

	public static SwissModeParameters buildDefault() {
		SwissModeParameters parameters = new SwissModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.126;
		parameters.lambdaCostCrowflyDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 0.827;
		parameters.car.betaTravelTime_u_min = -0.067;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		parameters.car.betaStatedPreferenceRegion1_u = -0.4;
		parameters.car.betaStatedPreferenceRegion3_u = 0.4;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.019;
		parameters.pt.betaWaitingTime_u_min = -0.038;
		parameters.pt.betaAccessEgressTime_u_min = -0.08;

		// Bike
		parameters.bike.alpha_u = 0.344;
		parameters.bike.betaTravelTime_u_min = -0.09;
		parameters.bike.betaAgeOver18_u_a = -0.049;

		parameters.bike.betaStatedPreferenceRegion3_u = -0.366;

		// Walk
		parameters.walk.alpha_u = 1.3;
		parameters.walk.betaTravelTime_u_min = -0.141;

		return parameters;
	}
}
