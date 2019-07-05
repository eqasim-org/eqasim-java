package org.eqasim.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.simulation.mode_choice.utilities.variables.BikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public class BikePredictor {
	public BikeVariables predict(List<? extends PlanElement> elements) {
		double travelTime_min = ((Leg) elements.get(0)).getTravelTime() / 60.0;

		return new BikeVariables(travelTime_min);
	}
}
