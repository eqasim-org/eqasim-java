package org.sutlab.seville_extended.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SevilleModeParameters extends ModeParameters {

	public class SevilleCarParameters {
		public double betaParkingZone_u;
		public double betaSpeed_u_kmh;

		public double betaDistanceAbove0_u_km;
		public double betaDistanceAbove2_u_km;
		public double betaDistanceAbove5_u_km;
		public double betaDistanceAbove10_u_km;

		public double betaWorkTrip_u;
		public double betaEducationTrip_u;
		public double betaFemale_u;
	}

	public final SevilleCarParameters sevilleCar = new SevilleCarParameters();

	public class SevilleCarPassengerParameters {
		public double betaDrivingPermit_u;

		public double betaDistanceAbove0_u_km;
		public double betaDistanceAbove2_u_km;
		public double betaDistanceAbove5_u_km;
		public double betaDistanceAbove10_u_km;

		public double betaWorkTrip_u;
		public double betaEducationTrip_u;
	}

	public final SevilleCarPassengerParameters sevilleCarPassenger = new SevilleCarPassengerParameters();

	public class SevillePtParameters {
		public double betaDrivingPermit_u;
		public double onlyBus_u;
		public double betaOutsideBoundary_u;

		public double betaDistanceAbove0_u_km;
		public double betaDistanceAbove2_u_km;
		public double betaDistanceAbove5_u_km;
		public double betaDistanceAbove10_u_km;
	}

	public final SevillePtParameters sevillePt = new SevillePtParameters();

	public class SevilleBikeParameters {
		public double lambdaTravelTimeEuclideanDistance;

		public double betaWorkTrip_u;
		public double betaEducationTrip_u;
		public double betaFemale_u;
		public double betaOver65_u;
	}

	public final SevilleBikeParameters sevilleBike = new SevilleBikeParameters();

	public class SevilleWalkParameters {
		public double lambdaTravelTimeEuclideanDistance;

		public double betaWorkTrip_u;
		public double betaEducationTrip_u;
		public double betaFemale_u;
		public double betaOver65_u;
	}

	public final SevilleWalkParameters sevilleWalk = new SevilleWalkParameters();

	public double betaAccessTime_u_min;

	// Estimated values from no-motorbike-v2.csv (motorbike trips are counted as car)
	public static SevilleModeParameters buildDefault() {
		SevilleModeParameters parameters = new SevilleModeParameters();

		// Access (car, car passenger, pt)
		parameters.betaAccessTime_u_min = -0.0355;

		
		parameters.betaCost_u_MU = -0.43548693272663824; // beta_cost_EUR
		parameters.lambdaCostEuclideanDistance = 0.0; // Cost (car, pt); the model has no cost-distance interaction

		// Mean crow-fly distance of the survey trips, reference for the travel time interaction of bike and walk
		parameters.referenceEuclideanDistance_km = 4.11;

		// Car
		parameters.car.alpha_u = 0.7140;
		parameters.car.betaTravelTime_u_min = -0.0339; // beta_car_in_vehicle_time_min

		parameters.sevilleCar.betaParkingZone_u = -0.8035; // beta_car_charged - whether a car enters the specified parking area
		parameters.sevilleCar.betaSpeed_u_kmh = 0.0592;
		parameters.sevilleCar.betaDistanceAbove0_u_km = -1.1197;
		parameters.sevilleCar.betaDistanceAbove2_u_km = -0.6346;
		parameters.sevilleCar.betaDistanceAbove5_u_km = -0.5583;
		parameters.sevilleCar.betaDistanceAbove10_u_km = -1.5315;
		parameters.sevilleCar.betaWorkTrip_u = 0.3438;
		parameters.sevilleCar.betaEducationTrip_u = -0.9707;
		parameters.sevilleCar.betaFemale_u = -0.5746;

		// Car passenger
		parameters.carPassenger.alpha_u = -0.3180;
		parameters.carPassenger.betaInVehicleTravelTime_u_min = -0.1116;
		parameters.sevilleCarPassenger.betaDrivingPermit_u = -1.1703;
		parameters.sevilleCarPassenger.betaDistanceAbove0_u_km = -0.4791;
		parameters.sevilleCarPassenger.betaDistanceAbove2_u_km = -1.0817;
		parameters.sevilleCarPassenger.betaDistanceAbove5_u_km = -0.7796;
		parameters.sevilleCarPassenger.betaDistanceAbove10_u_km = -1.4894;
		parameters.sevilleCarPassenger.betaWorkTrip_u = -0.7006;
		parameters.sevilleCarPassenger.betaEducationTrip_u = -1.0173;

		// PT
		parameters.pt.alpha_u = 0.0; // fixed
		parameters.pt.betaLineSwitch_u = -0.2425;
		parameters.pt.betaInVehicleTime_u_min = -0.0223;
		parameters.pt.betaWaitingTime_u_min = -0.0097;

		parameters.sevillePt.betaDrivingPermit_u = -0.3405;
		parameters.sevillePt.onlyBus_u = -0.3141;
		parameters.sevillePt.betaOutsideBoundary_u = -1.5742; // beta_transit_leaves_boundary of seville city
		parameters.sevillePt.betaDistanceAbove0_u_km = 0.0; // fixed at 0, normalisation 
		parameters.sevillePt.betaDistanceAbove2_u_km = -1.7424;
		parameters.sevillePt.betaDistanceAbove5_u_km = -0.6123;
		parameters.sevillePt.betaDistanceAbove10_u_km = -1.4960;

		// Bike
		parameters.bike.alpha_u = -0.6213;
		parameters.bike.betaTravelTime_u_min = -0.2944;

		parameters.sevilleBike.lambdaTravelTimeEuclideanDistance = 0.4839;
		parameters.sevilleBike.betaWorkTrip_u = 0.5302;
		parameters.sevilleBike.betaEducationTrip_u = 0.0547;
		parameters.sevilleBike.betaFemale_u = -0.9748;
		parameters.sevilleBike.betaOver65_u = -0.5850;

		// Walk
		parameters.walk.alpha_u = 2.7978;
		parameters.walk.betaTravelTime_u_min = -0.1376;

		parameters.sevilleWalk.lambdaTravelTimeEuclideanDistance = 0.0; // walk time has no distance interaction in the utility equation
		parameters.sevilleWalk.betaWorkTrip_u = -0.7054;
		parameters.sevilleWalk.betaEducationTrip_u = -0.9719;
		parameters.sevilleWalk.betaFemale_u = -0.2323;
		parameters.sevilleWalk.betaOver65_u = 0.6347;

		return parameters;
	}
}
