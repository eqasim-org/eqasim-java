package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SaoPauloModeParameters extends ModeParameters {
	public class SaoPauloWalkParameters {
		public double alpha_walk_city = 0.0;
	}
	
	public class SaoPauloCarParameters {
		public double alpha_car_city = 0.0;
	}
	
	public class SaoPauloPTParameters {
		public double alpha_pt_city = 0.0;
		public double alpha_age = 0.0;

	}
	
	public class SaoPauloIncomeElasticity {
		public double lambda_income = 0.0;
	}
	
	public class SaoPauloAvgHHLIncome {
		public double avg_hhl_income = 0.0;
	}
	
	public class SaoPauloTaxiParameters {
		public double alpha_taxi_city = 0.0;
		public double beta_TravelTime_u_min = 0.0;
		
		public double betaAccessEgressWalkTime_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double alpha_u = 0.0;
		
		public SaoPauloTaxiParameters() {
			this.alpha_taxi_city = 0.0;
		}
		
	}
	
	public final SaoPauloWalkParameters spWalk = new SaoPauloWalkParameters();
	public final SaoPauloPTParameters spPT = new SaoPauloPTParameters();
	public final SaoPauloCarParameters spCar = new SaoPauloCarParameters();
	public final SaoPauloIncomeElasticity spIncomeElasticity = new SaoPauloIncomeElasticity();
	public final SaoPauloAvgHHLIncome spAvgHHLIncome = new SaoPauloAvgHHLIncome();
	public final SaoPauloTaxiParameters spTaxi = new SaoPauloTaxiParameters();

	public static SaoPauloModeParameters buildDefault() {
		SaoPauloModeParameters parameters = new SaoPauloModeParameters();

		// Cost
		parameters.betaCost_u_MU = -0.0606;
		parameters.lambdaCostEuclideanDistance = 0.0;
		parameters.referenceEuclideanDistance_km = 40.0;
        parameters.spIncomeElasticity.lambda_income = -0.2019;
        parameters.spAvgHHLIncome.avg_hhl_income = 4215;
        
		// Car
		parameters.car.alpha_u = 0.0;
		parameters.car.betaTravelTime_u_min = -0.0246;
        
		parameters.car.additionalAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;
		parameters.spCar.alpha_car_city = -0.1597;

		// PT
		parameters.pt.alpha_u = -0.2;
		parameters.pt.betaLineSwitch_u = 0.0;
		parameters.pt.betaInVehicleTime_u_min = -0.0142;
		parameters.pt.betaWaitingTime_u_min = -0.0142;
		parameters.pt.betaAccessEgressTime_u_min = -0.0142;
		parameters.spPT.alpha_pt_city = 0.0;
		parameters.spPT.alpha_age = 0.0;
		// Bike
		parameters.bike.alpha_u = 0.0;
		parameters.bike.betaTravelTime_u_min = 0.0;
		parameters.bike.betaAgeOver18_u_a = 0.0;

		// Walk
		parameters.walk.alpha_u = 2.2;
		parameters.walk.betaTravelTime_u_min = -0.1257;
		parameters.spWalk.alpha_walk_city = 0.0;
		
		//Taxi
		parameters.spTaxi.alpha_taxi_city = 0.0;
		
		parameters.spTaxi.beta_TravelTime_u_min = -0.15;
		parameters.spTaxi.betaWaitingTime_u_min = parameters.pt.betaWaitingTime_u_min;
		parameters.spTaxi.betaAccessEgressWalkTime_min = parameters.pt.betaAccessEgressTime_u_min;
		parameters.spTaxi.alpha_u = -3.0;
		
		return parameters;
	}
}
