package org.eqasim.core.simulation.mode_choice.tour_finder;

import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.*;
import java.util.stream.Collectors;


/**
 * This tour finder works similarly to the ActivityTourFinder with the difference that it allows to build tours guaranteeing that activities of certain types are excluded, thus preventing trips related to these activities from being considered in the mode choice.
 * This tour finder relies on a delegate tour finder to first build a set of tours. Then a process is applied on those tours to further split them in sequences of trips not containing trips related to excluded activities and sequences containing only such trips.
 * The typical use case for this tour finder is to exclude trips related to 'outside' activities from being altered.
 * This can be done by using the {@link org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder} with activityTypes={home, outside} as a delegate tour finder and 'outside' as an excluded activity type.
 * Using the above construction on the plan 'home -> leisure -> outside -> work -> shop -> home' will produce the following 'tours' (separated with ;)
 * home -> leisure; leisure -> outside; outside -> work; work -> shop -> hope.
 * Using the {@link org.eqasim.core.simulation.mode_choice.filters.OutsideFilter} then allows to consider the first and last 'tours' for mode choice.
 * This then allows to consider more trips in the mode choice than when using the base ActivityTourFinder coupled with the OutsideTourFilter which will divide the previous plan
 * into the two 'tours' home -> leisure -> outside and outside -> work -> shop -> home and filter both of them out.
 */
public class ActivityTourFinderWithExcludedActivities implements TourFinder {

    private final Set<String> excludedActivityTypes;
    private final ActivityTourFinder delegate;

    public ActivityTourFinderWithExcludedActivities(Collection<String> excludedActivityTypes, ActivityTourFinder delegate) {
        this.excludedActivityTypes = new HashSet<>(excludedActivityTypes);
        this.delegate = delegate;
    }

    @Override
    public List<List<DiscreteModeChoiceTrip>> findTours(List<DiscreteModeChoiceTrip> trips) {
        List<List<DiscreteModeChoiceTrip>> baseTours = this.delegate.findTours(trips);
        return baseTours.stream().flatMap(tour -> this.isolateActivityTrips(tour).stream()).collect(Collectors.toList());
    }

    private List<List<DiscreteModeChoiceTrip>> isolateActivityTrips(List<DiscreteModeChoiceTrip> trips) {
        List<List<DiscreteModeChoiceTrip>> tours = new ArrayList<>();

        boolean isExcluding;
        boolean wasExcluding = this.isTripExcluded(trips.get(0));

        List<DiscreteModeChoiceTrip> currentTour = new ArrayList<>();

        for(int i=0; i<trips.size(); i++) {
            DiscreteModeChoiceTrip currentTrip = trips.get(i);
            if (i>0) {
                isExcluding = this.isTripExcluded(currentTrip);
                if(isExcluding != wasExcluding) {
                    tours.add(currentTour);
                    wasExcluding = isExcluding;
                    currentTour = new ArrayList<>();
                }
            }
            currentTour.add(trips.get(i));
        }

        if(currentTour.size() > 0) {
            tours.add(currentTour);
        }
        return tours;
    }

    private boolean isTripExcluded(DiscreteModeChoiceTrip trip) {
        return this.excludedActivityTypes.contains(trip.getOriginActivity().getType()) || this.excludedActivityTypes.contains(trip.getDestinationActivity().getType());
    }
}
