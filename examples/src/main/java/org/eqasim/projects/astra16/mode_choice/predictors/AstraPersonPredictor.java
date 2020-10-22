package org.eqasim.projects.astra16.mode_choice.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.projects.astra16.mode_choice.variables.AstraPersonVariables;
import org.eqasim.switzerland.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class AstraPersonPredictor extends CachedVariablePredictor<AstraPersonVariables> {
	public final SwissPersonPredictor delegate;

	@Inject
	public AstraPersonPredictor(SwissPersonPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected AstraPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double householdIncome_MU = AstraPredictorUtils.getHouseholdIncome(person);
		return new AstraPersonVariables(delegate.predictVariables(person, trip, elements), householdIncome_MU);
	}
}
