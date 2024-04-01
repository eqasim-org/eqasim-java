package org.eqasim.core.simulation.mode_choice.filters;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourFilter;

import java.util.List;

public class OutsideTripsOnlyTourFilter implements TourFilter {

    @Override
    public boolean filter(Person person, List<DiscreteModeChoiceTrip> tour) {
        return tour.stream().map(DiscreteModeChoiceTrip::getInitialMode).anyMatch(mode -> !mode.equals("outside"));
    }
}
