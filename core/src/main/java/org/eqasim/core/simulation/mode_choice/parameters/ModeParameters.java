package org.eqasim.core.simulation.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class ModeParameters implements ParameterDefinition {
	public class CarParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;

		public double constantAccessEgressWalkTime_min = 0.0;
		public double constantParkingSearchPenalty_min = 0.0;
	}

	public class PtParameters {
		public double alpha_u = 0.0;
		public double betaLineSwitch_u = 0.0;
		public double betaInVehicleTime_u_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double betaAccessEgressTime_u_min = 0.0;
	}

	public class BikeParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
		public double betaAgeOver18_u_a = 0.0;
		public double betaNonUrban_u = 0.0;
	}

	public class WalkParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
	}

	public double lambdaCostCrowflyDistance = 0.0;
	public double referenceEuclideanDistance_km = 0.0;

	public double betaCost_u_MU = 0.0;

	public final CarParameters car = new CarParameters();
	public final PtParameters pt = new PtParameters();
	public final BikeParameters bike = new BikeParameters();
	public final WalkParameters walk = new WalkParameters();

	public static ModeParameters buildDefault() {
		ModeParameters parameters = new ModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.126;
		parameters.lambdaCostCrowflyDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 0.827;
		parameters.car.betaTravelTime_u_min = -0.067;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

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

		// Walk
		parameters.walk.alpha_u = 1.3;
		parameters.walk.betaTravelTime_u_min = -0.141;

		return parameters;
	}
}
