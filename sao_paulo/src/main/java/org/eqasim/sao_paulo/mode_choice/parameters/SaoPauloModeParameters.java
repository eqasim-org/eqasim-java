package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.TaxiVariables;

public class SaoPauloModeParameters extends ModeParameters {
	public class SaoPauloWalkParameters {
		public double alpha_walk_city = 0.0;
	}
	
	public class SaoPauloPTParameters {
		public double alpha_pt_city = 0.0;
	}
	
	public class SaoPauloIncomeElasticity {
		public double lambda_income = 0.0;
	}
	
	public class SaoPauloAvgHHLIncome {
		public double avg_hhl_income = 0.0;
	}
	
	public class SaoPauloTaxiParameters extends TaxiVariables {
		public double alpha_taxi_city = 0.0;
		
		public SaoPauloTaxiParameters() {
			super(0.0,0.0,0.0,0.0);
			this.alpha_taxi_city = 0.0;
		}
		
		public SaoPauloTaxiParameters(double travelTime_min, double cost_MU, double euclideanDistance_km,
				double accessEgressTime_min) {
			super(travelTime_min, cost_MU, euclideanDistance_km, accessEgressTime_min);
			this.alpha_taxi_city = 0.0;
		}
		
	}
	
	public final SaoPauloWalkParameters spWalk = new SaoPauloWalkParameters();
	public final SaoPauloPTParameters spPT = new SaoPauloPTParameters();
	public final SaoPauloIncomeElasticity spIncomeElasticity = new SaoPauloIncomeElasticity();
	public final SaoPauloAvgHHLIncome spAvgHHLIncome = new SaoPauloAvgHHLIncome();
	public final SaoPauloTaxiParameters spTaxi = new SaoPauloTaxiParameters();

	public static SaoPauloModeParameters buildDefault() {
		SaoPauloModeParameters parameters = new SaoPauloModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.12;
		parameters.lambdaCostEuclideanDistance = 0.0;
		parameters.referenceEuclideanDistance_km = 40.0;
        parameters.spIncomeElasticity.lambda_income = -1.0;
        parameters.spAvgHHLIncome.avg_hhl_income = 124108;
		// Car
		parameters.car.alpha_u = 0.0;
		parameters.car.betaTravelTime_u_min = -0.0347;
        
		parameters.car.constantAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;

		// PT
		parameters.pt.alpha_u = -2.2;//-2.9636;
		parameters.pt.betaLineSwitch_u = -0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.008;
		parameters.pt.betaWaitingTime_u_min = -0.008;
		parameters.pt.betaAccessEgressTime_u_min = -0.0338;
		parameters.spPT.alpha_pt_city = 1.9553;
		
		// Bike
		parameters.bike.alpha_u = 0.0;
		parameters.bike.betaTravelTime_u_min = 0.0;
		parameters.bike.betaAgeOver18_u_a = 0.0;

		// Walk
		parameters.walk.alpha_u = -0.2; //-0.66
		parameters.walk.betaTravelTime_u_min = -0.0338;
		parameters.spWalk.alpha_walk_city = 0.9005;
		
		//Taxi
		parameters.spTaxi.alpha_taxi_city = 0.12;
		
		return parameters;
	}
}
