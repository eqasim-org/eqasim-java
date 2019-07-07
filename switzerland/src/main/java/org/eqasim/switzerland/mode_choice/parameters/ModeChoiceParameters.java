package org.eqasim.switzerland.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class ModeChoiceParameters implements ParameterDefinition {
	public class CarParameters {
		public double alpha = 0.0;
		public double betaTravelTime = 0.0;

		public double constantAccessEgressWalkTime_min = 0.0;
		public double constantParkingSearchPenalty_min = 0.0;

		public double betaStatedPreferenceRegion1 = 0.0;
		public double betaStatedPreferenceRegion3 = 0.0;
	}

	public class PtParameters {
		public double alpha = 0.0;
		public double betaLineSwitch = 0.0;
		public double betaInVehicleTime = 0.0;
		public double betaWaitingTime = 0.0;
		public double betaAccessEgressTime = 0.0;
	}

	public class BikeParameters {
		public double alpha = 0.0;
		public double betaTravelTime = 0.0;
		public double betaAgeOver18 = 0.0;
		public double betaNonUrban = 0.0;

		public double betaStatedPreferenceRegion3 = 0.0;
	}

	public class WalkParameters {
		public double alpha = 0.0;
		public double betaTravelTime = 0.0;
	}

	public double lambdaCostCrowflyDistance = 0.0;
	public double referenceCrowflyDistance_km = 0.0;

	public double betaCost = 0.0;

	public final CarParameters car = new CarParameters();
	public final PtParameters pt = new PtParameters();
	public final BikeParameters bike = new BikeParameters();
	public final WalkParameters walk = new WalkParameters();

	public static ModeChoiceParameters buildDefault() {
		ModeChoiceParameters parameters = new ModeChoiceParameters();

		// Cost
		parameters.betaCost = -0.126;
		parameters.lambdaCostCrowflyDistance = -0.4;
		parameters.referenceCrowflyDistance_km = 40.0;

		// Car
		parameters.car.alpha = 0.827;
		parameters.car.betaTravelTime = -0.067;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		parameters.car.betaStatedPreferenceRegion1 = -0.4;
		parameters.car.betaStatedPreferenceRegion3 = 0.4;

		// PT
		parameters.pt.alpha = 0.0;
		parameters.pt.betaLineSwitch = -0.17;
		parameters.pt.betaInVehicleTime = -0.019;
		parameters.pt.betaWaitingTime = -0.038;
		parameters.pt.betaAccessEgressTime = -0.08;

		// Bike
		parameters.bike.alpha = 0.344;
		parameters.bike.betaTravelTime = -0.09;
		parameters.bike.betaAgeOver18 = -0.049;

		parameters.bike.betaStatedPreferenceRegion3 = -0.366;

		// Walk
		parameters.walk.alpha = 1.3;
		parameters.walk.betaTravelTime = -0.141;

		return parameters;
	}
}
