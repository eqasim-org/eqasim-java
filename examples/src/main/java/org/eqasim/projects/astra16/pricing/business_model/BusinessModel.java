package org.eqasim.projects.astra16.pricing.business_model;

import org.eqasim.projects.astra16.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.astra16.pricing.cost_calculator.CostCalculatorParameters;

public class BusinessModel {
	private final CostCalculator costCalculator;

	private final int fleetSize;
	private double scalingFactor;

	private final double pricePerTrip_CHF;
	private double maximumPricePerKm_CHF;
	private double minimumPricePerKm_CHF;

	private final double infrastructureCostPerKm_CHF;

	public BusinessModel(CostCalculator costCalculator, int fleetSize, double scalingFactor, double pricePerTrip_CHF,
			double maximumPricePerKm_CHF, double minimumPricePerKm_CHF, double infrastructureCostPerKm_CHF) {
		this.costCalculator = costCalculator;

		this.fleetSize = (int) (fleetSize / scalingFactor);
		this.scalingFactor = scalingFactor;

		this.pricePerTrip_CHF = pricePerTrip_CHF;
		this.maximumPricePerKm_CHF = maximumPricePerKm_CHF;
		this.minimumPricePerKm_CHF = minimumPricePerKm_CHF;

		this.infrastructureCostPerKm_CHF = infrastructureCostPerKm_CHF;
	}

	public BusinessModelData update(double vehicleDistance_km, double passengerDistance_km, long passengerTrips) {
		if (fleetSize == 0) {
			return new BusinessModelData();
		}

		vehicleDistance_km /= scalingFactor;
		passengerDistance_km /= scalingFactor;
		passengerTrips /= scalingFactor;

		CostCalculatorParameters calculatorParameters = new CostCalculatorParameters( //
				fleetSize, //
				vehicleDistance_km, //
				(int) passengerTrips, //
				passengerDistance_km //
		);

		double fleetCost_CHF = costCalculator.calculateFleetCost(calculatorParameters);
		double infrastructureCost_CHF = vehicleDistance_km * infrastructureCostPerKm_CHF;
		double totalCost_CHF = fleetCost_CHF + infrastructureCost_CHF;

		double costPerPassengerKm_CHF = totalCost_CHF / passengerDistance_km;

		double tripFareRevenue_CHF = passengerTrips * pricePerTrip_CHF;
		double remainingFleetCost_CHF = totalCost_CHF - tripFareRevenue_CHF;

		// If base fare covers costs, we do not need additional distance fare, instead
		// we make profit
		double remainingCostPerPassengerKm_CHF = 0.0;

		if (remainingFleetCost_CHF > 0.0) {
			remainingCostPerPassengerKm_CHF = remainingFleetCost_CHF / passengerDistance_km;
		}

		// At this point:

		// - costPerPassengerKm_CHF quantifies the *full* cost per passenger kilometer
		// (no base fare considered)

		// - remainingCostPerPassengerKm_CHF quantifies the *remaining* cost per
		// passenger kilometer (after we consider revenues from the base fare already)

		BusinessModelData model = new BusinessModelData();

		model.vehicleDistance_km = vehicleDistance_km;
		model.passengerDistance_km = passengerDistance_km;
		model.fullCostPerPassengerKm_CHF = costPerPassengerKm_CHF;
		model.costPerPassengerKmAfterBaseFare_CHF = remainingCostPerPassengerKm_CHF;

		model.tripFareRevenue_CHF = tripFareRevenue_CHF;
		model.fleetCost_CHF = fleetCost_CHF;
		model.totalCost_CHF = totalCost_CHF;
		model.infrastructureCost_CHF = infrastructureCost_CHF;

		// No we can start calculating prices. First, we calculate the nominal price
		// (without taxes) that we need to ask to cover the remaining costs.
		double nominalPricePerKm_CHF = remainingCostPerPassengerKm_CHF;
		double boundedPricePerKm_CHF = nominalPricePerKm_CHF;

		// Next, we consider some minimum price if requested
		if (Double.isFinite(minimumPricePerKm_CHF)) {
			boundedPricePerKm_CHF = Math.max(minimumPricePerKm_CHF, boundedPricePerKm_CHF);
			// This means we may make additional profit
		}

		// Next, we consider a maximum price if given
		if (Double.isFinite(maximumPricePerKm_CHF)) {
			boundedPricePerKm_CHF = Math.min(maximumPricePerKm_CHF, boundedPricePerKm_CHF);
			// This means we may make losses!
		}

		// Calculate revenue and profit
		double distanceFareRevenue_CHF = boundedPricePerKm_CHF * passengerDistance_km;
		double profit_CHF = distanceFareRevenue_CHF + tripFareRevenue_CHF - fleetCost_CHF;

		model.distanceFareRevenue_CHF = distanceFareRevenue_CHF;
		model.profit_CHF = profit_CHF;

		// What remains is to calculate the final price with taxes
		double taxedPricePerKm_CHF = costCalculator.calculatePrice(boundedPricePerKm_CHF);
		double taxedPricePerTrip_CHF = costCalculator.calculatePrice(pricePerTrip_CHF);

		model.pricePerTrip_CHF = taxedPricePerTrip_CHF;
		model.pricePerPassengerKm_CHF = taxedPricePerKm_CHF;

		return model;
	}
}
