package org.eqasim.core.components.transit_with_abstract_access.routing;

import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class TransitWithAbstractAccessRoutingModule implements RoutingModule {

    public static final String ABSTRACT_ACCESS_LEG_MODE_NAME = "abstractAccess";

    private final IdMap<TransitStopFacility, List<AbstractAccessItem>> accessItems;
    private final QuadTree<TransitStopFacility> quadTree;
    private final RoutingModule transitRoutingModule;
    private final double maxRadius;

    private final PopulationFactory populationFactory;

    public TransitWithAbstractAccessRoutingModule(TransitSchedule transitSchedule, AbstractAccesses abstractAccesses, Network network, RoutingModule transitRoutingModule, PopulationFactory populationFactory) {
        this.accessItems = new IdMap<>(TransitStopFacility.class);
        double maxRadius = Double.MIN_VALUE;
        boolean atLeastOneAccess = false;
        for (Map.Entry<Id<TransitStopFacility>, List<AbstractAccessItem>> entry : abstractAccesses.getAbstractAccessItemsByTransitStop().entrySet()) {
            // In case there are facilities mentioned in the input map but with an empty list of access items
            if (entry.getValue().size() > 0) {
                atLeastOneAccess = true;
                this.accessItems.put(entry.getKey(), entry.getValue());
                for (AbstractAccessItem abstractAccessItem : entry.getValue()) {
                    if (abstractAccessItem.getRadius() > maxRadius) {
                        maxRadius = abstractAccessItem.getRadius();
                    }
                }
            }
        }
        if (!atLeastOneAccess) {
            throw new IllegalStateException("No abstract access defined for any of the transit stops");
        }
        this.maxRadius = maxRadius;

        this.transitRoutingModule = transitRoutingModule;
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        this.quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);

        this.populationFactory = populationFactory;

        Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();

        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
                    if (!processedFacilities.contains(transitStopFacility.getId())) {
                        processedFacilities.add(transitStopFacility.getId());
                        this.quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), transitStopFacility);
                    }
                }
            }
        }

        SpeedyALTFactory factory = new SpeedyALTFactory();
        factory.createPathCalculator(network, new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()), new FreeSpeedTravelTime());
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
        // Let's first find the transit stops that provide a feasible access from the origin and to the destination of the trip
        Coord fromFacilityCoord = routingRequest.getFromFacility().getCoord();
        IdMap<TransitStopFacility, AbstractAccessItem> bestAccessItemForOrigin = new IdMap<>(TransitStopFacility.class);
        TransitStopFacility accessTransitStopFacility = this.getClosestTransitScheduleWithValidAccessItem(fromFacilityCoord, bestAccessItemForOrigin);

        Coord toFacilityCoord = routingRequest.getToFacility().getCoord();
        IdMap<TransitStopFacility, AbstractAccessItem> bestAccessItemForDestination = new IdMap<>(TransitStopFacility.class);
        TransitStopFacility egresssTransitStopFacility = this.getClosestTransitScheduleWithValidAccessItem(routingRequest.getToFacility().getCoord(), bestAccessItemForDestination);

        if(accessTransitStopFacility == null && egresssTransitStopFacility == null) {
            return this.transitRoutingModule.calcRoute(routingRequest);
        }
        List<PlanElement> plan = new ArrayList<>();

        double departureTime = routingRequest.getDepartureTime();

        if(accessTransitStopFacility != null) {
            AbstractAccessItem abstractAccessItem = bestAccessItemForOrigin.get(accessTransitStopFacility.getId());
            Leg leg = PopulationUtils.createLeg(ABSTRACT_ACCESS_LEG_MODE_NAME);
            leg.setRoute(new DefaultAbstractAccessRoute(routingRequest.getFromFacility().getLinkId(), abstractAccessItem.getCenterStop().getLinkId(), abstractAccessItem));
            leg.setDepartureTime(departureTime);
            leg.getAttributes().putAttribute("accessId", abstractAccessItem.getId().toString());
            leg.setTravelTime(abstractAccessItem.getTimeToCenter(fromFacilityCoord));
            leg.getRoute().setTravelTime(leg.getTravelTime().seconds());
            plan.add(leg);
            departureTime+=leg.getTravelTime().seconds();
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", accessTransitStopFacility.getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
        } else {
            List<? extends PlanElement> ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(routingRequest.getFromFacility(), egresssTransitStopFacility, routingRequest.getDepartureTime(), routingRequest.getPerson()));
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg) {
                    Leg leg = (Leg) element;
                    departureTime = leg.getDepartureTime().seconds();
                    departureTime+=leg.getTravelTime().seconds();
                }
            }
        }

        if(egresssTransitStopFacility != null) {
            List<? extends PlanElement> ptRoute;
            if(accessTransitStopFacility != null) {
                ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessTransitStopFacility, egresssTransitStopFacility, departureTime, routingRequest.getPerson()));
            } else {
                ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(routingRequest.getFromFacility(), egresssTransitStopFacility, departureTime, routingRequest.getPerson()));
            }
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg) {
                    Leg leg = (Leg) element;
                    departureTime = leg.getDepartureTime().seconds();
                    departureTime+=leg.getTravelTime().seconds();
                }
            }
            plan.addAll(ptRoute);
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", egresssTransitStopFacility.getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
            AbstractAccessItem abstractAccessItem = bestAccessItemForDestination.get(egresssTransitStopFacility.getId());
            Leg leg = PopulationUtils.createLeg(ABSTRACT_ACCESS_LEG_MODE_NAME);
            leg.setDepartureTime(departureTime);
            leg.setRoute(new DefaultAbstractAccessRoute(abstractAccessItem.getCenterStop().getLinkId(), routingRequest.getToFacility().getLinkId(), abstractAccessItem));
            leg.getAttributes().putAttribute("accessId", abstractAccessItem.getId().toString());
            leg.setTravelTime(abstractAccessItem.getTimeToCenter(toFacilityCoord));
            leg.getRoute().setTravelTime(leg.getTravelTime().seconds());
            plan.add(leg);
        }
        return plan;
    }

    private TransitStopFacility getClosestTransitScheduleWithValidAccessItem(Coord coord, IdMap<TransitStopFacility, AbstractAccessItem> bestAccessesMap) {
        return this.quadTree.getDisk(coord.getX(), coord.getY(), this.maxRadius).
                stream().
                filter(transitStopFacility -> {
                    if (!accessItems.containsKey(transitStopFacility.getId())) {
                        return false;
                    }
                    AbstractAccessItem bestAccessItem = AbstractAccessItem.getFastestAccessItemForCoord(coord, accessItems.get(transitStopFacility.getId()));
                    if (bestAccessItem != null) {
                        bestAccessesMap.put(transitStopFacility.getId(), bestAccessItem);
                        return true;
                    }
                    return false;
                }).min((Comparator<Facility>) (o1, o2) ->
                        (int) (NetworkUtils.getEuclideanDistance(coord, o1.getCoord()) - NetworkUtils.getEuclideanDistance(coord, o2.getCoord()))).orElse(null);
    }
}
