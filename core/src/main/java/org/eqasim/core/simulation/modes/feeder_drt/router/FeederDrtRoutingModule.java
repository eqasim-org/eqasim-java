package org.eqasim.core.simulation.modes.feeder_drt.router;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search.AccessEgressStopSearch;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_selection.AccessEgressStopsSelector;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.Facility;

import java.util.*;
import java.util.regex.Pattern;


public class FeederDrtRoutingModule implements RoutingModule {

    public enum FeederDrtTripSegmentType {MAIN, DRT}

    public static final String CURRENT_SEGMENT_TYPE_ATTR = "currentSegmentType";

    private final RoutingModule drtRoutingModule;
    private final RoutingModule transitRoutingModule;

    private final PopulationFactory populationFactory;

    private final String mode;
    private final AccessEgressStopsSelector accessEgressStopsSelector;
    private final ScenarioExtent drtServiceAreaExtent;
    private final AccessEgressStopSearch accessEgressStopSearch;
    private final Pattern skippedFacilitiesIdPattern;

    public FeederDrtRoutingModule(String mode, RoutingModule feederRoutingModule, RoutingModule transitRoutingModule,
                                  PopulationFactory populationFactory, AccessEgressStopSearch accessEgressStopSearch, AccessEgressStopsSelector accessEgressStopsSelector,
                                  ScenarioExtent drtServiceAreaExtent, String skipAccessAndEgressAtFacilities) {
        this.mode = mode;
        this.drtRoutingModule = feederRoutingModule;
        this.transitRoutingModule = transitRoutingModule;
        this.populationFactory = populationFactory;
        this.accessEgressStopSearch = accessEgressStopSearch;
        this.accessEgressStopsSelector = accessEgressStopsSelector;
        this.drtServiceAreaExtent = drtServiceAreaExtent;
        if(skipAccessAndEgressAtFacilities.length() > 0) {
            this.skippedFacilitiesIdPattern = Pattern.compile(skipAccessAndEgressAtFacilities);
        } else {
            skippedFacilitiesIdPattern = null;
        }
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
        Facility fromFacility = routingRequest.getFromFacility();
        Facility toFacility = routingRequest.getToFacility();
        double departureTime = routingRequest.getDepartureTime();
        Person person = routingRequest.getPerson();


        // Identify closest stations from the origin and destination of the trip (if they are not skipped)
        Facility accessFacility = null;
        if(!skipFacility(routingRequest.getFromFacility())) {
            accessFacility = this.accessEgressStopsSelector.getAccessFacility(routingRequest, this.accessEgressStopSearch.getAccessFacilitiesQuadTree());
        }
        Facility egressFacility = null;
        if(!skipFacility(routingRequest.getToFacility())) {
            egressFacility = this.accessEgressStopsSelector.getEgressFacility(routingRequest, this.accessEgressStopSearch.getEgressFacilitiesQuadTree());
        }

        List<PlanElement> intermodalRoute = new LinkedList<>();
        List<? extends PlanElement> accessDrtRoute = null;
        List<? extends PlanElement> egressDrtRoute = null;

        // Computing the access DRT route if it's possible
        if (accessFacility != null && (drtServiceAreaExtent == null || drtServiceAreaExtent.isInside(fromFacility.getCoord()))) {
            accessDrtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, accessFacility, departureTime, person));
        }
        double accessTime = departureTime;
        if (accessDrtRoute == null) {
            // if no DRT route, next part of the trip starts from the origin
            accessFacility = fromFacility;
        } else {
            //Otherwise we have already a first part of the trip
            intermodalRoute.addAll(accessDrtRoute);
            for (PlanElement element : intermodalRoute) {
                if (element instanceof Leg leg) {
                    accessTime = Math.max(accessTime, leg.getDepartureTime().seconds());
                    accessTime += leg.getTravelTime().seconds();
                    leg.getAttributes().putAttribute(CURRENT_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.DRT);
                }
            }
            Activity accessInteractionActivity = populationFactory.createActivityFromLinkId(this.mode + " interaction", accessFacility.getLinkId());
            accessInteractionActivity.setMaximumDuration(0);
            intermodalRoute.add(accessInteractionActivity);
        }


        // We have to check the existence of the egress facility here, the pt router will not support a null value
        if (egressFacility == null || (drtServiceAreaExtent != null && !drtServiceAreaExtent.isInside(toFacility.getCoord()))) {
            egressFacility = toFacility;
        }

        // Compute the PT part of the route towards the egress (or to) facility
        List<PlanElement> ptRoute = new LinkedList<>(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, egressFacility, accessTime, person)));
        ptRoute.stream().filter(planElement -> planElement instanceof Leg).map(planElement -> (Leg) planElement).forEach(leg -> leg.getAttributes().putAttribute(CURRENT_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.MAIN));

        // It's ok to compare reference here, we want to check if we assigned toFacility to egressFacility above
        if(egressFacility != toFacility) {
            double egressTime = accessTime;
            for (PlanElement element : ptRoute) {
                if (element instanceof Leg leg) {
                    egressTime = Math.max(egressTime, leg.getDepartureTime().seconds());
                    egressTime += leg.getTravelTime().seconds();
                }
            }
            egressDrtRoute = drtRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(egressFacility, toFacility, egressTime, person));
        }

        // egressDrtRoute is assigned only in the above if, but it can be null without entering the if block, so we need to do this here, If no valid DRT route is found, we recompute a PT route from the access facility to the trip destination
        if (egressDrtRoute == null) {
            if(egressFacility != toFacility) {
                // In this case, the egressDrtRoute is null because the attempt to compute one wasn't successful, so we need to compute a pt route from the access (or from facility) to the to facility
                ptRoute = new LinkedList<>(transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessFacility, toFacility, accessTime, person)));
                ptRoute.stream().filter(planElement -> planElement instanceof Leg).map(planElement -> (Leg) planElement).forEach(leg -> leg.getAttributes().putAttribute(CURRENT_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.MAIN));
            }
            intermodalRoute.addAll(ptRoute);
        } else {
            // Here we have a pt route and an egress drt route, we need to propriately concatenate them in the overall route
            intermodalRoute.addAll(ptRoute);
            Activity egressInteractionActivity = populationFactory.createActivityFromLinkId(this.mode + " interaction", egressFacility.getLinkId());
            egressInteractionActivity.setMaximumDuration(0);
            egressDrtRoute.stream().filter(planElement -> planElement instanceof Leg).map(planElement -> (Leg) planElement).forEach(leg -> leg.getAttributes().putAttribute(CURRENT_SEGMENT_TYPE_ATTR, FeederDrtTripSegmentType.DRT));
            intermodalRoute.add(egressInteractionActivity);
            intermodalRoute.addAll(egressDrtRoute);
        }
        return intermodalRoute;
    }

    private boolean skipFacility(Facility facility) {
        if(this.skippedFacilitiesIdPattern != null && facility instanceof ActivityFacilityImpl activityFacility) {
            return skippedFacilitiesIdPattern.matcher(activityFacility.getId().toString()).matches();
        }
        return false;
    }
}
