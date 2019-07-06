package org.eqasim.simulation.ile_de_france.preparation;

import java.util.Map;

import org.eqasim.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class ImputeInnerParisAttribute {
	private final Map<String, IRIS> iris;

	public ImputeInnerParisAttribute(Map<String, IRIS> iris) {
		this.iris = iris;
	}

	public void run(Population population) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Imputing innerParis attribute ...",
				population.getPersons().size());
		progress.start();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						activity.getAttributes().putAttribute("innerParis", isCoveredByIRIS(activity));
					}
				}
			}

			progress.update();
		}

		progress.close();
	}

	private boolean isCoveredByIRIS(Activity activity) {
		for (IRIS candidate : iris.values()) {
			if (candidate.containsCoordinate(activity.getCoord())) {
				return true;
			}
		}

		return false;
	}
}