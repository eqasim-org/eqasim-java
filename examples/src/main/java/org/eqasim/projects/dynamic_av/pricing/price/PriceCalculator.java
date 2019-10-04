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

	private double observedPrice_MU_km;
	private double activePrice_MU_km;

	public PriceCalculator(ProjectCostParameters parameters, FleetDistanceListener fleetListener, int numberOfVehicles,
			CostCalculator costCalculator, double scalingFactor) {
		this.costParameters = parameters;
		this.fleetListener = fleetListener;
		this.numberOfVehicles = numberOfVehicles;
		this.costCalculator = costCalculator;
		this.scalingFactor = scalingFactor;

		this.observedPrice_MU_km = parameters.defaultPrice_MU_km;
		this.activePrice_MU_km = parameters.defaultPrice_MU_km;
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

		double fleetCost_MU = costCalculator.calculateFleetCost(calculatorParameters);

		// Second, obtain price per passenger kilometer
		double pricePerPassengerKm_MU = costCalculator.calculatePricePerPassengerKm(fleetCost_MU,
				calculatorParameters.passengerDistanceKm);
		observedPrice_MU_km = pricePerPassengerKm_MU;

		// Third, interpolate
		if (Double.isFinite(observedPrice_MU_km)) {
			double alpha = event.getIteration() >= costParameters.transientIterations ? costParameters.alpha : 0.0;
			activePrice_MU_km = activePrice_MU_km * (1.0 - alpha) + alpha * observedPrice_MU_km;
		}

		numberOfTrips = 0;
	}

	public double getObservedPrice_MU_km() {
		return observedPrice_MU_km;
	}

	public double getActivePrice_MU_km() {
		return activePrice_MU_km;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(AVModule.AV_MODE)) {
			numberOfTrips++;
		}
	}
}
