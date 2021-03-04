package org.eqasim.sao_paulo.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class SaoPauloCostParameters implements ParameterDefinition {
	
	public double carCost_BRL_km;	
	
	public double ptCostPerTrip_0Transfers_BRL;
	public double ptCostPerTrip_3Transfers_BRL;
	
	public double taxiPickUpFee_BRL;
	public double taxiCostPerMin_BRL;
	public double taxiCostPerkm_BRL;

	public double taxMinCost_BRL;

	public static SaoPauloCostParameters buildDefault() {
		SaoPauloCostParameters parameters = new SaoPauloCostParameters();

		parameters.carCost_BRL_km = 0.51;
		
		parameters.ptCostPerTrip_0Transfers_BRL = 4.3;
		parameters.ptCostPerTrip_3Transfers_BRL = 7.48;
		
		// it is assumed a 2 person occupancy of taxi services
		parameters.taxiPickUpFee_BRL = 2.08;//4.16;
		parameters.taxiCostPerMin_BRL = 0.24;//0.48;
		parameters.taxiCostPerkm_BRL = 1.2;//2.4;
		parameters.taxMinCost_BRL = 7.0;


		return parameters;
	}
}
