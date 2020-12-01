package org.eqasim.projects.astra16.pricing.cost_calculator.definitions;

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
	
	static public VehicleTypeDefinition createDefaultSolo() {
		VehicleTypeDefinition definition = new VehicleTypeDefinition();
		
		definition.name = "Solo";
		definition.capacity = 1;
		
		definition.acquisitionCostPerLifetime = 13000;
		definition.insuranceCostPerYear = 500;
		definition.taxCostPerYear = 120;
		definition.parkingCostPerYear = 1500;
		definition.otherCostPerYear = 40;
		
		definition.maintenanceCostPerKm = 0.02;
		definition.tireCostPerKm = 0.02;
		definition.fuelCostPerKm = 0.06;
		
		return definition;
	}
	
	static public VehicleTypeDefinition createDefaultMidsize() {
		VehicleTypeDefinition definition = new VehicleTypeDefinition();
		
		definition.name = "Midsize";
		definition.capacity = 4;
		
		definition.acquisitionCostPerLifetime = 35000;
		definition.insuranceCostPerYear = 1000;
		definition.taxCostPerYear = 250;
		definition.parkingCostPerYear = 1500;
		definition.otherCostPerYear = 40;
		
		definition.maintenanceCostPerKm = 0.06;
		definition.tireCostPerKm = 0.02;
		definition.fuelCostPerKm = 0.08;
		
		return definition;
	}
	
	static public VehicleTypeDefinition createDefaultVan() {
		VehicleTypeDefinition definition = new VehicleTypeDefinition();
		
		definition.name = "Van";
		definition.capacity = 8;
		
		definition.acquisitionCostPerLifetime = 66000;
		definition.insuranceCostPerYear = 1200;
		definition.taxCostPerYear = 700;
		definition.parkingCostPerYear = 1500;
		definition.otherCostPerYear = 40;
		
		definition.maintenanceCostPerKm = 0.12;
		definition.tireCostPerKm = 0.02;
		definition.fuelCostPerKm = 0.13;
		
		return definition;
	}
	
	static public VehicleTypeDefinition createDefaultMinibus() {
		VehicleTypeDefinition definition = new VehicleTypeDefinition();
		
		definition.name = "Minibus";
		definition.capacity = 20;
		
		definition.acquisitionCostPerLifetime = 70000;
		definition.insuranceCostPerYear = 1400;
		definition.taxCostPerYear = 1100;
		definition.parkingCostPerYear = 1500;
		definition.otherCostPerYear = 2200;
		
		definition.maintenanceCostPerKm = 0.13;
		definition.tireCostPerKm = 0.02;
		definition.fuelCostPerKm = 0.2;
		
		return definition;
	}
}
