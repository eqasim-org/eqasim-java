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
		parameters.qcPT.betaCOST_PT = -0.7166;

		// Car
		parameters.car.alpha_u = 0.0;
		parameters.car.betaTravelTime_u_min = -0.0790;

		
		// Car Passenger
		parameters.car_passenger.alpha_car_passenger = -1.9889;
		parameters.car_passenger.betaTravelTime_u_min = -0.1123;


		// PT
		parameters.pt.alpha_u = -2.6977;
		parameters.qcPT.betaTravelTime_u_min =  -0.0198;


		// Walk
		parameters.walk.alpha_u = 0.0422;
		parameters.walk.betaTravelTime_u_min = -0.1360 ;
		

		return parameters;
	}
}
