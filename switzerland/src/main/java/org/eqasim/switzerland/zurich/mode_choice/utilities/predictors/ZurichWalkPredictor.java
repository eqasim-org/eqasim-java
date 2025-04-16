package org.eqasim.switzerland.zurich.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.WalkPredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichWalkVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;


public class ZurichWalkPredictor extends CachedVariablePredictor<ZurichWalkVariables> {
	public final WalkPredictor delegate;

	@Inject
	public ZurichWalkPredictor(WalkPredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected ZurichWalkVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		return new ZurichWalkVariables(delegate.predictVariables(person, trip, elements), euclideanDistance_km);
	}
}