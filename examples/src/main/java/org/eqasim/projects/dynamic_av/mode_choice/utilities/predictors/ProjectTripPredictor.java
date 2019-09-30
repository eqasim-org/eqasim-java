package org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectTripPredictor extends CachedVariablePredictor<ProjectTripVariables> {
	@Override
	protected ProjectTripVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		return new ProjectTripVariables(trip.getDestinationActivity().getType());
	}
}
