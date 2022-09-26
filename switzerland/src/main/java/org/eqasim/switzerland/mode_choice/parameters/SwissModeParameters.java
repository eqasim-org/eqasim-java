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
		public double lambdaRCD = 0.0; // RCD is Road Conditions interacted with Distance
		public double referenceRoutedDistance_km = 0.0;

		public double betaAverageUphillGradient = 0.0;
		public double lambdaGD = 0.0; // GD is Gradient interacted with Distance

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

	public double betaCost_RD = 0.0; //RD is routed distance as opposed to euclidean distance
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

		parameters.betaCost_RD = -0.712;
		parameters.lambdaCostRoutedDistance = -0.964;
		parameters.referenceRoutedDistance_km = 10.0;

		// Car
		parameters.car.alpha_u = 1.8;
		parameters.car.betaTravelTime_u_min = -0.0; //g/ not used

		parameters.car.constantAccessEgressWalkTime_min = 0.0; //g/ not used
		parameters.car.constantParkingSearchPenalty_min = 0.0; //g/ not used

		parameters.swissCar.betaStatedPreferenceRegion1_u = -0.0; //g/ not used
		parameters.swissCar.betaStatedPreferenceRegion3_u = 0.0; //g/ not used

		parameters.swissCar.betaAge = 0.00752;
		parameters.swissCar.betaIsFemale = -0.193;
		parameters.swissCar.betaIsWorkTrip = -0.255;
		parameters.swissCar.betaTravelTime_hour = -4.68;


		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.2; //this is "transfers"
		parameters.pt.betaInVehicleTime_u_min = -0.0; //g/ not used
		parameters.pt.betaWaitingTime_u_min = -0.0; //g/ not used
		parameters.pt.betaAccessEgressTime_u_min = -0.0; //g/ not used

		parameters.swissPt.betaAge = -0.00627;
		parameters.swissPt.betaIsFemale = 0.23;
		parameters.swissPt.betaIsWorkTrip = 0.203;

		parameters.swissPt.betaAccessEgressTime_hour =-1.76;
		parameters.swissPt.betaInVehicleTime_hour =-2.36;
		parameters.swissPt.betaWaitingTime_hour =-0.354; //this is "transferTime"

		// Bike
		parameters.bike.alpha_u = -0.707;
		parameters.bike.betaTravelTime_u_min = -0.0; //g/ not used
		parameters.bike.betaAgeOver18_u_a = -0.0; //g/ not used
		parameters.swissBike.betaStatedPreferenceRegion3_u = -0.0; //g/ not used

		parameters.swissBike.betaPropS1L1 = 0.345;
		parameters.swissBike.betaPropS2L1 = 0.169;
		parameters.swissBike.betaPropS3L1 = -0.363;
		parameters.swissBike.betaPropS4L1 = -1.25;
		parameters.swissBike.betaPropS1L2 = 0.0979;
		parameters.swissBike.betaPropS2L2 = -1.07;
		parameters.swissBike.betaPropS3L2 = -2.89;
		parameters.swissBike.betaPropS4L2 = -0.853;
		parameters.swissBike.lambdaRCD = 0.0; //g/ no interaction term, use as 0
		parameters.swissBike.referenceRoutedDistance_km = 0.0; //g/ no interaction term, use as 0

		parameters.swissBike.betaAverageUphillGradient = -9.95;
		parameters.swissBike.lambdaGD = 0.751;

		parameters.swissBike.betaAge = -0.000914;
		parameters.swissBike.betaIsFemale = -0.0768;
		parameters.swissBike.betaIsWorkTrip = 0.46;
		parameters.swissBike.betaTravelTime_hour = -5.26;

		// Walk
		parameters.walk.alpha_u = 2.0;
		parameters.walk.betaTravelTime_u_min = -0.0; //g/ not used

		parameters.swissWalk.betaAge = -0.000337;
		parameters.swissWalk.betaIsFemale = 0.0397;
		parameters.swissWalk.betaIsWorkTrip = -0.409;
		parameters.swissWalk.betaTravelTime_hour = -8.36;

		return parameters;
	}
}
