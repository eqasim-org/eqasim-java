package org.eqasim.san_francisco.preparation;

import java.util.Set;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class ImputeInnerSFAttribute {
	private final Set<SFTract> sfTracts;

	public ImputeInnerSFAttribute(Set<SFTract> sfTracts) {
		this.sfTracts = sfTracts;
	}

	public void run(Population population) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Imputing innerSF attribute ...",
				population.getPersons().size());
		progress.start();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						activity.getAttributes().putAttribute("city", isCoveredByIRIS(activity));
					}
				}
			}

			progress.update();
		}

		progress.close();
	}

	private boolean isCoveredByIRIS(Activity activity) {
		for (SFTract candidate : this.sfTracts) {
			if (candidate.containsCoordinate(activity.getCoord())) {
				return true;
			}
		}

		return false;
	}
}