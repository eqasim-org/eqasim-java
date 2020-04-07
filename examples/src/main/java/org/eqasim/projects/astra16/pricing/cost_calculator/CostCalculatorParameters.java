package org.eqasim.projects.astra16.pricing.cost_calculator;

public class CostCalculatorParameters {
	public int numberOfVehicles;
	public double vehicleDistanceKm;

	public int numberOfTrips;
	public double passengerDistanceKm;

	public CostCalculatorParameters(int numberOfVehicles, double vehicleDistanceKm, int numberOfTrips,
			double passengerDistanceKm) {
		this.numberOfVehicles = numberOfVehicles;
		this.vehicleDistanceKm = vehicleDistanceKm;
		this.numberOfTrips = numberOfTrips;
		this.passengerDistanceKm = passengerDistanceKm;
	}
}
