package org.eqasim.auckland.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AucklandCarCostModel extends AbstractCostModel {
	static public final String NAME = "AucklandCarCostModel";

	private final AucklandCostParameters parameters;

	@Inject
	public AucklandCarCostModel(AucklandCostParameters parameters) {
		super(TransportMode.car);
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		return (getInVehicleDistance_km(elements) / 100.0) * parameters.carConsumption_l_100km
				* parameters.carFuelCost_MU_l;
	}
}
