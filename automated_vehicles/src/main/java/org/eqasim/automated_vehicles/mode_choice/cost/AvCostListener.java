package org.eqasim.automated_vehicles.mode_choice.cost;

import org.matsim.amodeus.analysis.FleetInformationListener;
import org.matsim.amodeus.analysis.FleetInformationListener.FleetInformation;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

public class AvCostListener implements AfterMobsimListener {
	private final AvCostParameters parameters;
	private final FleetInformationListener fleetListener;
	private final double numberOfVehicles;

	private double observedPrice_MU_km;
	private double activePrice_MU_km;

	public AvCostListener(AvCostParameters parameters, FleetInformationListener fleetListener, int numberOfVehicles) {
		this.parameters = parameters;
		this.fleetListener = fleetListener;
		this.numberOfVehicles = numberOfVehicles;

		this.observedPrice_MU_km = parameters.defaultPrice_MU_km;
		this.activePrice_MU_km = parameters.defaultPrice_MU_km;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// First, obtain fleet cost
		FleetInformation data = fleetListener.getInformation();

		double vehicleDistance_km = data.vehicleDistance_m * 1e-3;
		double passengerDistance_km = data.passengerDistance_m * 1e-3;

		double fleetCost_MU = 0.0;
		fleetCost_MU += vehicleDistance_km * parameters.distanceCost_MU_km;
		fleetCost_MU += numberOfVehicles * parameters.vehicleCost_MU;

		// Second, obtain price per passenger kilometer
		observedPrice_MU_km = fleetCost_MU / passengerDistance_km;
		observedPrice_MU_km *= parameters.priceFactor;

		// Third, interpolate
		if (Double.isFinite(observedPrice_MU_km)) {
			activePrice_MU_km = activePrice_MU_km * (1.0 - parameters.alpha) + parameters.alpha * observedPrice_MU_km;
		}
	}

	public double getObservedPrice_MU_km() {
		return observedPrice_MU_km;
	}

	public double getActivePrice_MU_km() {
		return activePrice_MU_km;
	}
}
