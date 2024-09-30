package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFCarParameters {
		public double betaInVehicleTravelTime_u_min;
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();
	
	public class IDFPassengerParameters {
		public double alpha_u;
		public double betaInVehicleTravelTime_u_min;
		public double betaDrivingPermit_u;
	}

	public final IDFPassengerParameters idfPassenger = new IDFPassengerParameters();

	public class IDFMotorbikeParameters {
		public double alpha_u;
		public double betaInVehicleTravelTime_u_min;
	}

	public final IDFMotorbikeParameters idfMotorbike = new IDFMotorbikeParameters();
	
	public class IDFPtParameters {
		public double betaDrivingPermit_u;
		public double onlyBus_u;
	}

	public final IDFPtParameters idfPt = new IDFPtParameters();
	
	public double betaAccessTime_u_min;

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Access
		parameters.betaAccessTime_u_min = -0.0313;
		
		// Cost
		parameters.betaCost_u_MU = -0.474;
		parameters.lambdaCostEuclideanDistance = -0.274;
		parameters.referenceEuclideanDistance_km = 4.4;

		// Car
		parameters.car.alpha_u = -0.662;
		parameters.car.betaTravelTime_u_min = -0.0262;
		
		parameters.idfCar.betaInVehicleTravelTime_u_min = -0.0262;
		
		// Car passenger
		parameters.idfPassenger.alpha_u = -2.08;
		parameters.idfPassenger.betaDrivingPermit_u = -1.17;
		parameters.idfPassenger.betaInVehicleTravelTime_u_min = -0.0608;

		// Motorbike
		parameters.idfMotorbike.alpha_u = -2.2;
		parameters.idfMotorbike.betaInVehicleTravelTime_u_min = -0.0321;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.452;
		parameters.pt.betaInVehicleTime_u_min = -0.0201;
		parameters.pt.betaWaitingTime_u_min = -0.0191;
		parameters.pt.betaAccessEgressTime_u_min = parameters.betaAccessTime_u_min;

		parameters.idfPt.betaDrivingPermit_u = -0.712;
		parameters.idfPt.onlyBus_u = -1.42;
		
		// Bike
		parameters.bike.alpha_u = -3.42;
		parameters.bike.betaTravelTime_u_min = -0.0824;

		// Walk
		parameters.walk.alpha_u = 1.2;
		parameters.walk.betaTravelTime_u_min = -0.158;

		return parameters;
	}
}
