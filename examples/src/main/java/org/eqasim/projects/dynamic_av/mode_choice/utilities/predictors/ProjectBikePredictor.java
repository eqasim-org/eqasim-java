package org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.ProjectBikeVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class ProjectBikePredictor extends CachedVariablePredictor<ProjectBikeVariables> {
	public final BikePredictor delegate;

	@Inject
	public ProjectBikePredictor(BikePredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected ProjectBikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		BikeVariables delegateVariables = delegate.predictVariables(person, trip, elements);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new ProjectBikeVariables(delegateVariables, euclideanDistance_km);
	}
}
