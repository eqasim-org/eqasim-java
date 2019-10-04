package org.eqasim.projects.dynamic_av.pricing;

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

public class FleetCostListener implements PersonArrivalEventHandler, AfterMobsimListener {
	private final FleetDistanceListener fleetDistanceListener;
	private final int numberOfVehicles;
	private final CostCalculator calculator;

	private int numberOfTrips = 0;

	private double fleetCost_MU;

	public FleetCostListener(CostCalculator calculator, FleetDistanceListener fleetDistanceListener,
			int numberOfVehicles) {
		this.fleetDistanceListener = fleetDistanceListener;
		this.numberOfVehicles = numberOfVehicles;
		this.calculator = calculator;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		OperatorData data = fleetDistanceListener.getData(OperatorConfig.DEFAULT_OPERATOR_ID);

		CostCalculatorParameters parameters = new CostCalculatorParameters( //
				numberOfVehicles, //
				data.emptyDistance_m + data.occupiedDistance_m, //
				numberOfTrips, //
				data.passengerDistance_m //
		);

		fleetCost_MU = calculator.calculateFleetCost(parameters);
	}

	public double getFleetCost_MU() {
		return fleetCost_MU;
	}

	public double getPricePerPassengerKm() {
		return fleetCost_MU;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(AVModule.AV_MODE)) {
			numberOfTrips++;
		}
	}

	@Override
	public void reset(int iteration) {
		numberOfTrips = 0;
	}
}
