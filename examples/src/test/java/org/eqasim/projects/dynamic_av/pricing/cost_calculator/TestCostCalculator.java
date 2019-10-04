package org.eqasim.projects.dynamic_av.pricing.cost_calculator;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.ScenarioDefinition;
import org.junit.Assert;
import org.junit.Test;

public class TestCostCalculator {
	public double getResult(int fleetSize, double relativeActiveTime, double relativeEmptyRides, double speed,
			double averageTripLengthKm) {
		ScenarioDefinition scenario = ScenarioDefinition.buildDefaultSwitzerlandSolo();

		CostCalculator costCalculator = new CostCalculator(scenario);

		double vehicleDistanceKm = 24.0 * relativeActiveTime * speed / (1.0 - relativeEmptyRides);
		double passengerDistanceKm = 24.0 * relativeActiveTime * speed;
		int numberOfTrips = (int) Math.floor(passengerDistanceKm / averageTripLengthKm);

		CostCalculatorParameters parameters = new CostCalculatorParameters( //
				fleetSize, //
				vehicleDistanceKm * fleetSize, //
				numberOfTrips * fleetSize, //
				passengerDistanceKm * fleetSize //
		);

		return costCalculator.calculatePricePerPassengerKm(costCalculator.calculateFleetCost(parameters),
				parameters.passengerDistanceKm);
	}

	@Test
	public void testCostCalculator() {
		Assert.assertEquals(0.281494040681094, getResult(12000, 0.5, 0.3, 32.0, 15.0), 1e-3);
		Assert.assertEquals(0.281494040681094, getResult(15000, 0.5, 0.3, 32.0, 15.0), 1e-3);
		Assert.assertEquals(0.253783639628523, getResult(12000, 0.7, 0.3, 32.0, 15.0), 1e-3);
		Assert.assertEquals(0.344375975541895, getResult(12000, 0.5, 0.5, 32.0, 15.0), 1e-3);
		Assert.assertEquals(0.377388332371288, getResult(12000, 0.5, 0.3, 16.0, 15.0), 1e-3);
		Assert.assertEquals(0.295691496600541, getResult(12000, 0.5, 0.3, 32.0, 10.0), 1e-3);
	}
}
