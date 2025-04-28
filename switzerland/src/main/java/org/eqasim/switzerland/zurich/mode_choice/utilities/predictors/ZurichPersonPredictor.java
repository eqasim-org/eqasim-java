package org.eqasim.switzerland.zurich.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;


public class ZurichPersonPredictor extends CachedVariablePredictor<ZurichPersonVariables> {
	public final SwissPersonPredictor delegate;

	@Inject
	public ZurichPersonPredictor(SwissPersonPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected ZurichPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double householdIncome_MU = ZurichPredictorUtils.getHouseholdIncome(person);
		return new ZurichPersonVariables(delegate.predictVariables(person, trip, elements), householdIncome_MU);
	}
}