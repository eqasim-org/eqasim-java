package org.eqasim.switzerland.ch.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * The PT subscriptions of a person: the subscriptions attribute of the current populations (none, GA, HT, VA or VA+HT,
 * decoded as in ch_cmdp), else the boolean attributes of the older ones (ptHasGA, ptHasHalbtax, ptHasVerbund).
 */
public class SwissPredictorUtils {
	static public final String SUBSCRIPTIONS = "subscriptions";

	private static String subscriptions(Person person) {
		Object subscriptions = person.getAttributes().getAttribute(SUBSCRIPTIONS);
		return subscriptions == null ? null : subscriptions.toString();
	}

	private static boolean flag(Person person, String attribute) {
		Object value = person.getAttributes().getAttribute(attribute);
		return value instanceof Boolean flag ? flag : value != null && Boolean.parseBoolean(value.toString());
	}

	static public boolean hasGeneralSubscription(Person person) {
		String subscriptions = subscriptions(person);
		return subscriptions != null
				? org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils.hasGeneralSubscription(subscriptions)
				: flag(person, "ptHasGA");
	}

	static public boolean hasHalbtaxSubscription(Person person) {
		String subscriptions = subscriptions(person);
		return subscriptions != null
				? org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils.hasHalbtaxSubscription(subscriptions)
				: flag(person, "ptHasHalbtax");
	}

	static public boolean hasRegionalSubscription(Person person) {
		String subscriptions = subscriptions(person);
		return subscriptions != null
				? org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils.hasRegionalSubscription(subscriptions)
				: flag(person, "ptHasVerbund");
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
