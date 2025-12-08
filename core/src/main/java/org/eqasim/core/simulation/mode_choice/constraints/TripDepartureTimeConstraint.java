package org.eqasim.core.simulation.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.Collection;
import java.util.List;

public class TripDepartureTimeConstraint extends AbstractTourConstraint {

    private final TimeInterpretation timeInterpretation;

    public TripDepartureTimeConstraint(TimeInterpretation timeInterpretation) {
        this.timeInterpretation = timeInterpretation;
    }

    public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate, List<TourCandidate> previousCandidates) {
        TimeTracker timeTracker = new  TimeTracker(timeInterpretation);
        for(int i=0; i<tour.size(); i++) {
            DiscreteModeChoiceTrip trip =  tour.get(i);
            if (i == 0) {
                timeTracker.addActivity(trip.getOriginActivity());
            }
            if(trip.getDepartureTime() < timeTracker.getTime().seconds()) {
                return false;
            }
            TripCandidate tripCandidate = candidate.getTripCandidates().get(i);
            timeTracker.addDuration(tripCandidate.getDuration());
            timeTracker.addActivity(trip.getDestinationActivity());
        }
        return true;
    }


    public static class Factory implements TourConstraintFactory {
        private final TimeInterpretation timeInterpretation;

        public Factory(TimeInterpretation timeInterpretation) {
            this.timeInterpretation = timeInterpretation;
        }

        @Override
        public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new TripDepartureTimeConstraint(timeInterpretation);
        }
    }
}
