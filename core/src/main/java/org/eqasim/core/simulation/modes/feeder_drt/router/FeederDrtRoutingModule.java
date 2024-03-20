package org.eqasim.core.simulation.modes.feeder_drt.router;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class FeederDrtRoutingModule implements RoutingModule {

    public enum FeederDrtTripSegmentType {MAIN, DRT};

    public static final String STAGE_ACTIVITY_PREVIOUS_SEGMENT_TYPE_ATTR = "previousSegmentType";

    private final RoutingModule drtRoutingModule;
    private final RoutingModule transitRoutingModule;

    private final PopulationFactory populationFactory;

    private static final Logger logger = LogManager.getLogger(FeederDrtRoutingModule.class);
    private final QuadTree<Facility> quadTree;
    private final String mode;

    public FeederDrtRoutingModule(String mode, RoutingModule feederRoutingModule, RoutingModule transitRoutingModule,
                                  PopulationFactory populationFactory, TransitSchedule schedule, Network drtNetwork) {
        logger.info("Starting initialization");
        this.mode = mode;
        this.drtRoutingModule = feederRoutingModule;
        this.transitRoutingModule = transitRoutingModule;
        this.populationFactory = populationFactory;
        double[] bounds = NetworkUtils.getBoundingBox(drtNetwork.getNodes().values());
        quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();

        for (TransitLine transitLine : schedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                if (transitRoute.getTransportMode().equals("rail") || transitRoute.getTransportMode().equals("subway")) {
                    for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                        TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
                        if (!processedFacilities.contains(transitStopFacility.getId())) {
                            processedFacilities.add(transitStopFacility.getId());
                            Facility interactionFacility = FacilitiesUtils.wrapLink(NetworkUtils.getNearestLink(drtNetwork, transitStopFacility.getCoord()));
                            try {
                                if (!quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), interactionFacility)) {
                                    logger.warn("Cannot add this stop : " + transitStopFacility.getName());
                                }
                            } catch (IllegalArgumentException exception) {
                                logger.warn("Cannot add this stop because it's out of DRT's network : " + transitStopFacility.getName());
                            }
                        }
                    }
                }
            }
        }
        logger.info("Initialization finished");
    }

    public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
                                                 Person person) {
        // Identify closest stations from the origin and destination of the trip
        Facility accessFacility = this.quadTree.getClosest(fromFacility.getCoord().getX(), fromFacility.getCoord().getY());
        Facility egressFacility = this.quadTree.getClosest(toFacility.getCoord().getX(), toFacility.getCoord().getY());

        List<PlanElement> intermodalRoute = new LinkedList<>();
        // Computing the access DRT route
        List<? extends PlanElement> drtRoute = null;
        // If the trip starts right after an outside activity, we leave its first part as PT
        if (!(fromFacility instanceof ActivityFacilityImpl) || !((ActivityFacilityImpl) fromFacility).getId().toString().startsWith("outside")) {
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

        // Compute the egress DRT route
        // Same as above, if the trip ends right before an outside activity, we leave its last part as PT
        if (!(egressFacility instanceof ActivityFacilityImpl) || !((ActivityFacilityImpl) egressFacility).getId().toString().startsWith("outside")) {
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

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
        return this.calcRoute(routingRequest.getFromFacility(), routingRequest.getToFacility(), routingRequest.getDepartureTime(), routingRequest.getPerson());
    }
}
