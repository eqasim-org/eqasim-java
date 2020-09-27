package org.eqasim.core.simulation.mode_choice.filters;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;

public class OutsideFilter implements TourFilter {
	@Override
	public boolean filter(Person person, List<DiscreteModeChoiceTrip> tour) {
		DiscreteModeChoiceTrip start = tour.get(0);
		DiscreteModeChoiceTrip end = tour.get(tour.size() - 1);

		if (start.getOriginActivity().getType().equals("outside")) {
			return false;
		}

		if (end.getDestinationActivity().getType().equals("outside")) {
			return false;
		}

		return true;
	}
}
