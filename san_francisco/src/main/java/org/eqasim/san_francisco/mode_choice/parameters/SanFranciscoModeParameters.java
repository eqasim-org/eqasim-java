package org.eqasim.san_francisco.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SanFranciscoModeParameters extends ModeParameters {
	
	public class SanFranciscoWalkParameters {
		public double alpha_walk_city = 0.0;
	}
	
	public class SanFranciscoPTParameters {
		public double alpha_pt_city = 0.0;
	}
	
	public class SanFranciscoIncomeELasticity {
		public double lambda_income = 0.0;
	}
	
	public final SanFranciscoWalkParameters sfWalk = new SanFranciscoWalkParameters();
	public final SanFranciscoPTParameters sfPT = new SanFranciscoPTParameters();
	public final SanFranciscoIncomeELasticity sfIncomeElasticity = new SanFranciscoIncomeELasticity();

	public static SanFranciscoModeParameters buildDefault() {
		SanFranciscoModeParameters parameters = new SanFranciscoModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.12;
		parameters.lambdaCostEuclideanDistance = 0.0;
		parameters.referenceEuclideanDistance_km = 40.0;
        parameters.sfIncomeElasticity.lambda_income = -1.0;
        
		// Car
		parameters.car.alpha_u = 0.0;
		parameters.car.betaTravelTime_u_min = -0.0347;
        
		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		// PT
		parameters.pt.alpha_u = -2.9636;
		parameters.pt.betaLineSwitch_u = -0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.008;
		parameters.pt.betaWaitingTime_u_min = -0.008;
		parameters.pt.betaAccessEgressTime_u_min = -0.0338;
		parameters.sfPT.alpha_pt_city = 1.9553;
		
		// Bike
		parameters.bike.alpha_u = 0.0;
		parameters.bike.betaTravelTime_u_min = 0.0;
		parameters.bike.betaAgeOver18_u_a = 0.0;

		// Walk
		parameters.walk.alpha_u = -0.6696;
		parameters.walk.betaTravelTime_u_min = -0.0338;
		parameters.sfWalk.alpha_walk_city = 0.9005;
        
		
		return parameters;
	}
}
