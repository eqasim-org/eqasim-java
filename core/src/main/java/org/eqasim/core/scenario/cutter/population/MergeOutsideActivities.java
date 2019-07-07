package org.eqasim.core.scenario.cutter.population;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;

public interface MergeOutsideActivities {
	void run(List<PlanElement> plan);
}
