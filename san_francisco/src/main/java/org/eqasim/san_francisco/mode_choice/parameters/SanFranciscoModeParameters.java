package org.eqasim.san_francisco.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SanFranciscoModeParameters extends ModeParameters {
	
	public class SanFranciscoWalkParameters {
		public double alpha_walk_city = 0.0;
	}
	
	public class SanFranciscoPTParameters {
		public double alpha_pt_city = 0.0;
	}
	
	public final SanFranciscoWalkParameters sfWalk = new SanFranciscoWalkParameters();
	public final SanFranciscoPTParameters sfPT = new SanFranciscoPTParameters();

	public static SanFranciscoModeParameters buildDefault() {
		SanFranciscoModeParameters parameters = new SanFranciscoModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.167;
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 1.35;
		parameters.car.betaTravelTime_u_min = -0.06;
        
		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.017;
		parameters.pt.betaWaitingTime_u_min = -0.0484;
		parameters.pt.betaAccessEgressTime_u_min = -0.0804;
		parameters.sfPT.alpha_pt_city = 1.9553;
		
		// Bike
		parameters.bike.alpha_u = 0.0;
		parameters.bike.betaTravelTime_u_min = -0.15;
		parameters.bike.betaAgeOver18_u_a = -0.0496;

		// Walk
		parameters.walk.alpha_u = 1.43;
		parameters.walk.betaTravelTime_u_min = -0.09;
		parameters.sfWalk.alpha_walk_city = 0.9005;
        
		
		return parameters;
	}
}
