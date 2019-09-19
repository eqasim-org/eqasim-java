package org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.variables.DAPersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class DAPersonPredictor extends CachedVariablePredictor<DAPersonVariables> {
	public final SwissPersonPredictor delegate;

	@Inject
	public DAPersonPredictor(SwissPersonPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected DAPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double householdIncome_MU = DAPredictorUtils.getHouseholdIncome(person);
		return new DAPersonVariables(delegate.predictVariables(person, trip, elements), householdIncome_MU);
	}
}
