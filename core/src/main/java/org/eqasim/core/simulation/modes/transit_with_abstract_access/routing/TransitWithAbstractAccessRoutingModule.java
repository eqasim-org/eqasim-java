package org.eqasim.core.simulation.modes.transit_with_abstract_access.routing;

import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

public class TransitWithAbstractAccessRoutingModule implements RoutingModule {

    public static final String ABSTRACT_ACCESS_LEG_MODE_NAME = "abstractAccess";
    private final IdMap<TransitStopFacility, List<AbstractAccessItem>> accessItems;
    private final IdMap<AbstractAccessItem, LeastCostPathCalculator> pathCalculators;
    private final RoutingModule transitRoutingModule;
    private final double maxRadius;
    private final PopulationFactory populationFactory;
    private final IdMap<TransitStopFacility, Id<Link>> transitStopFacilityLinks;
    private final TransitWithAbstractAccessData transitWithAbstractAccessData;

    public TransitWithAbstractAccessRoutingModule(TransitWithAbstractAccessData transitWithAbstractAccessData, AbstractAccesses abstractAccesses, RoutingModule transitRoutingModule, PopulationFactory populationFactory) {

        this.transitWithAbstractAccessData = transitWithAbstractAccessData;

        this.accessItems = new IdMap<>(TransitStopFacility.class);
        this.pathCalculators = new IdMap<>(AbstractAccessItem.class);
        this.transitStopFacilityLinks = new IdMap<>(TransitStopFacility.class);
        double maxRadius = Double.MIN_VALUE;
        boolean atLeastOneAccess = false;

        SpeedyALTFactory factory = new SpeedyALTFactory();

        for (Map.Entry<Id<TransitStopFacility>, List<AbstractAccessItem>> entry : abstractAccesses.getAbstractAccessItemsByTransitStop().entrySet()) {
            // In case there are facilities mentioned in the input map but with an empty list of access items
            if (!entry.getValue().isEmpty()) {
                atLeastOneAccess = true;
                this.accessItems.put(entry.getKey(), entry.getValue());
                for (AbstractAccessItem abstractAccessItem : entry.getValue()) {
                    if (abstractAccessItem.getRadius() > maxRadius) {
                        maxRadius = abstractAccessItem.getRadius();
                    }
                    if(!this.transitStopFacilityLinks.containsKey(entry.getKey())) {
                        Link link = NetworkUtils.getNearestLink(this.transitWithAbstractAccessData.getNetwork(), abstractAccessItem.getCenterStop().getCoord());
                        this.transitStopFacilityLinks.put(entry.getKey(), link.getId());
                    }
                    if(abstractAccessItem.isUsingRoutedDistance()) {
                        TravelTime travelTime = (link, time, person, vehicle) -> Math.min(link.getLength() / abstractAccessItem.getAvgSpeedToCenterStop(), link.getLength() / link.getFreespeed());
                        LeastCostPathCalculator pathCalculator = factory.createPathCalculator(this.transitWithAbstractAccessData.getNetwork(), new OnlyTimeDependentTravelDisutility(travelTime), travelTime);
                        this.pathCalculators.put(abstractAccessItem.getId(), pathCalculator);
                    }
                }
            }
        }
        if (!atLeastOneAccess) {
            throw new IllegalStateException("No abstract access defined for any of the transit stops");
        }
        this.maxRadius = maxRadius;

        this.transitRoutingModule = transitRoutingModule;

        this.populationFactory = populationFactory;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest routingRequest) {
        // Let's first find the transit stops that provide a feasible access from the origin and to the destination of the trip
        Coord fromFacilityCoord = this.transitWithAbstractAccessData.getNetwork().getLinks().get(routingRequest.getFromFacility().getLinkId()).getCoord();
        IdMap<TransitStopFacility, AbstractAccessItem> bestAccessItemForOrigin = new IdMap<>(TransitStopFacility.class);
        TransitStopFacility accessTransitStopFacility = this.getClosestTransitStopWithValidAccessItem(fromFacilityCoord, bestAccessItemForOrigin);

        Coord toFacilityCoord = this.transitWithAbstractAccessData.getNetwork().getLinks().get(routingRequest.getToFacility().getLinkId()).getCoord();
        IdMap<TransitStopFacility, AbstractAccessItem> bestAccessItemForDestination = new IdMap<>(TransitStopFacility.class);
        TransitStopFacility egresssTransitStopFacility = this.getClosestTransitStopWithValidAccessItem(toFacilityCoord, bestAccessItemForDestination);

        if(accessTransitStopFacility == null && egresssTransitStopFacility == null) {
            return this.transitRoutingModule.calcRoute(routingRequest);
        }
        List<PlanElement> plan = new ArrayList<>();

        double departureTime = routingRequest.getDepartureTime();

        if(accessTransitStopFacility != null) {
            AbstractAccessItem abstractAccessItem = bestAccessItemForOrigin.get(accessTransitStopFacility.getId());
            Leg leg = this.createAbstractAccessLeg(abstractAccessItem, true, routingRequest.getFromFacility().getLinkId(), departureTime, routingRequest.getPerson());
            plan.add(leg);
            departureTime+=leg.getTravelTime().seconds();
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", accessTransitStopFacility.getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
        } else {
            // We are sure that egressTransitStopFacility is not null because of the if block above
            List<? extends PlanElement> ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(routingRequest.getFromFacility(), egresssTransitStopFacility, routingRequest.getDepartureTime(), routingRequest.getPerson()));
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg leg) {
                    departureTime = leg.getDepartureTime().seconds();
                    departureTime+=leg.getTravelTime().seconds();
                }
                plan.add(element);
            }
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", egresssTransitStopFacility.getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
        }

        if(egresssTransitStopFacility != null) {
            List<? extends PlanElement> ptRoute;
            if(accessTransitStopFacility != null) {
                ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessTransitStopFacility, egresssTransitStopFacility, departureTime, routingRequest.getPerson()));
            } else {
                ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(routingRequest.getFromFacility(), egresssTransitStopFacility, departureTime, routingRequest.getPerson()));
            }
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg leg) {
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
            Leg leg = this.createAbstractAccessLeg(abstractAccessItem, false, routingRequest.getToFacility().getLinkId(), departureTime, routingRequest.getPerson());
            plan.add(leg);
        } else {
            // We are sure that the accessStopFacility was not null, so we route from there to the destination
            List<? extends PlanElement> ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessTransitStopFacility, routingRequest.getToFacility(), departureTime, routingRequest.getPerson()));
            plan.addAll(ptRoute);
        }
        return plan;
    }

    private Leg createAbstractAccessLeg(AbstractAccessItem accessItem, boolean access, Id<Link> otherLinkId, double departureTime, Person person) {
        Leg leg = PopulationUtils.createLeg(ABSTRACT_ACCESS_LEG_MODE_NAME);
        leg.setDepartureTime(departureTime);
        Id<Link> accessTransitStopFacilityLink = this.transitStopFacilityLinks.get(accessItem.getCenterStop().getId());
        DefaultAbstractAccessRoute abstractAccessRoute = new DefaultAbstractAccessRoute(access ? otherLinkId : accessItem.getCenterStop().getLinkId(), access ? accessItem.getCenterStop().getLinkId() : otherLinkId, accessItem);
        leg.setRoute(abstractAccessRoute);

        Id<Link> fromLinkId = access ? otherLinkId : accessTransitStopFacilityLink;
        Id<Link> toLinkId = access ? accessTransitStopFacilityLink : otherLinkId;
        leg.getAttributes().putAttribute("accessId", accessItem.getId().toString());

        if(accessItem.isUsingRoutedDistance()) {
            Node fromNode = this.transitWithAbstractAccessData.getNetwork().getLinks().get(fromLinkId).getFromNode();
            Node toNode = this.transitWithAbstractAccessData.getNetwork().getLinks().get(toLinkId).getToNode();
            LeastCostPathCalculator.Path path = this.pathCalculators.get(accessItem.getId()).calcLeastCostPath(fromNode, toNode, departureTime, person, null);
            double travelTime = path.travelTime;
            abstractAccessRoute.setDistance(path.travelCost);
            abstractAccessRoute.setTravelTime(travelTime);
        } else {
            double distance = accessItem.getDistanceToCenter(this.transitWithAbstractAccessData.getNetwork().getLinks().get(otherLinkId).getCoord());
            abstractAccessRoute.setDistance(distance);
            abstractAccessRoute.setTravelTime(accessItem.getTimeToCenter(distance));
        }
        abstractAccessRoute.setWaitTime(accessItem.getWaitTime());
        // we add the wait time to the travel time of the leg since the current departure handler does not explicitly simulate waiting
        leg.setTravelTime(abstractAccessRoute.getWaitTime() + abstractAccessRoute.getTravelTime().seconds());
        return leg;
    }

    private TransitStopFacility getClosestTransitStopWithValidAccessItem(Coord coord, IdMap<TransitStopFacility, AbstractAccessItem> bestAccessesMap) {
        return this.transitWithAbstractAccessData.getQuadTree().getDisk(coord.getX(), coord.getY(), this.maxRadius).
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
