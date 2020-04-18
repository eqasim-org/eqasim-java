package org.eqasim.sao_paulo.preparation;

import java.util.Set;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class ImputeInnerSPAttribute {
	private final Set<SPTract> spTracts;

	public ImputeInnerSPAttribute(Set<SPTract> spTracts) {
		this.spTracts = spTracts;
	}

	public void run(Population population, String attributeName) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Imputing innerSP attribute ...",
				population.getPersons().size());
		progress.start();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						activity.getAttributes().putAttribute(attributeName, isCoveredByIRIS(activity));
					}
				}
			}

			progress.update();
		}

		progress.close();
	}

	private boolean isCoveredByIRIS(Activity activity) {
		for (SPTract candidate : this.spTracts) {
			if (candidate.containsCoordinate(activity.getCoord())) {
				return true;
			}
		}

		return false;
	}
}