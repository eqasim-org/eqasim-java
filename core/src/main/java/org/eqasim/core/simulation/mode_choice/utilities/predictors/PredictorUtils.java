package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

public class PredictorUtils {
	private PredictorUtils() {
	}

	static public double calculateEuclideanDistance_km(DiscreteModeChoiceTrip trip) {
		return CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord()) * 1e-3;
	}

	static public int getAge(Person person) {
		return (int) (Integer) person.getAttributes().getAttribute("age");
	}
}
