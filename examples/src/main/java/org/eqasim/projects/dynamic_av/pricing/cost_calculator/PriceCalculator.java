package org.eqasim.projects.dynamic_av.pricing.cost_calculator;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.ScenarioDefinition;

public class PriceCalculator {
	private final ScenarioDefinition scenario;

	public PriceCalculator(ScenarioDefinition scenario) {
		this.scenario = scenario;
	}

	public double calculatePricePerPassengerKm(double fleetCost, double passengerDistanceKm) {
		double costPerPassengerKm = fleetCost / passengerDistanceKm;
		return calculatePrice(costPerPassengerKm);
	}

	public double calculatePrice(double cost) {
		cost /= (1.0 - scenario.profitMargin);
		cost /= (1.0 - scenario.paymentTransationFee);
		cost *= (1.0 + scenario.vat.vat);
		return cost;
	}
}
