package org.eqasim.switzerland.mode_choice.utilities.predictors;

import org.eqasim.switzerland.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;

public class SwissPersonPredictor {
	public SwissPersonVariables predictVariables(Person person) {
		boolean hasGeneralSubscription = SwissPredictorUtils.hasGeneralSubscription(person);
		boolean hasHalbtaxSubscription = SwissPredictorUtils.hasHalbtaxSubscription(person);
		boolean hasRegionalSubscription = SwissPredictorUtils.hasRegionalSubscription(person);

		int statedPreferenceRegion = SwissPredictorUtils.getStatedPreferenceRegion(person);

		Coord homeLocation = SwissPredictorUtils.getHomeLocation(person);

		return new SwissPersonVariables(hasGeneralSubscription, hasHalbtaxSubscription, hasRegionalSubscription,
				statedPreferenceRegion, homeLocation);
	}
}
