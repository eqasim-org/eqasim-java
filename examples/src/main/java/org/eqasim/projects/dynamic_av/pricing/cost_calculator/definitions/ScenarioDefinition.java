package org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions;

import java.util.Collection;
import java.util.HashSet;

public class ScenarioDefinition {
	public double vehicleLifetimeKm;
	public double midsizeCarReferencePrice;

	public double cleaningPrice;
	public double cleaningFrequencyPerTrip;

	public double overheadCostPerVehiclePerDay;
	public double operationsManagementCostPerVehiclePerDay;

	public double interestRatePerYear;
	public double creditPeriodYears;

	public double profitMargin;
	public double paymentTransationFee;

	public VATDefinition vat = new VATDefinition();
	public VehicleTypeDefinition vehicleType = new VehicleTypeDefinition();
	public Collection<TechnologyDefinition> technologies = new HashSet<>();

	public static ScenarioDefinition buildSwitzerlandSolo() {
		ScenarioDefinition scenario = new ScenarioDefinition();

		scenario.vehicleLifetimeKm = 300000.0;
		scenario.midsizeCarReferencePrice = 0.0;

		scenario.cleaningPrice = 15.0;
		scenario.cleaningFrequencyPerTrip = 0.025;

		scenario.overheadCostPerVehiclePerDay = 14.0;
		scenario.operationsManagementCostPerVehiclePerDay = 10.0;

		scenario.interestRatePerYear = 0.015;
		scenario.creditPeriodYears = 3.0;

		scenario.profitMargin = 0.03;
		scenario.paymentTransationFee = 0.0044;

		TechnologyDefinition technologyElectric = new TechnologyDefinition();
		technologyElectric.name = "electric";
		technologyElectric.insuranceCostFactor = -0.35;
		technologyElectric.taxCostFactor = -1.0;
		technologyElectric.maintenanceCostFactor = 0.975;
		technologyElectric.fuelCostFactor = -0.5;
		scenario.technologies.add(technologyElectric);

		TechnologyDefinition technologyAutomated = new TechnologyDefinition();
		technologyElectric.name = "automated";
		technologyAutomated.acquisitionCostFactor = 0.2;
		technologyAutomated.insuranceCostFactor = -0.5;
		technologyAutomated.tireCostFactor = -0.1;
		technologyAutomated.fuelCostFactor = -0.1;
		scenario.technologies.add(technologyAutomated);

		TechnologyDefinition technologyFleet = new TechnologyDefinition();
		technologyElectric.name = "fleet";
		technologyFleet.acquisitionCostFactor = -0.3;
		technologyFleet.insuranceCostFactor = -0.2;
		technologyFleet.parkingCostFactor = 1.333;
		technologyFleet.maintenanceCostFactor = -0.25;
		technologyFleet.tireCostFactor = -0.25;
		technologyFleet.fuelCostFactor = -0.05;
		scenario.technologies.add(technologyFleet);

		scenario.vat.acquisitionCostIsDeductible = true;
		scenario.vat.insuranceCostIsDeductible = true;
		scenario.vat.parkingCostIsDeductible = true;
		scenario.vat.otherCostPerYearIsDeductible = true;
		scenario.vat.maintenanceCostIsDeductible = true;
		scenario.vat.tireCostIsDeductible = true;
		scenario.vat.fuelCostIsDeductible = true;

		scenario.vat.vat = 0.08;

		scenario.vehicleType.name = "Solo";

		scenario.vehicleType.acquisitionCostPerLifetime = 13000.0;
		scenario.vehicleType.insuranceCostPerYear = 500.0;
		scenario.vehicleType.taxCostPerYear = 120.0;
		scenario.vehicleType.parkingCostPerYear = 1500.0;
		scenario.vehicleType.otherCostPerYear = 40.0;

		scenario.vehicleType.maintenanceCostPerKm = 0.02;
		scenario.vehicleType.tireCostPerKm = 0.02;
		scenario.vehicleType.fuelCostPerKm = 0.06;

		return scenario;
	}
}
