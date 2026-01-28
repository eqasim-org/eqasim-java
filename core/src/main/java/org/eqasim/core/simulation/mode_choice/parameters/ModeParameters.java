package org.eqasim.core.simulation.mode_choice.parameters;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.FileWriter;
import java.lang.reflect.Field;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.contrib.ev.charging.ChargeUpToMaxSocStrategy;
import org.yaml.snakeyaml.Yaml;

public class ModeParameters implements ParameterDefinition {
	public class CarParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;

		public double additionalAccessEgressWalkTime_min = 0.0;
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
	}

	public class WalkParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
	}

	public class DrtParameters {
		public double alpha_u = 0.0;
		public double betaTravelTime_u_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double betaAccessEgressTime_u_min = 0.0;
	}

	public class CarPassengerParameters {
		public double alpha_u;
		public double betaInVehicleTravelTime_u_min;
	}


	public double lambdaCostEuclideanDistance = 0.0;
	public double referenceEuclideanDistance_km = 0.0;

	public double betaCost_u_MU = 0.0;

	public final CarParameters car = new CarParameters();
	public final PtParameters pt = new PtParameters();
	public final BikeParameters bike = new BikeParameters();
	public final WalkParameters walk = new WalkParameters();
	public final DrtParameters drt = new DrtParameters();
	public final CarPassengerParameters carPassenger = new CarPassengerParameters();

	}
