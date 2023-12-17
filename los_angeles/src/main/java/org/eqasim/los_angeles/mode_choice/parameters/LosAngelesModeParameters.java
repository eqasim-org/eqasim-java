package org.eqasim.los_angeles.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class LosAngelesModeParameters extends ModeParameters {
	
	public class LosAngelesWalkParameters {
		public double alpha_walk_city = 0.0;
		public double vot_min = 0.0;
	}
	
	public class LosAngelesPTParameters {
		public double alpha_pt_city = 0.0;
		public double vot_min = 0.0;
		public double alpha_orange_county = 0.0;
	}
	
	public class LosAngelesCarParameters {
		public double vot_min = 0.0;
	}
	
	public class LosAngelesIncomeElasticity {
		public double lambda_income = 0.0;
	}
	
	public class LosAngelesAvgHHLIncome {
		public double avg_hhl_income = 0.0;
	}
	
	public final LosAngelesWalkParameters laWalk = new LosAngelesWalkParameters();
	public final LosAngelesPTParameters laPT = new LosAngelesPTParameters();
	public final LosAngelesCarParameters laCar = new LosAngelesCarParameters();
	public final LosAngelesIncomeElasticity laIncomeElasticity = new LosAngelesIncomeElasticity();
	public final LosAngelesAvgHHLIncome laAvgHHLIncome = new LosAngelesAvgHHLIncome();

	public static LosAngelesModeParameters buildDefault() {
		LosAngelesModeParameters parameters = new LosAngelesModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.0189;
		parameters.lambdaCostEuclideanDistance = 0.0;
		parameters.referenceEuclideanDistance_km = 40.0;
        parameters.laIncomeElasticity.lambda_income = -1.0;
        parameters.laAvgHHLIncome.avg_hhl_income = 83786;

        // Car
		parameters.car.alpha_u = 0.0;       
		parameters.car.additionalAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;
		parameters.laCar.vot_min = 0.1618;
		
		// PT
		parameters.pt.alpha_u = -1.7;//-3.07;
		parameters.pt.betaLineSwitch_u = 1.33;
		parameters.pt.betaWaitingTime_u_min = -0.0;
		parameters.pt.betaAccessEgressTime_u_min = -0.0;
		parameters.laPT.alpha_pt_city = 1.4;//1.3359;
		parameters.laPT.vot_min = 0.0331;
		parameters.laPT.alpha_orange_county  = -1.4;//0.0

		// Bike
		parameters.bike.alpha_u = 0.0;
		parameters.bike.betaTravelTime_u_min = 0.0;
		parameters.bike.betaAgeOver18_u_a = 0.0;

		// Walk
		parameters.walk.alpha_u = -0.3;//-0.9181;
		parameters.laWalk.vot_min = 0.0685;
		parameters.laWalk.alpha_walk_city = 0.7;//0.4016;
        
		
		return parameters;
	}
}
