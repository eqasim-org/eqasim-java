package org.eqasim.core.simulation.modes.parking_aware_car.mode_choice;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty.ParkingAwareCarPenaltyProvider;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.ParkingAwareNetworkRoutingModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.DefaultTourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParkingAwareCumulativeTourEstimator implements TourEstimator {

    public static final String NAME = "ParkingAwareCumulativeTourEstimator";

    public record ParkingUsage(ParkingSpace parkingSpace, double entryTime, double exitTime) {

    }

    private final TripEstimator delegate;
    private final TimeInterpretation timeInterpretation;
    private final String mode;
    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final ParkingAwareCarPenaltyProvider parkingAwareCarPenaltyProvider;

    public ParkingAwareCumulativeTourEstimator(TripEstimator delegate, TimeInterpretation timeInterpretation, String mode, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingAwareCarPenaltyProvider parkingAwareCarPenaltyProvider) {
        this.delegate = delegate;
        this.timeInterpretation = timeInterpretation;
        this.mode = mode;
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.parkingAwareCarPenaltyProvider = parkingAwareCarPenaltyProvider;
    }

    @Override
    public TourCandidate estimateTour(Person person, List<String> modes, List<DiscreteModeChoiceTrip> trips, List<TourCandidate> previousTours) {
        List<TripCandidate> tripCandidates = new LinkedList<>();
        double utility = 0.0;

        TimeTracker timeTracker = new TimeTracker(timeInterpretation);
        timeTracker.setTime(trips.get(0).getDepartureTime());

        ParkingSpace currentParkingSpace = null;
        double currentEntryTime = -1;

        List<ParkingUsage> parkingUsages = new ArrayList<>();

        for (int i = 0; i < modes.size(); i++) {
            String mode = modes.get(i);
            DiscreteModeChoiceTrip trip = trips.get(i);

            if (i > 0) { // We're already at the end of the first origin activity
                timeTracker.addActivity(trip.getOriginActivity());
                trip.setDepartureTime(timeTracker.getTime().seconds());
            }

            if(mode.equals(this.mode) && currentParkingSpace != null) {
                parkingUsages.add(new ParkingUsage(currentParkingSpace, currentEntryTime, timeTracker.getTime().seconds()));
            }

            TripCandidate tripCandidate = delegate.estimateTrip(person, mode, trip, tripCandidates);
            utility += tripCandidate.getUtility();
            timeTracker.addDuration(tripCandidate.getDuration());

            if(mode.equals(this.mode) && (tripCandidate instanceof DefaultRoutedTripCandidate defaultRoutedTripCandidate)) {

                Id<Link> parkingLinkId = null;
                Id<ParkingType> parkingTypeId = null;
                List<? extends PlanElement> elements = defaultRoutedTripCandidate.getRoutedPlanElements();
                for(int j=elements.size()-1; j>=0; j--) {
                    if(elements.get(j) instanceof Leg leg && leg.getMode().equals(mode)) {
                        parkingLinkId = leg.getRoute().getEndLinkId();
                        parkingTypeId = (Id<ParkingType>) leg.getAttributes().getAttribute(ParkingAwareNetworkRoutingModule.PARKING_TYPE_ATTR);
                        break;
                    }
                }
                if(parkingTypeId.equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
                    currentParkingSpace = null;
                    currentEntryTime = -1;
                } else {
                    currentParkingSpace = this.networkWideParkingSpaceStore.getLinkParkingSpaces(parkingLinkId).get(parkingTypeId);
                    currentEntryTime = timeTracker.getTime().seconds();
                }
            }

            tripCandidates.add(tripCandidate);
        }

        if(currentParkingSpace != null) {
            parkingUsages.add(new ParkingUsage(currentParkingSpace, currentEntryTime, timeTracker.getTime().seconds()));
        }

        double parkingPenalty = parkingUsages.stream().mapToDouble(usage -> parkingAwareCarPenaltyProvider.getPenalty(person.getId(), usage.parkingSpace.linkId(), usage.entryTime, usage.exitTime, usage.parkingSpace.parkingType().id())).sum();

        return new DefaultTourCandidate(utility - parkingPenalty, tripCandidates);
    }
}
