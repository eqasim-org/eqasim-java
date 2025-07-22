package org.eqasim.switzerland.zurich.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.zurich.mode_choice.utilities.variables.ZurichTripVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;


public class ZurichTripPredictor extends CachedVariablePredictor<ZurichTripVariables> {
	@Override
	protected ZurichTripVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		boolean isWork = ZurichPredictorUtils.hasPurposeWork(trip.getOriginActivity())
				|| ZurichPredictorUtils.hasPurposeWork(trip.getDestinationActivity());

		boolean isCity = ZurichPredictorUtils.isInsideCity(trip.getOriginActivity())
				|| ZurichPredictorUtils.isInsideCity(trip.getDestinationActivity());

		return new ZurichTripVariables(isWork, isCity);
	}
}