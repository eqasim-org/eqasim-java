package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

/**
 * This class is a TourEstimator which is based on a TripEstimator. Every trip
 * in the tour is estimated by the underlying TripEstimator and utilities are
 * summed up to arrive at a total utility for the whole tour.
 *
 * @author sebhoerl
 */
public class CumulativeTourEstimator implements TourEstimator {
    private final TimeInterpretation timeInterpretation;
    private final TripEstimator delegate;
    private final TripConstraintFactory tripConstraintFactory;
    private final ModeAvailability modeAvailability;

    public CumulativeTourEstimator(TripEstimator delegate, TimeInterpretation timeInterpretation, TripConstraintFactory tripConstraintFactory, ModeAvailability modeAvailability) {
        this.delegate = delegate;
        this.timeInterpretation = timeInterpretation;
        this.tripConstraintFactory = tripConstraintFactory;
        this.modeAvailability = modeAvailability;
    }

    @Override
    public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
                                      List<TourCandidate> preceedingTours) {
        return this.estimateTour(person, modes, trips, preceedingTours, false);
    }

    @Override
    public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips,
                                      List<TourCandidate> preceedingTours, boolean validate) {
        validate = false;
        TripConstraint tripConstraint = null;
        List<TripCandidate> previousCandidates = new ArrayList<>();
        if(validate) {
            tripConstraint = this.tripConstraintFactory.createConstraint(person, trips,  this.modeAvailability.getAvailableModes(person, trips));
        }
        List<TripCandidate> tripCandidates = new LinkedList<>();
        double utility = 0.0;

        TimeTracker timeTracker = new TimeTracker(timeInterpretation);
        timeTracker.setTime(trips.get(0).getDepartureTime());

        for (int i = 0; i < modes.size(); i++) {
            String mode = modes.get(i);
            DiscreteModeChoiceTrip trip = trips.get(i);

            if (i > 0) { // We're already at the end of the first origin activity
                timeTracker.addActivity(trip.getOriginActivity());
                trip.setDepartureTime(timeTracker.getTime().seconds());
            }
            TripCandidate tripCandidate = delegate.estimateTrip(person, mode, trip, tripCandidates);
            if(validate) {
                if(!tripConstraint.validateAfterEstimation(trip, tripCandidate, previousCandidates)) {
                    return null;
                }
                previousCandidates.add(tripCandidate);
            }
            utility += tripCandidate.getUtility();
            timeTracker.addDuration(tripCandidate.getDuration());

            tripCandidates.add(tripCandidate);
        }

        return new DefaultTourCandidate(utility, tripCandidates);
    }
}
