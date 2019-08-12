package org.eqasim.switzerland.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class SwissPredictorUtils {
	static public boolean hasGeneralSubscription(Person person) {
		Boolean hasGeneralSubscription = (Boolean) person.getAttributes().getAttribute("ptHasGA");
		return hasGeneralSubscription != null && hasGeneralSubscription;
	}

	static public boolean hasHalbtaxSubscription(Person person) {
		Boolean hasHalbtaxSubscription = (Boolean) person.getAttributes().getAttribute("ptHasHalbtax");
		return hasHalbtaxSubscription != null && hasHalbtaxSubscription;
	}

	static public boolean hasRegionalSubscription(Person person) {
		Boolean hasRegionalSubscription = (Boolean) person.getAttributes().getAttribute("ptHasVerbund");
		return hasRegionalSubscription != null && hasRegionalSubscription;
	}

	static public int getStatedPreferenceRegion(Person person) {
		Integer spRegion = (Integer) person.getAttributes().getAttribute("spRegion");
		return spRegion == null ? -1 : spRegion;
	}

	static public Coord getHomeLocation(Person person) {
		Double homeX = (Double) person.getAttributes().getAttribute("home_x");
		Double homeY = (Double) person.getAttributes().getAttribute("home_y");

		if (homeX == null || homeY == null) {
			homeX = 0.0;
			homeY = 0.0;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getType().equals("home")) {
						homeX = activity.getCoord().getX();
						homeY = activity.getCoord().getY();
					}
				}
			}

			person.getAttributes().putAttribute("home_x", homeX);
			person.getAttributes().putAttribute("home_y", homeY);
		}

		return new Coord(homeX, homeY);
	}
}
