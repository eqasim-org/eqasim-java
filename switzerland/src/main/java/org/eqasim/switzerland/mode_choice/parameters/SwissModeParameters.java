package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SwissModeParameters extends ModeParameters {
	public class SwissCarParameters {
		public double betaStatedPreferenceRegion1_u = 0.0;
		public double betaStatedPreferenceRegion3_u = 0.0;
	}

	public class SwissBikeParameters {
		public double betaStatedPreferenceRegion3_u = 0.0;

		//new
		public double betaAge = 0.0;
		public double betaIsFemale = 0.0;
		public double betaIsWorkTrip = 0.0;
		public double betaTravelTime_hour = 0.0;
		public double betaPropS1L1 = 0.0;
		public double betaPropS2L1 = 0.0;
		public double betaPropS3L1 = 0.0;
		public double betaPropS4L1 = 0.0;
		public double betaPropS1L2 = 0.0;
		public double betaPropS2L2 = 0.0;
		public double betaPropS3L2 = 0.0;
		public double betaPropS4L2 = 0.0;
		public double lambdaRCD = 0.0;
		public double referenceDist = 0.0;

	}

	public final SwissCarParameters swissCar = new SwissCarParameters();
	public final SwissBikeParameters swissBike = new SwissBikeParameters();

	public static SwissModeParameters buildDefault() {
		SwissModeParameters parameters = new SwissModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.126;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 0.827;
		parameters.car.betaTravelTime_u_min = -0.067;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		parameters.swissCar.betaStatedPreferenceRegion1_u = -0.4;
		parameters.swissCar.betaStatedPreferenceRegion3_u = 0.4;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.019;
		parameters.pt.betaWaitingTime_u_min = -0.038;
		parameters.pt.betaAccessEgressTime_u_min = -0.08;

		// Bike
		parameters.bike.alpha_u = 0.344;
		parameters.bike.betaTravelTime_u_min = -0.09; //g/ what does u_ mean?
		parameters.bike.betaAgeOver18_u_a = -0.049;

		parameters.swissBike.betaStatedPreferenceRegion3_u = -0.366;

		//new
		parameters.swissBike.betaAge = 0.0;
		parameters.swissBike.betaIsFemale = 0.0;
		parameters.swissBike.betaIsWorkTrip = 0.0;
		parameters.swissBike.betaTravelTime_hour = 0.0;
		parameters.swissBike.betaPropS1L1 = 0.0;
		parameters.swissBike.betaPropS2L1 = 0.0;
		parameters.swissBike.betaPropS3L1 = 0.0;
		parameters.swissBike.betaPropS4L1 = 0.0;
		parameters.swissBike.betaPropS1L2 = 0.0;
		parameters.swissBike.betaPropS2L2 = 0.0;
		parameters.swissBike.betaPropS3L2 = 0.0;
		parameters.swissBike.betaPropS4L2 = 0.0;
		parameters.swissBike.lambdaRCD = 0.0;
		parameters.swissBike.referenceDist = 0.0;

		// Walk
		parameters.walk.alpha_u = 1.3;
		parameters.walk.betaTravelTime_u_min = -0.141;

		return parameters;
	}
}
