package org.eqasim.quebec.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class QuebecModeParameters extends ModeParameters {
	
	public class QuebecWalkParameters {
		public double alpha_walk = 0.0;
	}
	
	public class QuebecCarParameters {
		public double alpha_car = 0.0;

	}
	
	public class QuebecCarPassengerParameters {
		public double alpha_car_passenger = 0.0;
		public double betaTravelTime_u_min= 0.0;
	}
	
	public class QuebecPTParameters {
		public double alpha_pt = 0.0;
		public double betaCOST_PT= 0.0;
		public double betaTravelTime_u_min= 0.0;

	}

	public final QuebecPTParameters qcPT = new QuebecPTParameters();
	public final QuebecCarParameters qcCar = new QuebecCarParameters();
	public final QuebecCarPassengerParameters car_passenger = new QuebecCarPassengerParameters();

	public static QuebecModeParameters buildDefault() {
		QuebecModeParameters parameters = new QuebecModeParameters();

		// Cost
		parameters.qcPT.betaCOST_PT = -0.7035;

		// Car
		parameters.car.alpha_u = 0.0;
		parameters.car.betaTravelTime_u_min = -0.0871;

		
		// Car Passenger
		parameters.car_passenger.alpha_car_passenger = -1.8225;
		parameters.car_passenger.betaTravelTime_u_min = -0.1243;


		// PT
		parameters.pt.alpha_u = -1.0858;
		parameters.qcPT.betaTravelTime_u_min =  -0.0225;


		// Walk
		parameters.walk.alpha_u = 0.2455;
		parameters.walk.betaTravelTime_u_min = -0.1402 ;
		

		return parameters;
	}
}
