package org.eqasim.examples.auckland_av;

import org.eqasim.auckland.mode_choice.AucklandModeParameters;
import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;

public class AucklandAvModeParameters extends AucklandModeParameters {
	public AvModeParameters av = new AvModeParameters();
	
	static public AucklandAvModeParameters buildDefault() {
		AucklandAvModeParameters parameters = new AucklandAvModeParameters();
		
		// Cost
		parameters.betaCost_u_MU = -0.079; // Adjusted to NZD
		parameters.lambdaCostEuclideanDistance = -0.4;
		parameters.referenceEuclideanDistance_km = 40.0;

		// Car
		parameters.car.alpha_u = 0.827 - 0.8; // Adjusted to Auckland
		parameters.car.betaTravelTime_u_min = -0.067;

		parameters.car.constantAccessEgressWalkTime_min = 4.0;
		parameters.car.constantParkingSearchPenalty_min = 4.0;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.17;
		parameters.pt.betaInVehicleTime_u_min = -0.019;
		parameters.pt.betaWaitingTime_u_min = -0.038;
		parameters.pt.betaAccessEgressTime_u_min = -0.08;

		// Walk
		parameters.walk.alpha_u = 1.3;
		parameters.walk.betaTravelTime_u_min = -0.141;

		// AV
		parameters.av.alpha_u = 0.0;
		parameters.av.betaTravelTime_u_min = -0.019;
		parameters.av.betaWaitingTime_u_min = -0.038;
		
		return parameters;
	}
}
