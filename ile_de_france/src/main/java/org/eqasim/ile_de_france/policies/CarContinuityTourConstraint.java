package org.eqasim.ile_de_france.policies;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.components.utils.home_finder.HomeFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Distance-based Car continuity constraint
 * Checks if car is in a 800m radius when car is used as a mode
 * 
 * @author akramelb
 */
public class CarContinuityTourConstraint implements TourConstraint{

    private final MultimodalLinkChooser linkChooser;
    private final QuadTree<TransitStopFacility> railStops;


    public CarContinuityTourConstraint(Id<? extends BasicLocation> vehicleLocationId, MultimodalLinkChooser linkChooser, QuadTree<TransitStopFacility> railStops) {
        this.linkChooser = linkChooser;
        this.railStops = railStops;
    }

    @Override
    public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
            List<List<String>> previousModes) {
        return false;
    }

    public boolean isCarAvailable(Person person, Link parkingLink, DiscreteModeChoiceTrip currentTrip){
        if (parkingLink == null){
            if (person.getAttributes().getAttribute("carLocation") == null || person.getAttributes().getAttribute("carLocation").equals("home")){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            Coord parkingCoord = parkingLink.getCoord();
            Coord tripOriginCoord = currentTrip.getOriginActivity().getCoord();
            if (CoordUtils.calcEuclideanDistance(parkingCoord, tripOriginCoord) < 800){
                return true;
            }
            else {
                return false;
            }
        }
    }

    @Override
    public boolean validateBeforeEstimation(List<DiscreteModeChoiceTrip> tour, List<String> modes,
			List<List<String>> previousModes, Person person) {
        if (modes.contains("car_pt") || modes.contains("car")){

            Link firstAccessLink = null;
            int previousCarIndex = -1;
            Link lastEgressLink = null;
            boolean carAvailable = isCarAvailable(person, null, null);

            for (int i = 0; i < tour.size(); i++){
                String mode = modes.get(i);
                DiscreteModeChoiceTrip trip = tour.get(i);

                if (previousCarIndex == -1){
                    if (mode.equals("car_pt")){
                        if (carAvailable){

                            trip.getTripAttributes().putAttribute("car_pt", "ACCESS");
    
                            Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), null);
                            Link accessLink = linkChooser.decideOnLink(originFacility, null, person);
                            trip.getTripAttributes().putAttribute("access_link", accessLink);
                            
                            TransitStopFacility stopFacility = railStops.getClosest(trip.getOriginActivity().getCoord().getX(), trip.getOriginActivity().getCoord().getY());
                            Link egressLink = linkChooser.decideOnLink(stopFacility, null, person);
                            trip.getTripAttributes().putAttribute("egress_link", egressLink);
    
                            previousCarIndex = i;
                            lastEgressLink = egressLink;
                            firstAccessLink = accessLink;
                        }
                        else {
                            trip.getTripAttributes().putAttribute("car_pt", "EGRESS");
    
                            TransitStopFacility stopFacility = railStops.getClosest(trip.getDestinationActivity().getCoord().getX(), trip.getDestinationActivity().getCoord().getY());
                            Link accessLink = linkChooser.decideOnLink(stopFacility, null, person);
                            trip.getTripAttributes().putAttribute("access_link", accessLink);
    
                            Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), null);
                            Link egressLink = linkChooser.decideOnLink(destinationFacility, null, person);
                            trip.getTripAttributes().putAttribute("egress_link", egressLink);
    
                            previousCarIndex = i;
                            lastEgressLink = egressLink;
                            firstAccessLink = accessLink;
                        }
                    }
                    else if (mode.equals("car")){
                        if (!carAvailable){
                            return false;
                        }
                        Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), null);
                        Link accessLink = linkChooser.decideOnLink(originFacility, null, person);
                        trip.getTripAttributes().putAttribute("access_link", accessLink);

                        Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), null);
                        Link egressLink = linkChooser.decideOnLink(destinationFacility, null, person);
                        trip.getTripAttributes().putAttribute("egress_link", egressLink);
                        
                        previousCarIndex = i;
                        lastEgressLink = egressLink;
                        firstAccessLink = accessLink;
                    }
                }

                else {
                    carAvailable = isCarAvailable(person, lastEgressLink, trip);
                    if (mode.equals("car_pt")){
                        if (carAvailable){
                            trip.getTripAttributes().putAttribute("car_pt", "ACCESS");
                            trip.getTripAttributes().putAttribute("access_link", lastEgressLink);

                            TransitStopFacility stopFacility = railStops.getClosest(trip.getOriginActivity().getCoord().getX(), trip.getOriginActivity().getCoord().getY());
                            Link egressLink = linkChooser.decideOnLink(stopFacility, null, person);
                            trip.getTripAttributes().putAttribute("egress_link", egressLink);

                            previousCarIndex = i;
                            lastEgressLink = egressLink;
                            carAvailable = false;
                        }
                        else {
                            trip.getTripAttributes().putAttribute("car_pt", "EGRESS");
                            trip.getTripAttributes().putAttribute("access_link", lastEgressLink);

                            Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), null);
                            Link egressLink = linkChooser.decideOnLink(destinationFacility, null, person);
                            trip.getTripAttributes().putAttribute("egress_link", egressLink);

                            previousCarIndex = i;
                            lastEgressLink = egressLink;
                            carAvailable = true;
                        }
                    }
                    else if (mode.equals("car")){
                        if (!carAvailable){
                            return false;
                        }
                        else {
                            trip.getTripAttributes().putAttribute("access_link", lastEgressLink);

                            Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), null);
                            Link egressLink = linkChooser.decideOnLink(destinationFacility, null, person);
                            trip.getTripAttributes().putAttribute("egress_link", egressLink);

                            previousCarIndex = i;
                            lastEgressLink = egressLink;
                        }
                    }

                }
            }
            if (firstAccessLink != lastEgressLink){
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean validateAfterEstimation(List<DiscreteModeChoiceTrip> tour, TourCandidate candidate,
            List<TourCandidate> previousCandidates) {
        for (int i = 0; i < tour.size(); i++){
            DiscreteModeChoiceTrip trip = tour.get(i);
            
            trip.getTripAttributes().removeAttribute("car_pt");
            trip.getTripAttributes().removeAttribute("access_link");
            trip.getTripAttributes().removeAttribute("egress_link");

            TripCandidate tripCandidate = candidate.getTripCandidates().get(i);
            if (tripCandidate.getMode().equals("car_pt") && tripCandidate instanceof RoutedTripCandidate){
                RoutedTripCandidate routedTripCandidate = (RoutedTripCandidate) tripCandidate;
                List<? extends PlanElement> planElement = routedTripCandidate.getRoutedPlanElements();
                for (PlanElement pe : planElement){
                    if (pe instanceof Leg){
                        Leg leg = (Leg) pe;
                        if ((!leg.getMode().equals("pt:rail")) && (!leg.getMode().equals("walk")) && (!leg.getMode().equals("car"))){
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }


    public static class Factory implements TourConstraintFactory {
        private final HomeFinder homeFinder;
        private final MultimodalLinkChooser linkChooser;
        private final QuadTree<TransitStopFacility> railStops;

        public Factory(HomeFinder homeFinder, MultimodalLinkChooser linkChooser, TransitSchedule schedule) {
            this.homeFinder = homeFinder;
            this.linkChooser = linkChooser;
            this.railStops = createRailStops(schedule);
        }

        public QuadTree<TransitStopFacility> createRailStops(TransitSchedule schedule){
            List<TransitStopFacility> railStops = new LinkedList<>();

                for (TransitLine transitLine : schedule.getTransitLines().values()) {
                    for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                        if (transitRoute.getTransportMode().equals("rail")) {
                           for (TransitRouteStop railStop : transitRoute.getStops()) {
                                railStops.add(railStop.getStopFacility());
                           }
                        }
                    }
                }

                QuadTree<TransitStopFacility> railQT = QuadTrees.createQuadTree(railStops);

                return railQT;
        }


        public TourConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                Collection<String> availableModes) {

                return new CarContinuityTourConstraint(homeFinder.getHomeLocationId(planTrips), linkChooser, railStops);
        }
    }

}
