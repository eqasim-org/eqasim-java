package org.eqasim.jakarta.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class JakartaCostParameters implements ParameterDefinition {
	
	public double carCost_BRL_km;	
	
	public double ptCostPerTrip_0Transfers_BRL;
	public double ptCostPerTrip_3Transfers_BRL;
	
	public double taxiPickUpFee_BRL;
	public double taxiCostPerMin_BRL;
	public double taxiCostPerkm_BRL;

	public double taxMinCost_BRL;

	public static JakartaCostParameters buildDefault() {
		JakartaCostParameters parameters = new JakartaCostParameters();

		parameters.carCost_BRL_km = 0.51;
		
		parameters.ptCostPerTrip_0Transfers_BRL = 4.3;
		parameters.ptCostPerTrip_3Transfers_BRL = 7.48;
		
		parameters.taxiPickUpFee_BRL = 3.25;//4.16;
		parameters.taxiCostPerMin_BRL = 0.38;//0.48;
		parameters.taxiCostPerkm_BRL = 2.0;//2.4;
		parameters.taxMinCost_BRL = 7.0;//2.4;

		return parameters;
	}
}
