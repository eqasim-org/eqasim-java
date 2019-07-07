package org.eqasim.ile_de_france.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.ile_de_france.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public class WalkPredictor {
	public WalkVariables predict(List<? extends PlanElement> elements) {
		double travelTime_min = ((Leg) elements.get(0)).getTravelTime() / 60.0;

		return new WalkVariables(travelTime_min);
	}
}
