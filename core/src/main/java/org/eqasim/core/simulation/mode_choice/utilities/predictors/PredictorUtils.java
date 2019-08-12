package org.eqasim.core.simulation.mode_choice.utilities.predictors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class PredictorUtils {
	static public int getAge(Person person) {
		Integer age = (Integer) person.getAttributes().getAttribute("age");
		return age == null ? -1 : age;
	}

	static public double getEuclideanDistance_km(DiscreteModeChoiceTrip trip) {
		return CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord()) * 1e-3;
	}
}
