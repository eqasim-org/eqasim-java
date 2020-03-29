package org.eqasim.projects.astra16.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.projects.astra16.mode_choice.variables.AstraBikeVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AstraBikePredictor extends CachedVariablePredictor<AstraBikeVariables> {
	public final BikePredictor delegate;

	@Inject
	public AstraBikePredictor(BikePredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected AstraBikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		BikeVariables delegateVariables = delegate.predictVariables(person, trip, elements);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new AstraBikeVariables(delegateVariables, euclideanDistance_km);
	}
}
