package org.eqasim.san_francisco.mode_choice.utilities.predictors;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.List;

public class SanFranciscoBikePredictor extends CachedVariablePredictor<BikeVariables> {
	@Override
	public BikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() != 5) {
			System.out.println("Number of elements: " + elements.size());
			System.out.println(elements.toString());
		}

		Leg legAccess = (Leg) elements.get(0);
		Leg legBike = (Leg) elements.get(2);
		Leg legEgress = (Leg) elements.get(4);
		double travelTime_min = legBike.getTravelTime() / 60.0;
		double accessEgressTime_min = legAccess.getTravelTime() / 60.0 + legEgress.getTravelTime() / 60.0;

		return new BikeVariables(accessEgressTime_min + travelTime_min);
	}
}
