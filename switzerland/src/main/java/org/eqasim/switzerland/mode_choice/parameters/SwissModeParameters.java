package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SwissModeParameters extends ModeParameters {
	public class SwissCarParameters {
		public double betaStatedPreferenceRegion1_u = 0.0;
		public double betaStatedPreferenceRegion3_u = 0.0;

		//new parameters
		public double betaAge = 0.0;
		public double betaIsFemale = 0.0;
		public double betaIsWorkTrip = 0.0;
		public double betaTravelTime_hour = 0.0;

	}

	public class SwissBikeParameters {
		public double betaStatedPreferenceRegion3_u = 0.0;

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
		public double referenceRoutedDistance_km = 0.0;

	}

	public class SwissWalkParameters {
		public double betaAge = 0.0;
		public double betaIsFemale = 0.0;
		public double betaIsWorkTrip = 0.0;
		public double betaTravelTime_hour = 0.0;
	}

	public class SwissPtParameters {
		public double betaAge = 0.0;
		public double betaIsFemale = 0.0;
		public double betaIsWorkTrip = 0.0;
		public double betaInVehicleTime_hour = 0.0;
		public double betaWaitingTime_hour = 0.0;
		public double betaAccessEgressTime_hour = 0.0;
	}

	public double betaCost_RD = 0.0;
	public double lambdaCostRoutedDistance = 0.0;
	public double referenceRoutedDistance_km = 0.0;

	public final SwissCarParameters swissCar = new SwissCarParameters();
	public final SwissBikeParameters swissBike = new SwissBikeParameters();
	public final SwissWalkParameters swissWalk = new SwissWalkParameters();
	public final SwissPtParameters swissPt = new SwissPtParameters();

	public static SwissModeParameters buildDefault() {
		SwissModeParameters parameters = new SwissModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.0; //g/ not used
		parameters.lambdaCostEuclideanDistance = -0.0; //g/ not used
		parameters.referenceEuclideanDistance_km = 0.0; //g/ not used

		parameters.betaCost_RD = -0.711;
		parameters.lambdaCostRoutedDistance = -0.97;
		parameters.referenceRoutedDistance_km = 10.0;

		// Car
		parameters.car.alpha_u = 1.83;
		parameters.car.betaTravelTime_u_min = -0.0; //g/ not used

		parameters.car.constantAccessEgressWalkTime_min = 0.0; //g/ not used
		parameters.car.constantParkingSearchPenalty_min = 0.0; //g/ not used

		parameters.swissCar.betaStatedPreferenceRegion1_u = -0.0; //g/ not used
		parameters.swissCar.betaStatedPreferenceRegion3_u = 0.0; //g/ not used

		parameters.swissCar.betaAge = 0.00736;
		parameters.swissCar.betaIsFemale = -0.192;
		parameters.swissCar.betaIsWorkTrip = -0.246;
		parameters.swissCar.betaTravelTime_hour = -4.67;


		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.0; //<---change
		parameters.pt.betaInVehicleTime_u_min = -0.0; //g/ not used
		parameters.pt.betaWaitingTime_u_min = -0.0; //g/ not used
		parameters.pt.betaAccessEgressTime_u_min = -0.0; //g/ not used

		parameters.swissPt.betaAge = -0.00642;
		parameters.swissPt.betaIsFemale = 0.233;
		parameters.swissPt.betaIsWorkTrip = 0.186;

		parameters.swissPt.betaAccessEgressTime_hour =-1.66;
		parameters.swissPt.betaInVehicleTime_hour =-2.47;
		parameters.swissPt.betaWaitingTime_hour =-1.19;//<---change

		// Bike
		parameters.bike.alpha_u = -0.684;
		parameters.bike.betaTravelTime_u_min = -0.0; //g/ not used
		parameters.bike.betaAgeOver18_u_a = -0.0; //g/ not used
		parameters.swissBike.betaStatedPreferenceRegion3_u = -0.0; //g/ not used

		parameters.swissBike.betaPropS1L1 = 0.206;
		parameters.swissBike.betaPropS2L1 = 0.000514;
		parameters.swissBike.betaPropS3L1 = -0.447;
		parameters.swissBike.betaPropS4L1 = -1.44;
		parameters.swissBike.betaPropS1L2 = -0.452;
		parameters.swissBike.betaPropS2L2 = -1.49;
		parameters.swissBike.betaPropS3L2 = -3.33;
		parameters.swissBike.betaPropS4L2 = -1.29;
		parameters.swissBike.lambdaRCD = 0.0; //g/ no interaction term, use as 0
		parameters.swissBike.referenceRoutedDistance_km = 0.0; //g/ no interaction term, use as 0

		parameters.swissBike.betaAge = -0.000618;
		parameters.swissBike.betaIsFemale = -0.0794;
		parameters.swissBike.betaIsWorkTrip = 0.465;
		parameters.swissBike.betaTravelTime_hour = -6.28;

		// Walk
		parameters.walk.alpha_u = 1.97;
		parameters.walk.betaTravelTime_u_min = -0.0; //g/ not used

		parameters.swissWalk.betaAge = -0.00032;
		parameters.swissWalk.betaIsFemale = 0.039;
		parameters.swissWalk.betaIsWorkTrip = -0.406;
		parameters.swissWalk.betaTravelTime_hour = -8.27;

		return parameters;
	}
}
