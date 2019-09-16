package org.eqasim.automated_vehicles.mode_choice.cost;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.analysis.FleetDistanceListener.OperatorData;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AvCostModel extends AbstractCostModel {
	private final AvCostParameters parameters;
	private final FleetDistanceListener fleetListener;
	private final int numberOfVehicles;

	public AvCostModel(AvCostParameters parameters, FleetDistanceListener fleetListener, int numberOfVehicles) {
		super("av");

		this.parameters = parameters;
		this.fleetListener = fleetListener;
		this.numberOfVehicles = numberOfVehicles;
	}

	/*
	 * TODO: Actually everything in this function is done over and over again. Would
	 * make sense to calculate this at a central spot at the end of the Mobsim.
	 */
	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// First, obtain fleet cost
		OperatorData data = fleetListener.getData(OperatorConfig.DEFAULT_OPERATOR_ID);

		double vehicleDistance_km = data.emptyDistance_m + data.occupiedDistance_m;
		double passengerDistance_km = data.passengerDistance_m;

		double fleetCost_MU = 0.0;
		fleetCost_MU += vehicleDistance_km * parameters.distanceCost_MU_km;
		fleetCost_MU += numberOfVehicles * parameters.vehicleCost_MU;

		// Second, obtain price per passenger kilometer
		double distancePrice_MU_km = fleetCost_MU / passengerDistance_km;
		distancePrice_MU_km *= parameters.priceFactor;

		// Third, calculate actual trip price
		double tripDistance_km = getInVehicleDistance_km(elements);
		return distancePrice_MU_km * tripDistance_km;
	}
}
