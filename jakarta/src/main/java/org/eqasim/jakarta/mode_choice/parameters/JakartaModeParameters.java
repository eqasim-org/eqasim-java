package org.eqasim.jakarta.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class JakartaModeParameters extends ModeParameters {
	public class JakartaWalkParameters {
		public double alpha_age = 0.0;
		
	}
	
	public class JakartaCarParameters {
		//public double alpha_car_city = 0.0;
	}
	
	public class JakartaPTParameters {
	//	public double alpha_pt_city = 0.0;
		public double alpha_age = 0.0;

	}
	
	public class JakartaIncomeElasticity {
		public double lambda_income = 0.0;
	}
	
	public class JakartaAvgHHLIncome {
		public double avg_hhl_income = 0.0;
	}
	
	//public class JakartaTaxiParameters {
	//	public double alpha_taxi_city = 0.0;
	//	public double beta_TravelTime_u_min = 0.0;
	//	
	//	public double betaAccessEgressWalkTime_min = 0.0;
	//	public double betaWaitingTime_u_min = 0.0;
	//	public double alpha_u = 0.0;
	//	
	//	public JakartaTaxiParameters() {
	//		this.alpha_taxi_city = 0.0;
	//	}
	//	
	//}
	
	
	public class JakartaCarodtParameters {
	//	public double alpha_carodt_city = 0.0;
		public double beta_TravelTime_u_min = 0.0;
		
		public double betaAccessEgressWalkTime_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double alpha_u = 0.0;
		public double alpha_sex = 0.0;
		public double alpha_age = 0.0;
		
	//	public JakartaCarodtParameters() {
	//		this.alpha_carodt_city = 0.0;
	//	}
		
	}
	
	
	public class JakartaMcodtParameters {
		//public double alpha_mcodt_city = 0.0;
		public double alpha_age = 0.0;
		public double beta_TravelTime_u_min = 0.0;
		
		public double betaAccessEgressWalkTime_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double alpha_u = 0.0;
		public double alpha_sex = 0.0;
		
	//	public JakartaMcodtParameters() //{
			//this.alpha_mcodt_city = 0.0;
	//	}
		
	}
	
	
	public class JakartaMotorcycleParameters {
	//	public double alpha_motorcycle_city = 0.0;
		public double alpha_age = 0.0;
		public double beta_TravelTime_u_min = 0.0;
		
		public double betaAccessEgressWalkTime_min = 0.0;
		public double betaWaitingTime_u_min = 0.0;
		public double alpha_u = 0.0;
		
	//	public JakartaMotorcycleParameters() {
	//		this.alpha_motorcycle_city = 0.0;
	//	}
		
	}
	
	public final JakartaWalkParameters jWalk = new JakartaWalkParameters();
	public final JakartaPTParameters jPT = new JakartaPTParameters();
	public final JakartaCarParameters jCar = new JakartaCarParameters();
	public final JakartaIncomeElasticity jIncomeElasticity = new JakartaIncomeElasticity();
	public final JakartaAvgHHLIncome jAvgHHLIncome = new JakartaAvgHHLIncome();
	//public final JakartaTaxiParameters jTaxi = new JakartaTaxiParameters();
	public final JakartaCarodtParameters jCarodt = new JakartaCarodtParameters();
	public final JakartaMcodtParameters jMcodt = new JakartaMcodtParameters();
	public final JakartaMotorcycleParameters jMotorcycle = new JakartaMotorcycleParameters();

	public static JakartaModeParameters buildDefault() {
		JakartaModeParameters parameters = new JakartaModeParameters();

		// Cost
		parameters.betaCost_u_MU = -2.08;
		parameters.lambdaCostEuclideanDistance = -0.75;
		parameters.referenceEuclideanDistance_km = 9.19;
        parameters.jIncomeElasticity.lambda_income = -0.06;
        parameters.jAvgHHLIncome.avg_hhl_income = 5331;
        
		// Car
		parameters.car.alpha_u = -1.09;
		parameters.car.betaTravelTime_u_min = -1.24;
        
		parameters.car.constantAccessEgressWalkTime_min = 0.0;
		parameters.car.constantParkingSearchPenalty_min = 0.0;
		//parameters.jCar.alpha_car_city = -0.1597;

		// PT
		parameters.pt.alpha_u = -3.75;
		parameters.pt.betaLineSwitch_u = 0.0;
		parameters.pt.betaInVehicleTime_u_min = -1.49;
		//parameters.pt.betaWaitingTime_u_min = -0.0142;
		//parameters.pt.betaAccessEgressTime_u_min = -0.0142;
		//parameters.jPT.alpha_pt_city = 0.0;
		//parameters.jPT.alpha_age = 0.0;
		
		// Bike
		parameters.bike.alpha_u = -4.24;
		parameters.bike.betaTravelTime_u_min = -9.05;
		//parameters.bike.betaAgeOver18_u_a = 0.0;

		// Walk
		parameters.walk.alpha_u = -2.57;
		parameters.walk.betaTravelTime_u_min = -0.52;
		parameters.jWalk.alpha_age = -0.52;
		//parameters.jWalk.alpha_walk_city = 0.0;
		
		//Carodt
		//parameters.jCarodt.alpha_carodt_city = 0.0;
		
		parameters.jCarodt.beta_TravelTime_u_min = -6.26 ;
		//parameters.jCarodt.betaWaitingTime_u_min = 0.0 ;
		//parameters.jCarodt.betaAccessEgressWalkTime_min = 0.0;
		parameters.jCarodt.alpha_u = -1.23;
		parameters.jCarodt.alpha_sex = -0.42;
		parameters.jCarodt.alpha_age = -1.32;

		//Taxi
		//parameters.jTaxi.alpha_taxi_city = 0.0;		
		//parameters.jTaxi.beta_TravelTime_u_min = 0.0 ;
		//parameters.jTaxi.betaWaitingTime_u_min = 0.0 ;
		//parameters.jTaxi.betaAccessEgressWalkTime_min = 0.0;
		//parameters.jTaxi.alpha_u = 0.0;	
		
		//Mcodt
		//parameters.jMcodt.alpha_mcodt_city = 0.0;				
		parameters.jMcodt.beta_TravelTime_u_min = -6.260 ;
		//parameters.jMcodt.betaWaitingTime_u_min = 0.0 ;
		//parameters.jMcodt.betaAccessEgressWalkTime_min = 0.0;
		parameters.jMcodt.alpha_u = -1.23;
		parameters.jMcodt.alpha_sex = -0.42;
		parameters.jMcodt.alpha_age = -1.32;
		
		
		//Motorcycle
		//parameters.jMcodt.alpha_mcodt_city = 0.0;				
		parameters.jMotorcycle.beta_TravelTime_u_min = -3.32 ;
		//parameters.jMotorcycle.betaWaitingTime_u_min = 0.0 ;
		//parameters.jMotorcycle.betaAccessEgressWalkTime_min = 0.0;
		parameters.jMotorcycle.alpha_u = 0.0;
		parameters.jMotorcycle.alpha_age = -0.83;
		
		
		
		return parameters;
	}
}
