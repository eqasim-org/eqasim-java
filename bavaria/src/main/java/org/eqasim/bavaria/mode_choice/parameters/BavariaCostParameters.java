package org.eqasim.bavaria.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class BavariaCostParameters implements ParameterDefinition {
	public double carCost_EUR_km = 0.0;
	public double parisParkingCost_EUR_h = 0.0;

	public static BavariaCostParameters buildDefault() {
		BavariaCostParameters parameters = new BavariaCostParameters();

		parameters.carCost_EUR_km = 0.2;
		parameters.parisParkingCost_EUR_h = 3.0;

		return parameters;
	}
}
