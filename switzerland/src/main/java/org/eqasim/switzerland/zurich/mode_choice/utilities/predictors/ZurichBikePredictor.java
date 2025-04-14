package org.eqasim.switzerland.zurich.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichBikeVariables;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.google.inject.Inject;

public class ZurichBikePredictor extends CachedVariablePredictor<ZurichBikeVariables> {
	public final BikePredictor delegate;

	@Inject
	public ZurichBikePredictor(BikePredictor delegate) {
		this.delegate = delegate;
	}

	@Override
	protected ZurichBikeVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		double travelTime_min = ((Leg)elements.get(2)).getTravelTime().seconds() / 60.0;
      
		BikeVariables delegateVariables = new BikeVariables(travelTime_min);
		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

		return new ZurichBikeVariables(delegateVariables, euclideanDistance_km);
	}
}