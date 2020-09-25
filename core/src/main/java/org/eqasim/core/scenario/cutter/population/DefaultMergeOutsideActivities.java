package org.eqasim.core.scenario.cutter.population;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public class DefaultMergeOutsideActivities implements MergeOutsideActivities {
	@Override
	public void run(List<PlanElement> plan) {
		while (plan.size() > 1 && ((Activity) plan.get(0)).getType().equals("outside")
				&& ((Leg) plan.get(1)).getMode().equals("outside")
				&& ((Activity) plan.get(2)).getType().equals("outside")) {
			// While the first two activities are outside, remove the first one and the
			// following leg
			plan.remove(0);
			plan.remove(0);
		}

		while (plan.size() > 1 && ((Activity) plan.get(plan.size() - 3)).getType().equals("outside")
				&& ((Leg) plan.get(plan.size() - 2)).getMode().equals("outside")
				&& ((Activity) plan.get(plan.size() - 1)).getType().equals("outside")) {
			// While the last two activities are outside, remove the last one and the
			// preceeding leg
			plan.remove(plan.size() - 1);
			plan.remove(plan.size() - 1);
		}

		for (int i = 0; i < plan.size() - 4; i += 2) {
			// As long as there can be found three following outside activities, remove the
			// middle one and the preceeding leg

			Activity firstActivity = (Activity) plan.get(i);
			Leg firstLeg = (Leg) plan.get(i + 1);
			Activity secondActivity = (Activity) plan.get(i + 2);
			Leg secondLeg = (Leg) plan.get(i + 3);
			Activity thirdActivity = (Activity) plan.get(i + 4);

			boolean firstActivityIsOutside = firstActivity.getType().equals("outside");
			boolean firstLegIsOutside = firstLeg.getMode().equals("outside");
			boolean secondActivityIsOutside = secondActivity.getType().equals("outside");
			boolean secondLegIsOutside = secondLeg.getMode().equals("outside");
			boolean thirdActivityIsOutside = thirdActivity.getType().equals("outside");

			if (firstActivityIsOutside && firstLegIsOutside && secondActivityIsOutside && secondLegIsOutside
					&& thirdActivityIsOutside) {
				// We can delete the one in the middle
				plan.remove(i + 1);
				plan.remove(i + 1);

				// Check the current one again.
				i -= 2;

				firstActivity.setEndTime(secondActivity.getEndTime().seconds());
			}
		}

		if (plan.size() == 1) {
			Activity activity = (Activity) plan.get(0);

			if (activity.getType().equals("outside")) {
				plan.clear();
			}
		}
	}
}
