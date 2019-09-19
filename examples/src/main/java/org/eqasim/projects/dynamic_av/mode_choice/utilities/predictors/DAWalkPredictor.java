package org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DAWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class DAWalkPredictor extends CachedVariablePredictor<DAWalkVariables> {
	public final WalkPredictor delegate;

	@Inject
	public DAWalkPredictor(WalkPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected DAWalkVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		return new DAWalkVariables(delegate.predict(person, trip, elements), euclideanDistance_km);
	}
}
