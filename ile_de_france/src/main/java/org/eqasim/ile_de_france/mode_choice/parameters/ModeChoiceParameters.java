package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class ModeChoiceParameters implements ParameterDefinition {
	public class CarParameters {
		public double alpha = 0.0;
		public double betaTravelTime = 0.0;

		public double constantAccessEgressWalkTime_min = 0.0;
		public double constantParkingSearchPenalty_min = 0.0;
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
		parameters.betaCost = -0.206;
		parameters.lambdaCostCrowflyDistance = -0.4;
		parameters.referenceCrowflyDistance_km = 40.0;

		// Car
		parameters.car.alpha = 0.827;
		parameters.car.betaTravelTime = -0.0667;

		parameters.car.constantAccessEgressWalkTime_min = 5.0;
		parameters.car.constantParkingSearchPenalty_min = 6.0;

		// PT
		parameters.pt.alpha = 0.0;
		parameters.pt.betaLineSwitch = -0.17;
		parameters.pt.betaInVehicleTime = -0.0192;
		parameters.pt.betaWaitingTime = -0.0384;
		parameters.pt.betaAccessEgressTime = -0.0804;

		// Bike
		parameters.bike.alpha = -0.1;
		parameters.bike.betaTravelTime = -0.0805;
		parameters.bike.betaAgeOver18 = -0.0496;

		// Walk
		parameters.walk.alpha = 0.63;
		parameters.walk.betaTravelTime = -0.141;

		return parameters;
	}
}
