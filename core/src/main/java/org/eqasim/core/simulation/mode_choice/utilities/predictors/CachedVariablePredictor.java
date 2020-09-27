package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public abstract class CachedVariablePredictor<T extends BaseVariables> implements VariablePredictor<T> {
	private DiscreteModeChoiceTrip cacheKey = null;
	private T cacheValue = null;

	@Override
	public T predictVariables(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (trip != cacheKey) {
			cacheKey = trip;
			cacheValue = predict(person, trip, elements);
		}

		return cacheValue;
	}

	protected abstract T predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements);
}
