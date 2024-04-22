package org.eqasim.core.simulation.modes.feeder_drt.router;

import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector.AccessEgressStopsSelector;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

import java.util.*;


public class FeederDrtRoutingModule implements RoutingModule {

    public enum FeederDrtTripSegmentType {MAIN, DRT};

    public static final String STAGE_ACTIVITY_PREVIOUS_SEGMENT_TYPE_ATTR = "previousSegmentType";

    private final RoutingModule drtRoutingModule;
    private final RoutingModule transitRoutingModule;

    private final PopulationFactory populationFactory;

    private final String mode;
    private final AccessEgressStopsSelector accessEgressStopsSelector;

    public FeederDrtRoutingModule(String mode,RoutingModule feederRoutingModule, RoutingModule transitRoutingModule,
                                  PopulationFactory populationFactory, AccessEgressStopsSelector accessEgressStopsSelector) {
        this.mode = mode;
        this.drtRoutingModule = feederRoutingModule;
        this.transitRoutingModule = transitRoutingModule;
        this.populationFactory = populationFactory;
        this.accessEgressStopsSelector = accessEgressStopsSelector;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
        Facility fromFacility = routingRequest.getFromFacility();
        Facility toFacility = routingRequest.getToFacility();
        double departureTime = routingRequest.getDepartureTime();
        Person person = routingRequest.getPerson();


        // Identify closest stations from the origin and destination of the trip
        Facility accessFacility = this.accessEgressStopsSelector.getAccessFacility(routingRequest);
        Facility egressFacility = this.accessEgressStopsSelector.getEgressFacility(routingRequest);

        List<PlanElement> intermodalRoute = new LinkedList<>();
        // Computing the access DRT route
        List<? extends PlanElement> drtRoute = null;

        if (accessFacility != null) {
            drtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, accessFacility, departureTime, person));
        }
        double accessTime = departureTime;
        if (drtRoute == null) {
            // if no DRT route, next part of the trip starts from the origin
            accessFacility = fromFacility;
        } else {
            //Otherwise we have already a first part of the trip
            intermodalRoute.addAll(drtRoute);
            for (PlanElement element : intermodalRoute) {
                if (element instanceof Leg leg) {
                    accessTime = Math.max(accessTime, leg.getDepartureTime().seconds());
                    accessTime += leg.getTravelTime().seconds();
                }
            }
            Activity accessInteractionActivity = populationFactory.createActivityFromLinkId(this.mode + " interaction", accessFacility.getLinkId());
            accessInteractionActivity.setMaximumDuration(0);
            accessInteractionActivity.getAttributes().putAttribute(STAGE_ACTIVITY_PREVIOUS_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.DRT);
            intermodalRoute.add(accessInteractionActivity);
        }

        // Compute the PT part of the route
        List<PlanElement> ptRoute = new LinkedList<>(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, egressFacility, accessTime, person)));
        double egressTime = accessTime;

        for (PlanElement element : ptRoute) {
            if (element instanceof Leg leg) {
                egressTime = Math.max(egressTime, leg.getDepartureTime().seconds());
                egressTime += leg.getTravelTime().seconds();
            }
        }

        if (egressFacility != null) {
            drtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(egressFacility, toFacility, egressTime, person));
        } else {
            drtRoute = null;
        }

        // If no valid DRT route is found, we recompute a PT route from the access facility to the trip destination
        if (drtRoute == null) {
            intermodalRoute.addAll(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, toFacility, accessTime, person)));
        } else {
            // Otherwise we add it as an egress to the whole route
            intermodalRoute.addAll(ptRoute);
            Activity egressInteractionActivity = populationFactory.createActivityFromLinkId(this.mode + " interaction", egressFacility.getLinkId());
            egressInteractionActivity.setMaximumDuration(0);
            egressInteractionActivity.getAttributes().putAttribute(STAGE_ACTIVITY_PREVIOUS_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.MAIN);
            intermodalRoute.add(egressInteractionActivity);
            intermodalRoute.addAll(drtRoute);
        }
        return intermodalRoute;
    }
}
