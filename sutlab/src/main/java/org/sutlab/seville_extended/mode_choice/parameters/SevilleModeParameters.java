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

	// Estimated values from working_gk/seville/clean-parameters-2026-10-20.csv
	public static SevilleModeParameters buildDefault() {
		SevilleModeParameters parameters = new SevilleModeParameters();

		// Access (car, car passenger, pt)
		parameters.betaAccessTime_u_min = -0.03619758007023475; // beta_access_time_min

		// Cost (car, pt); the model has no cost-distance interaction
		parameters.betaCost_u_MU = -0.442732051861837; // beta_cost_EUR
		parameters.lambdaCostEuclideanDistance = 0.0;

		// Mean crow-fly distance of the survey trips, reference for the travel time interaction of bike and walk
		parameters.referenceEuclideanDistance_km = 4.11;

		// Car
		parameters.car.alpha_u = 0.5228100921017825; // beta_car_asc
		parameters.car.betaTravelTime_u_min = -0.04976747828545618; // beta_car_in_vehicle_time_min

		parameters.sevilleCar.betaParkingZone_u = -0.9293975682818959; // beta_car_charged
		parameters.sevilleCar.betaSpeed_u_kmh = 0.056537030556507335; // beta_car_speed_kmh
		parameters.sevilleCar.betaDistanceAbove0_u_km = 0.20931800975074388; // beta_car_distance_above_0_km
		parameters.sevilleCar.betaDistanceAbove2_u_km = -0.17344308034119488; // beta_car_distance_above_2_km
		parameters.sevilleCar.betaDistanceAbove5_u_km = -0.42057988265360663; // beta_car_distance_above_5_km
		parameters.sevilleCar.betaDistanceAbove10_u_km = -0.8387145281750858; // beta_car_distance_above_10_km
		parameters.sevilleCar.betaWorkTrip_u = 0.2489523506568171; // beta_car_work_trip
		parameters.sevilleCar.betaEducationTrip_u = -1.0778264603675936; // beta_car_education_trip
		parameters.sevilleCar.betaFemale_u = -0.1763039989437495; // beta_car_female

		// Car passenger
		parameters.carPassenger.alpha_u = -1.3698756729196828; // beta_car_passenger_asc
		parameters.carPassenger.betaInVehicleTravelTime_u_min = -0.12115815024058019; // beta_car_passenger_in_vehicle_time_min

		parameters.sevilleCarPassenger.betaDrivingPermit_u = -0.13412380572301613; // beta_car_passenger_driving_permit
		parameters.sevilleCarPassenger.betaDistanceAbove0_u_km = 0.8561787065969076; // beta_car_passenger_distance_above_0_km
		parameters.sevilleCarPassenger.betaDistanceAbove2_u_km = -0.6069984643596305; // beta_car_passenger_distance_above_2_km
		parameters.sevilleCarPassenger.betaDistanceAbove5_u_km = -0.7043173154847143; // beta_car_passenger_distance_above_5_km
		parameters.sevilleCarPassenger.betaDistanceAbove10_u_km = -0.7617840563796596; // beta_car_passenger_distance_above_10_km
		parameters.sevilleCarPassenger.betaWorkTrip_u = -1.0762109600186036; // beta_car_passenger_work_trip
		parameters.sevilleCarPassenger.betaEducationTrip_u = -1.3494963294173279; // beta_car_passenger_education_trip

		// PT
		parameters.pt.alpha_u = 0.0; // beta_transit_asc, fixed
		parameters.pt.betaLineSwitch_u = -0.24108665630289386; // beta_transit_transfers
		parameters.pt.betaInVehicleTime_u_min = -0.028762168405471703; // beta_transit_in_vehicle_time_total_min
		parameters.pt.betaWaitingTime_u_min = -0.010971734669184892; // beta_transit_waiting_time_min

		parameters.sevillePt.betaDrivingPermit_u = -0.2902164568160196; // beta_transit_driving_permit
		parameters.sevillePt.onlyBus_u = -0.2587839553054978; // beta_transit_only_bus
		parameters.sevillePt.betaOutsideBoundary_u = -1.427348118187231; // beta_transit_leaves_boundary
		parameters.sevillePt.betaDistanceAbove0_u_km = 1.2681020440086195; // beta_transit_distance_above_0_km
		parameters.sevillePt.betaDistanceAbove2_u_km = -1.2855671922824945; // beta_transit_distance_above_2_km
		parameters.sevillePt.betaDistanceAbove5_u_km = -0.44917970242085387; // beta_transit_distance_above_5_km
		parameters.sevillePt.betaDistanceAbove10_u_km = -0.7798846769548126; // beta_transit_distance_above_10_km

		// Bike
		parameters.bike.alpha_u = -0.10890926725198605; // beta_bicycle_asc
		parameters.bike.betaTravelTime_u_min = -0.04185033670237169; // beta_bicycle_travel_time_min

		parameters.sevilleBike.lambdaTravelTimeEuclideanDistance = 1.1882816471732485; // lambda_time_distance_bicycle
		parameters.sevilleBike.betaWorkTrip_u = 0.5372438340594421; // beta_bicycle_work_trip
		parameters.sevilleBike.betaEducationTrip_u = 0.15567491266557276; // beta_bicycle_education_trip
		parameters.sevilleBike.betaFemale_u = -0.757074355228201; // beta_bicycle_female
		parameters.sevilleBike.betaOver65_u = -0.5647495375049817; // beta_bicycle_over_65

		// Walk
		parameters.walk.alpha_u = 3.338924418743773; // beta_walk_asc
		parameters.walk.betaTravelTime_u_min = -0.06164385922390796; // beta_walk_travel_time_min

		parameters.sevilleWalk.lambdaTravelTimeEuclideanDistance = -0.3762165963062983; // lambda_time_distance_walk
		parameters.sevilleWalk.betaWorkTrip_u = -0.6895033908982898; // beta_walk_work_trip
		parameters.sevilleWalk.betaEducationTrip_u = -0.9027753658059083; // beta_walk_education_trip
		parameters.sevilleWalk.betaFemale_u = -0.0013673591810640304; // beta_walk_female
		parameters.sevilleWalk.betaOver65_u = 0.6439169411485912; // beta_walk_over_65

		return parameters;
	}
}
