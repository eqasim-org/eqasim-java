package org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions;

public class VehicleTypeDefinition {
	public String name;
	
	public int capacity;

	public double acquisitionCostPerLifetime;

	public double insuranceCostPerYear;
	public double taxCostPerYear;
	public double parkingCostPerYear;
	public double otherCostPerYear;

	public double maintenanceCostPerKm;
	public double tireCostPerKm;
	public double fuelCostPerKm;
	public double otherCostPerKm;

	public VehicleTypeDefinition copy() {
		VehicleTypeDefinition copy = new VehicleTypeDefinition();

		copy.capacity = capacity;

		copy.acquisitionCostPerLifetime = acquisitionCostPerLifetime;

		copy.insuranceCostPerYear = insuranceCostPerYear;
		copy.taxCostPerYear = taxCostPerYear;
		copy.parkingCostPerYear = parkingCostPerYear;
		copy.otherCostPerYear = otherCostPerYear;

		copy.maintenanceCostPerKm = maintenanceCostPerKm;
		copy.tireCostPerKm = tireCostPerKm;
		copy.fuelCostPerKm = fuelCostPerKm;
		copy.otherCostPerKm = otherCostPerKm;

		return copy;
	}
}
