package org.eqasim.ile_de_france.policies;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class CarPtTourConstraint implements TourConstraint{

    private final Id<? extends BasicLocation> vehicleLocationId;
    private final MultimodalLinkChooser linkChooser;
    private final SwissRailRaptorData data;

    public CarPtTourConstraint(Id<? extends BasicLocation> vehicleLocationId, MultimodalLinkChooser linkChooser, SwissRailRaptorData data) {
        this.vehicleLocationId = vehicleLocationId;
        this.linkChooser = linkChooser;
        this.data = data;
    }

	private int getFirstIndex(String mode, List<String> modes) {
		for (int i = 0; i < modes.size(); i++) {
			if (modes.get(i).equals(mode)) {
				return i;
			}
		}

		return -1;
	}

	private int getLastIndex(String mode, List<String> modes) {
		for (int i = modes.size() - 1; i >= 0; i--) {
			if (modes.get(i).equals(mode)) {
				return i;
			}
		}

		return -1;
	}

    private void setParkingLink(Person person, DiscreteModeChoiceTrip trip){
        TransitStopFacility stopFacility = data.findNearestStop(trip.getOriginActivity().getCoord().getX(), trip.getOriginActivity().getCoord().getY());
        Link parkingLink = linkChooser.decideOnLink(stopFacility, null, person);
        trip.getTripAttributes().putAttribute("parking", parkingLink);
    }

	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes) {
        // Requires passing Person as an argument
		return false;
	}


	@Override
	public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes, Person person) {
        if (modes.contains("car_pt")) {
            if (modes.stream().filter(mode -> mode.equals("car_pt")).count() % 2 != 0){
                return false;
            }
            // I) Make sure vehicle is picked up and dropped off at its predetermined home
            // base. If the chain does not start at the vehicle base, the vehicle may also
            // be picked up at the first activity. If the chain does not end at the vehicle
            // base, the vehicle may still be dropped off at the last activity.

            int firstIndex = getFirstIndex("car_pt", modes);
            int lastIndex = getLastIndex("car_pt", modes);

            Id<? extends BasicLocation> startLocationId = LocationUtils
                    .getLocationId(tour.get(firstIndex).getOriginActivity());
            Id<? extends BasicLocation> endLocationId = LocationUtils
                    .getLocationId(tour.get(lastIndex).getDestinationActivity());

            if (!startLocationId.equals(vehicleLocationId)) {
                // Vehicle does not depart at the depot

                if (firstIndex > 0) {
                    // If vehicle starts at very first activity, we still allow this tour!
                    return false;
                }
                else if (modes.get(0).equals("car_pt")){
                    DiscreteModeChoiceTrip trip = tour.get(0);
                    trip.getTripAttributes().putAttribute("car_pt", "ACCESS");
                    setParkingLink(person, trip);
                }
            }

            if (startLocationId.equals(vehicleLocationId) && modes.get(0).equals("car_pt")){
                DiscreteModeChoiceTrip trip = tour.get(0);
                trip.getTripAttributes().putAttribute("car_pt", "ACCESS");
                setParkingLink(person, trip);
            }

            if (!endLocationId.equals(vehicleLocationId)) {
                // Vehicle does not end at the depot

                if (lastIndex < modes.size() - 1) {
                    // If vehicle ends at the very last activity, we still allow this tour!
                    return false;
                }
                else if (modes.get(lastIndex).equals("car_pt")){
                    tour.get(lastIndex).getTripAttributes().putAttribute("car_pt", "EGRESS");
                }
            }

            if (endLocationId.equals(vehicleLocationId) && modes.get(lastIndex).equals("car_pt")){
                tour.get(lastIndex).getTripAttributes().putAttribute("car_pt", "EGRESS");
            }

            // II) Make sure that in between the vehicle is only picked up at the location
            // where it has been moved previously

            Id<? extends BasicLocation> currentLocationId = LocationUtils
                    .getLocationId(tour.get(firstIndex).getDestinationActivity());

            boolean foundAccess = true;
            int lastAccessIndex = 0;

            for (int index = firstIndex + 1; index <= lastIndex; index++) {
                if (modes.get(index).equals("car_pt")) {
                    DiscreteModeChoiceTrip trip = tour.get(index);

                    if (!currentLocationId.equals(LocationUtils.getLocationId(trip.getOriginActivity()))) {
                        return false;
                    }

                    currentLocationId = LocationUtils.getLocationId(trip.getDestinationActivity());
        
                    if (foundAccess){
                        trip.getTripAttributes().putAttribute("car_pt", "EGRESS");
                        Link parkingLink = (Link) tour.get(lastAccessIndex).getTripAttributes().getAttribute("parking");
                        trip.getTripAttributes().putAttribute("parking", parkingLink);

                        foundAccess = false;
                    }
                    else {
                        trip.getTripAttributes().putAttribute("car_pt", "ACCESS");
                        setParkingLink(person, trip);
                        foundAccess = true;
                        lastAccessIndex = index;
                    }
                }
            }
        }
		return true;
	}

	@Override
	public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
			List<TourCandidate> previousCandidates) {
		return true;
	}


    public static class Factory implements TourConstraintFactory {
        private final HomeFinder homeFinder;
        private final MultimodalLinkChooser linkChooser;
        private final SwissRailRaptorData data;

        public Factory(HomeFinder homeFinder, MultimodalLinkChooser linkChooser, SwissRailRaptorData data) {
            this.homeFinder = homeFinder;
            this.linkChooser = linkChooser;
            this.data = data;
        }

        @Override
        public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                Collection<String> availableModes) {
            return new CarPtTourConstraint(homeFinder.getHomeLocationId(planTrips), linkChooser, data);
        }
    }

}
