package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class SwissPersonPredictor extends CachedVariablePredictor<SwissPersonVariables> {
	@Override
	protected SwissPersonVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {
		Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);
		boolean hasGeneralSubscription = SwissPredictorUtils.hasGeneralSubscription(person);
		boolean hasHalbtaxSubscription = SwissPredictorUtils.hasHalbtaxSubscription(person);
		boolean hasRegionalSubscription = SwissPredictorUtils.hasRegionalSubscription(person);
		int statedPreferenceRegion = SwissPredictorUtils.getStatedPreferenceRegion(person);

		return new SwissPersonVariables(homeLocation, hasGeneralSubscription, hasHalbtaxSubscription,
				hasRegionalSubscription, statedPreferenceRegion);
	}
}
