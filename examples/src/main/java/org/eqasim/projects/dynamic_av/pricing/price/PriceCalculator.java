package org.eqasim.projects.dynamic_av.pricing.price;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculatorParameters;
import org.eqasim.projects.dynamic_av.pricing.price.ProjectCostParameters.PriceStructure;
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

	private List<Double> history = new LinkedList<>();
	private double interpolatedPrice_MU_km;

	public PriceCalculator(ProjectCostParameters parameters, FleetDistanceListener fleetListener, int numberOfVehicles,
			CostCalculator costCalculator, double scalingFactor) {
		this.costParameters = parameters;
		this.fleetListener = fleetListener;
		this.numberOfVehicles = numberOfVehicles;
		this.costCalculator = costCalculator;
		this.scalingFactor = scalingFactor;

		for (int i = 0; i < parameters.horizon; i++) {
			history.add(parameters.defaultPrice_MU_km);
		}

		this.interpolatedPrice_MU_km = parameters.defaultPrice_MU_km;
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

		// Second, set up cost structure
		information.baseFareRevenue_CHF = costParameters.baseFare_CHF * calculatorParameters.numberOfTrips;
		information.pricePerTrip_CHF = costParameters.baseFare_CHF;

		if (costParameters.priceStructure.equals(PriceStructure.COST_COVERING)) {
			double remainingFleetCost = information.fleetCost_CHF - information.baseFareRevenue_CHF;

			if (remainingFleetCost > 0.0) {
				double requiredPricePerPassengerKm_CHF = remainingFleetCost / information.passengerDistance_km;
				double cappedPricePerPassengerKm_CHF = Math.min(
						Math.max(requiredPricePerPassengerKm_CHF, costParameters.minimumDistanceFare_CHF_km),
						costParameters.maximumDistanceFare_CHF_km);

				information.pricePerPassengerKm_CHF = cappedPricePerPassengerKm_CHF;
				information.profit_CHF = (cappedPricePerPassengerKm_CHF - requiredPricePerPassengerKm_CHF)
						* information.passengerDistance_km;
			} else {
				information.pricePerPassengerKm_CHF = 0.0;
				information.profit_CHF = -remainingFleetCost;
			}
		} else if (costParameters.priceStructure.equals(PriceStructure.COST_COVERING_PLUS_BASE_FARE)) {
			double requiredPricePerPassengerKm_CHF = information.fleetCost_CHF / information.passengerDistance_km;

			double cappedPricePerPassengerKm_CHF = Math.min(
					Math.max(requiredPricePerPassengerKm_CHF, costParameters.minimumDistanceFare_CHF_km),
					costParameters.maximumDistanceFare_CHF_km);

			information.pricePerPassengerKm_CHF = cappedPricePerPassengerKm_CHF;
			information.profit_CHF = (cappedPricePerPassengerKm_CHF - requiredPricePerPassengerKm_CHF)
					* information.passengerDistance_km + information.baseFareRevenue_CHF;
		} else if (costParameters.priceStructure.equals(PriceStructure.FIXED_FARE)) {
			double requiredPricePerPassengerKm_CHF = information.fleetCost_CHF / information.passengerDistance_km;
			information.pricePerPassengerKm_CHF = costParameters.defaultPrice_MU_km;
			information.profit_CHF = (costParameters.defaultPrice_MU_km - requiredPricePerPassengerKm_CHF)
					* information.passengerDistance_km;
		} else {
			throw new IllegalStateException();
		}

		information.pricePerPassengerKm_CHF = costCalculator.calculatePrice(information.pricePerPassengerKm_CHF);
		information.pricePerTrip_CHF = costCalculator.calculatePrice(information.pricePerTrip_CHF);

		// Third, interpolate
		if (Double.isFinite(information.pricePerPassengerKm_CHF)) {
			history.remove(0);
			history.add(information.pricePerPassengerKm_CHF);

			interpolatedPrice_MU_km = 0.0;

			for (int i = 0; i < costParameters.horizon; i++) {
				interpolatedPrice_MU_km += history.get(i);
			}

			interpolatedPrice_MU_km /= costParameters.horizon;
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
		return distance_km * interpolatedPrice_MU_km + information.pricePerTrip_CHF;
	}

	public FinancialInformation getInformation() {
		return information;
	}

	public double getInterpolatedPricePerKm_CHF() {
		return interpolatedPrice_MU_km;
	}
}
