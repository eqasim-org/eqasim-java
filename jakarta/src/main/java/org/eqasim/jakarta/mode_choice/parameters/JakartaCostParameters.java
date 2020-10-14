package org.eqasim.jakarta.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class JakartaCostParameters implements ParameterDefinition {
	
	public double carCost_KIDR_km;
	public double carCharging_KIDR_km;
	public double mcCharging_KIDR_km;
	
	public double motorcycleCost_KIDR_km;
	
	public double ptCostPerTrip_0Transfers_KIDR;
	public double ptCostPerTrip_3Transfers_KIDR;
	
	public double carodtPickUpFee_KIDR;
	public double carodtCostPerMin_KIDR;
	public double carodtCostPerkm_KIDR;
	
	
	public double mcodtPickUpFee_KIDR;
	public double mcodtCostPerMin_KIDR;
	public double mcodtCostPerkm_KIDR;
	
	//public double taxiPickUpFee_BRL;
	//public double taxiCostPerMin_BRL;
	//public double taxiCostPerkm_BRL;

	//public double taxMinCost_KIDR;
	public double carodtMinCost_KIDR;
	public double mcodtMinCost_KIDR;

	public static JakartaCostParameters buildDefault() {
		JakartaCostParameters parameters = new JakartaCostParameters();

		parameters.carCost_KIDR_km = 2.95;
		parameters.carCharging_KIDR_km = 5.0;
		parameters.mcCharging_KIDR_km = 2.5;
		
		
		parameters.motorcycleCost_KIDR_km = 0.59;
		
	
		
		parameters.ptCostPerTrip_0Transfers_KIDR = 4.0;
		parameters.ptCostPerTrip_3Transfers_KIDR = 10.0;
		
		//parameters.taxiPickUpFee_BRL = 3.25;//4.16;
		//parameters.taxiCostPerMin_BRL = 0.38;//0.48;
		//parameters.taxiCostPerkm_BRL = 2.0;//2.4;
		//parameters.taxMinCost_KIDR = 7.0;//2.4;

		parameters.carodtPickUpFee_KIDR = 6;//4.16;
		//parameters.carodtCostPerMin_KIDR = 0.38;//0.48;
		parameters.carodtCostPerkm_KIDR = 4.5;//2.4;
		parameters.carodtMinCost_KIDR = 10;//2.4;
		
		parameters.mcodtPickUpFee_KIDR = 4.0;//4.16;
		//parameters.mcodtCostPerMin_KIDR = 0.38;//0.48;
		parameters.mcodtCostPerkm_KIDR = 2.5;//2.4;
		parameters.mcodtMinCost_KIDR = 6.0;//2.4;
		
		
		
		return parameters;
	}
}
