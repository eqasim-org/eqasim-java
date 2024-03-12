package org.eqasim.core.simulation.mode_choice.tour_finder;

import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This tour finder works similarly to the ActivityTourFinder with the difference that it allows to build tours guaranteeing that activities of certain types are excluded, thus preventing trips related to these activities from being considered in the mode choice.
 * The typical use case for this tour finder is to exclude related to 'outside' activities from being altered.
 * For example, using the ActivityTourFinderWithExcludedActivities with excludedActivityTypes={outside} and borderActivityTypes={home} on the following trip sequence:
 * home -> leisure -> outside -> work -> shop -> home
 * would produce the following "tours" home -> leisure, and work -> shop -> home
 * This allows to consider more trips in the mode choice than when using the ActivityTourFinder with activityTypes={home, outside} coupled with the OutsideTourFilter which will filter out all the tours resulting from the previous trip sequence
 */
public class ActivityTourFinderWithExcludedActivities implements TourFinder {

    private final Set<String> borderActivityTypes;
    private final Set<String> excludedActivityTypes;
    private final ActivityTourFinder delegate;

    public ActivityTourFinderWithExcludedActivities(Collection<String> borderActivityTypes, Collection<String> excludedActivityTypes) {
        this.borderActivityTypes = new HashSet<>(borderActivityTypes);
        this.excludedActivityTypes = new HashSet<>(excludedActivityTypes);
        this.delegate = new ActivityTourFinder(this.borderActivityTypes);
    }

    @Override
    public List<List<DiscreteModeChoiceTrip>> findTours(List<DiscreteModeChoiceTrip> trips) {
        List<List<DiscreteModeChoiceTrip>> baseTours = this.delegate.findTours(trips);
        List<List<DiscreteModeChoiceTrip>> tours = baseTours.stream().flatMap(tour -> this.isolateActivityTrips(tour).stream()).collect(Collectors.toList());
        return tours;
    }

    private List<List<DiscreteModeChoiceTrip>> isolateActivityTrips(List<DiscreteModeChoiceTrip> trips) {
        List<List<DiscreteModeChoiceTrip>> isolatedTrips = new ArrayList<>();
        ArrayList<Boolean> isolating = new ArrayList<>();
        for(DiscreteModeChoiceTrip currentTrip: trips) {
            if(this.excludedActivityTypes.contains(currentTrip.getOriginActivity().getType()) || this.excludedActivityTypes.contains(currentTrip.getDestinationActivity().getType())) {
                isolating.add(true);
            } else {
                isolating.add(false);
            }
        }
        List<DiscreteModeChoiceTrip> currentTour = new ArrayList<>();
        isolatedTrips.add(currentTour);

        boolean isIsolating = isolating.get(0);
        for(int i=0; i<trips.size(); i++) {
            if(isIsolating != isolating.get(i)) {
                isIsolating = isolating.get(i);
                currentTour = new ArrayList<>();
                isolatedTrips.add(currentTour);
            }
            currentTour.add(trips.get(i));
        }
        return isolatedTrips;
    }
}
