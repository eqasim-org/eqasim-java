package org.eqasim.ile_de_france.policies;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class CarPTRouter implements RoutingModule{

    private final RoutingModule carRoutingModule;
    private final RoutingModule ptRoutingModule;
    
    public CarPTRouter(RoutingModule carRoutingModule, RoutingModule ptRoutingModule, SwissRailRaptorData data) {
        this.carRoutingModule = carRoutingModule;
        this.ptRoutingModule = ptRoutingModule;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        
        final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();
        final Attributes routingAttributes = request.getAttributes();

        final String direction = request.getAttributes().getAttribute("car_pt").toString();
        if (direction == null){
            throw new IllegalArgumentException("No direction specified");
        }
        else if (direction == "ACCESS"){
            Gbl.assertNotNull(fromFacility);
            Gbl.assertNotNull(toFacility);
            
            Link parkingLink = (Link) routingAttributes.getAttribute("parking");

            if (parkingLink == null){
                throw new IllegalArgumentException("No parking link specified");
            }

            Facility parkingFacility = new LinkWrapperFacility(parkingLink);
    
            RoutingRequest carRequest = DefaultRoutingRequest.of(fromFacility, parkingFacility, departureTime, person, routingAttributes);
            List<? extends PlanElement> carRoute = carRoutingModule.calcRoute(carRequest);
    
            Activity interactionActivity = createInteractionActivity(parkingFacility.getCoord(), parkingLink.getId(), "car-pt");

            Leg lastCarLeg = (Leg) carRoute.get(carRoute.size() - 1);
            RoutingRequest ptRequest = DefaultRoutingRequest.of(parkingFacility, toFacility, lastCarLeg.getDepartureTime().seconds() + lastCarLeg.getTravelTime().seconds(), person, routingAttributes);
            List<? extends PlanElement> ptRoute = ptRoutingModule.calcRoute(ptRequest);
    
            List<PlanElement> combinedRoute = new ArrayList<>(carRoute);
            combinedRoute.add(interactionActivity);
            combinedRoute.addAll(ptRoute);
            return combinedRoute;
        }
        else if (direction == "EGRESS"){
            Gbl.assertNotNull(fromFacility);
            Gbl.assertNotNull(toFacility);

            Link parkingLink = (Link) routingAttributes.getAttribute("parking");
            
            if (parkingLink == null){
                throw new IllegalArgumentException("No parking link specified");
            }

            Facility parkingFacility = new LinkWrapperFacility(parkingLink);

            RoutingRequest ptRequest = DefaultRoutingRequest.of(fromFacility, parkingFacility, departureTime, person, routingAttributes);
            List<? extends PlanElement> ptRoute = ptRoutingModule.calcRoute(ptRequest);

            Activity interactionActivity = createInteractionActivity(parkingLink.getCoord(), parkingFacility.getLinkId(), "car-pt");

            Leg lastPtLeg = (Leg) ptRoute.get(ptRoute.size() - 1);
            RoutingRequest carRequest = DefaultRoutingRequest.of(parkingFacility, toFacility, lastPtLeg.getDepartureTime().seconds() + lastPtLeg.getTravelTime().seconds(), person, routingAttributes);
            List<? extends PlanElement> carRoute = carRoutingModule.calcRoute(carRequest);

            List<PlanElement> combinedRoute = new ArrayList<>(ptRoute);
            combinedRoute.add(interactionActivity);
            combinedRoute.addAll(carRoute);
            return combinedRoute;
        }
        else {
            throw new IllegalArgumentException("Invalid direction specified");
        }
    }
    
    private static Activity createInteractionActivity(final Coord interactionCoord, final Id<Link> interactionLink, final String mode) {
         Activity act = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(interactionCoord, interactionLink, mode);		
         return act;
     }

}
