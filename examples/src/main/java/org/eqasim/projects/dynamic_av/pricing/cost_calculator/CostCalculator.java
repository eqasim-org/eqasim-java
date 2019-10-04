package org.eqasim.projects.dynamic_av.pricing.cost_calculator;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.ScenarioDefinition;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.TechnologyDefinition;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.VATDefinition;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.VehicleTypeDefinition;

public class CostCalculator {
	private final ScenarioDefinition scenario;

	public CostCalculator(ScenarioDefinition scenario) {
		this.scenario = scenario;
	}

	private double calculateInterestSum(double acquisitionCost, double interestRatePerYear, double years,
			double paymentFrequency) {
		double q = 1.0 + interestRatePerYear / paymentFrequency;
		double ann = acquisitionCost * Math.pow(q, years * paymentFrequency) * (q - 1)
				/ (Math.pow(q, years * paymentFrequency) - 1.0);
		return ann * years * paymentFrequency - acquisitionCost;
	}

	private void applyTechnologies(VehicleTypeDefinition vehicleType) {
		for (TechnologyDefinition technology : scenario.technologies) {
			vehicleType.acquisitionCostPerLifetime *= (1.0 + technology.acquisitionCostFactor);

			vehicleType.insuranceCostPerYear *= (1.0 + technology.insuranceCostFactor);
			vehicleType.taxCostPerYear *= (1.0 + technology.taxCostFactor);
			vehicleType.parkingCostPerYear *= (1.0 + technology.parkingCostFactor);
			vehicleType.otherCostPerYear *= (1.0 + technology.otherCostPerYearFactor);

			vehicleType.maintenanceCostPerKm *= (1.0 + technology.maintenanceCostFactor);
			vehicleType.tireCostPerKm *= (1.0 + technology.tireCostFactor);
			vehicleType.fuelCostPerKm *= (1.0 + technology.fuelCostFactor);
			vehicleType.otherCostPerKm *= (1.0 + technology.otherCostPerKmFactor);
		}
	}

	private void applyVATDeducation(VehicleTypeDefinition vehicleType) {
		VATDefinition vat = scenario.vat;

		vehicleType.acquisitionCostPerLifetime /= 1.0 + (vat.acquisitionCostIsDeductible ? vat.vat : 1.0);

		vehicleType.insuranceCostPerYear /= 1.0 + (vat.acquisitionCostIsDeductible ? vat.vat : 1.0);
		vehicleType.taxCostPerYear /= 1.0 + (vat.taxCostIsDeductible ? vat.vat : 1.0);
		vehicleType.parkingCostPerYear /= 1.0 + (vat.parkingCostIsDeductible ? vat.vat : 1.0);
		vehicleType.otherCostPerYear /= 1.0 + (vat.otherCostPerYearIsDeductible ? vat.vat : 1.0);

		vehicleType.maintenanceCostPerKm /= 1.0 + (vat.maintenanceCostIsDeductible ? vat.vat : 1.0);
		vehicleType.tireCostPerKm /= 1.0 + (vat.tireCostIsDeductible ? vat.vat : 1.0);
		vehicleType.fuelCostPerKm /= 1.0 + (vat.fuelCostIsDeductible ? vat.vat : 1.0);
		vehicleType.otherCostPerKm /= 1.0 + (vat.otherCostPerKmIsDeductible ? vat.vat : 1.0);
	}

	private double calculateFixedCostPerVehiclePerDay(VehicleTypeDefinition vehicleType, int numberOfTrips) {
		double cleaningCostPerYear = scenario.cleaningPrice * scenario.cleaningFrequencyPerTrip * numberOfTrips
				* 365.25;

		return (0.0 //
				+ vehicleType.insuranceCostPerYear //
				+ vehicleType.taxCostPerYear //
				+ vehicleType.parkingCostPerYear //
				+ cleaningCostPerYear //
				+ vehicleType.otherCostPerYear //
		) / 365.25;
	}

	private double calculateVariableCostPerVehiclePerDay(VehicleTypeDefinition vehicleType, double vehicleDistanceKm,
			int numberOfVehicles) {
		double acquisition = vehicleType.acquisitionCostPerLifetime / scenario.vehicleLifetimeKm;
		double interest = calculateInterestSum(vehicleType.acquisitionCostPerLifetime, scenario.interestRatePerYear,
				scenario.creditPeriodYears, 1.0) / scenario.vehicleLifetimeKm;

		return (0.0 //
				+ acquisition //
				+ interest //
				+ vehicleType.maintenanceCostPerKm //
				+ vehicleType.tireCostPerKm //
				+ vehicleType.fuelCostPerKm //
				+ vehicleType.otherCostPerKm //
		) * vehicleDistanceKm / numberOfVehicles;
	}

	private double calculateOverheadCostPerDay() {
		return scenario.overheadCostPerVehiclePerDay + scenario.operationsManagementCostPerVehiclePerDay;
	}

	private double calculateTotalCostPerVehiclePerDay(VehicleTypeDefinition vehicleType, int numberOfVehicles,
			double vehicleDistanceKm, int numberOfTrips) {
		return calculateOverheadCostPerDay() //
				+ calculateVariableCostPerVehiclePerDay(vehicleType, vehicleDistanceKm, numberOfVehicles) //
				+ calculateFixedCostPerVehiclePerDay(vehicleType, numberOfTrips);
	}

	public double calculateFleetCost(CostCalculatorParameters parameters) {
		VehicleTypeDefinition vehicleTypeDefinition = scenario.vehicleType.copy();
		applyTechnologies(vehicleTypeDefinition);
		applyVATDeducation(vehicleTypeDefinition);

		return calculateTotalCostPerVehiclePerDay(vehicleTypeDefinition, parameters.numberOfVehicles,
				parameters.vehicleDistanceKm, parameters.numberOfTrips);
	}
}
