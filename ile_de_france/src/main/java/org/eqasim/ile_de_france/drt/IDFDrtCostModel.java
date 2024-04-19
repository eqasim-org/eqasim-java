package org.eqasim.ile_de_france.drt;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Singleton;

@Singleton
public class IDFDrtCostModel extends AbstractCostModel {
	public IDFDrtCostModel() {
		super("drt");
	}

	private final double baseFare_EUR = 1.9;
	private final double distanceFare_EUR_km = 0.5;

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double distance_km = getInVehicleDistance_km(elements);
		return baseFare_EUR + distanceFare_EUR_km * distance_km;
	}
}
