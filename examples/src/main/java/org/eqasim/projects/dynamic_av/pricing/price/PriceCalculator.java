package org.eqasim.projects.dynamic_av.pricing.price;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculatorParameters;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.analysis.FleetDistanceListener.OperatorData;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;

public class PriceCalculator implements AfterMobsimListener, PersonArrivalEventHandler {
	private final ProjectCostParameters costParameters;
	private final CostCalculator costCalculator;
	private final FleetDistanceListener fleetListener;
	private final int numberOfVehicles;
	private final double scalingFactor;

	private int numberOfTrips;

	private FinancialInformation information = new FinancialInformation();
	private double interpolatedPricePerKm_CHF;

	public PriceCalculator(ProjectCostParameters parameters, FleetDistanceListener fleetListener, int numberOfVehicles,
			CostCalculator costCalculator, double scalingFactor) {
		this.costParameters = parameters;
		this.fleetListener = fleetListener;
		this.numberOfVehicles = numberOfVehicles;
		this.costCalculator = costCalculator;
		this.scalingFactor = scalingFactor;

		this.interpolatedPricePerKm_CHF = parameters.defaultPrice_MU_km;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// First, obtain fleet cost
		OperatorData data = fleetListener.getData(OperatorConfig.DEFAULT_OPERATOR_ID);

		CostCalculatorParameters calculatorParameters = new CostCalculatorParameters( //
				(int) (numberOfVehicles / scalingFactor), //
				1e-3 * (data.emptyDistance_m + data.occupiedDistance_m) / scalingFactor, //
				(int) (numberOfTrips / scalingFactor), //
				1e-3 * data.passengerDistance_m / scalingFactor //
		);

		information.fleetCost_CHF = costCalculator.calculateFleetCost(calculatorParameters);
		information.costPerPassengerKm_CHF = information.fleetCost_CHF / calculatorParameters.passengerDistanceKm;
		
		information.vehicleDistance_km = calculatorParameters.vehicleDistanceKm;
		information.passengerDistance_km = calculatorParameters.passengerDistanceKm;

		// Second obtain fare structure
		double baseFareRevenue_CHF = costParameters.baseFare_CHF * calculatorParameters.numberOfTrips;
		information.baseFareRevenue_CHF = baseFareRevenue_CHF;
		
		if (Double.isNaN(costParameters.distanceFare_CHF_km)) {
			// Cost-covering case, we calculate a price that covers the costs

			double remainingFleetCost_MU = Math.max(0.0, information.fleetCost_CHF - baseFareRevenue_CHF);
			information.profit_CHF = Math.max(0.0, baseFareRevenue_CHF - information.fleetCost_CHF);
			
			double costPerPassengerKm_MU = remainingFleetCost_MU / calculatorParameters.passengerDistanceKm;
			
			double minimumCostPerPassengerKm_MU = 0.0;
			
			if (costPerPassengerKm_MU < costParameters.minimumDistanceFare_CHF_km) {
				// We make profit from distance
				minimumCostPerPassengerKm_MU = costParameters.minimumDistanceFare_CHF_km;
				information.profit_CHF += (costParameters.minimumDistanceFare_CHF_km - costPerPassengerKm_MU) * calculatorParameters.passengerDistanceKm;
			} else {
				// No profit from distance
				minimumCostPerPassengerKm_MU = costPerPassengerKm_MU;
			}
			
			information.pricePerPassengerKm_CHF = costCalculator.calculatePrice(minimumCostPerPassengerKm_MU);
			information.pricePerTrip_CHF = costCalculator.calculatePrice(costParameters.baseFare_CHF);
			
		} else {
			// Fixed price, we calculate with revenue and profit

			information.pricePerPassengerKm_CHF = costCalculator.calculatePrice(costParameters.distanceFare_CHF_km);
			information.pricePerTrip_CHF = costCalculator.calculatePrice(costParameters.baseFare_CHF);

			double distanceFareRevenue_CHF = costParameters.distanceFare_CHF_km
					* calculatorParameters.passengerDistanceKm;
			double totalRevenue_CHF = distanceFareRevenue_CHF + baseFareRevenue_CHF;

			information.profit_CHF = totalRevenue_CHF - information.fleetCost_CHF;
		}

		// Third, interpolate
		if (Double.isFinite(information.pricePerPassengerKm_CHF)) {
			double alpha = event.getIteration() >= costParameters.transientIterations ? costParameters.alpha : 0.0;
			interpolatedPricePerKm_CHF = interpolatedPricePerKm_CHF * (1.0 - alpha)
					+ alpha * information.pricePerPassengerKm_CHF;
		}

		numberOfTrips = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(AVModule.AV_MODE)) {
			numberOfTrips++;
		}
	}

	public double calculateTripPrice_MU(double distance_km) {
		return distance_km * interpolatedPricePerKm_CHF + information.pricePerTrip_CHF;
	}

	public FinancialInformation getInformation() {
		return information;
	}

	public double getInterpolatedPricePerKm_CHF() {
		return interpolatedPricePerKm_CHF;
	}
}
