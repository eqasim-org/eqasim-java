package org.eqasim.core.simulation.mode_choice.tour_finder;

import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.ArrayList;
import java.util.List;

public class IsolatedOutsideRelatedTripsTourFinder implements TourFinder {


    // For a sequence outside -> work -> shop -> leisure -> outside
    // This tour finder returns one tour work -> shop -> leisure

    @Override
    public List<List<DiscreteModeChoiceTrip>> findTours(List<DiscreteModeChoiceTrip> trips) {
        List<List<DiscreteModeChoiceTrip>> tours = new ArrayList<>();
        List<DiscreteModeChoiceTrip> currentTour = null;

        boolean previousTripRelatedToOutside = false;

        for(int i=0; i<trips.size(); i++) {
            DiscreteModeChoiceTrip currentTrip = trips.get(i);
            boolean startingNewTour = false;
            if(currentTrip.getOriginActivity().getType().equals("home")) {
                startingNewTour = true;
            }
            boolean newPreviousTripRelatedToOutside;
            if(currentTrip.getDestinationActivity().getType().equals("outside") || currentTrip.getOriginActivity().getType().equals("outside")) {
                startingNewTour = true;
                newPreviousTripRelatedToOutside = true;
            } else {
                newPreviousTripRelatedToOutside = false;
            }
            if(startingNewTour || currentTour == null || previousTripRelatedToOutside) {
                currentTour = new ArrayList<>();
                tours.add(currentTour);
            }
            currentTour.add(currentTrip);
            previousTripRelatedToOutside = newPreviousTripRelatedToOutside;
        }
        return tours;
    }
}
