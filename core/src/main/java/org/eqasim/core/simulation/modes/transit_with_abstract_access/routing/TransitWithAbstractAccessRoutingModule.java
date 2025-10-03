package org.eqasim.core.simulation.modes.transit_with_abstract_access.routing;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.TransportMode;
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
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
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
    private final RaptorParametersForPerson raptorParametersForPerson;

    public TransitWithAbstractAccessRoutingModule(TransitWithAbstractAccessData transitWithAbstractAccessData, AbstractAccesses abstractAccesses, RoutingModule transitRoutingModule, PopulationFactory populationFactory, RaptorParametersForPerson raptorParametersForPerson, RaptorStaticConfig raptorStaticConfig) {

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

        this.raptorParametersForPerson = raptorParametersForPerson;
    }


    private double calcPtRoutingCost(List<? extends PlanElement> planElements, Facility fromFacility, Facility toFacility, Person person) {
        Double routingCost = null;
        if(planElements.size() == 1) {
            RaptorParameters raptorParameters = this.raptorParametersForPerson.getRaptorParameters(person);
            Leg leg = (Leg) planElements.get(0);
            if (!leg.getMode().equals("walk")) {
                throw new IllegalStateException();
            }
            double beelineDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
            double walkTime = beelineDistance / raptorParameters.getBeelineWalkSpeed();
            double walkCost_per_s = - raptorParameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.walk);
            routingCost = walkTime * walkCost_per_s;
        } else {
            for(PlanElement planElement : planElements) {
                if(planElement instanceof Leg leg && leg.getMode().equals("pt")) {
                    DefaultTransitPassengerRoute defaultTransitPassengerRoute = (DefaultTransitPassengerRoute) leg.getRoute();
                    routingCost = defaultTransitPassengerRoute.totalPtRoutingCost;
                    break;
                }
            }
        }

        if(routingCost == null) {
            throw new IllegalStateException();
        }
        return routingCost;
    }

    private double calcAbstractAccessLegRoutingCost(DefaultAbstractAccessRoute accessRoute, Person person) {
        RaptorParameters raptorParameters = this.raptorParametersForPerson.getRaptorParameters(person);
        double routingCost = -accessRoute.getTravelTime().seconds() * raptorParameters.getMarginalUtilityOfTravelTime_utl_s("bus");
        routingCost -= accessRoute.getWaitTime() * raptorParameters.getMarginalUtilityOfWaitingPt_utl_s();
        return routingCost;
    }

    private TransitWithAbstractAccessRouteAlternative calcRoutingAlternative(RoutingRequest routingRequest, AbstractAccessItem accessItem, AbstractAccessItem egressItem) {
        double departureTime = routingRequest.getDepartureTime();

        List<PlanElement> plan = new ArrayList<>();

        if(accessItem == null && egressItem == null) {
            plan.addAll(this.transitRoutingModule.calcRoute(routingRequest));
            return new TransitWithAbstractAccessRouteAlternative(plan, calcPtRoutingCost(plan, routingRequest.getFromFacility(), routingRequest.getToFacility(), routingRequest.getPerson()), accessItem, egressItem);
        }

        double routingCost = 0;

        double transferCost = this.raptorParametersForPerson.getRaptorParameters(routingRequest.getPerson()).getTransferPenaltyFixCostPerTransfer();

        boolean foundPtLeg = false;

        if(accessItem != null) {
            Leg leg = this.createAbstractAccessLeg(accessItem, true, routingRequest.getFromFacility().getLinkId(), departureTime, routingRequest.getPerson());
            plan.add(leg);
            departureTime+=leg.getTravelTime().seconds();
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", accessItem.getCenterStop().getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
            routingCost += calcAbstractAccessLegRoutingCost((DefaultAbstractAccessRoute) leg.getRoute(), routingRequest.getPerson());
            routingCost += transferCost;
        } else {
            // We are sure that egressTransitStopFacility is not null because of the if block above
            List<? extends PlanElement> ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(routingRequest.getFromFacility(), egressItem.getCenterStop(), routingRequest.getDepartureTime(), routingRequest.getPerson()));
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg leg) {
                    if(leg.getMode().equals("pt")) {
                        foundPtLeg = true;
                    }
                    departureTime = leg.getDepartureTime().seconds();
                    departureTime+=leg.getTravelTime().seconds();
                }
                plan.add(element);
            }
            routingCost += calcPtRoutingCost(plan, routingRequest.getFromFacility(), egressItem.getCenterStop(), routingRequest.getPerson());
            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", egressItem.getCenterStop().getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
        }

        if(egressItem != null) {
            List<? extends PlanElement> ptRoute;
            if(accessItem != null) {
                ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessItem.getCenterStop(), egressItem.getCenterStop(), departureTime, routingRequest.getPerson()));
                routingCost += calcPtRoutingCost(ptRoute, accessItem.getCenterStop(), egressItem.getCenterStop(), routingRequest.getPerson());
                for(PlanElement element: ptRoute) {
                    if(element instanceof Leg leg) {
                        if(leg.getMode().equals("pt")) {
                            foundPtLeg = true;
                        }
                        departureTime = leg.getDepartureTime().seconds();
                        departureTime += leg.getTravelTime().seconds();
                    }
                    plan.add(element);
                }
            } // Otherwise the pt route from the origin to the egress center has already been computed in the else block above

            Activity activity = this.populationFactory.createActivityFromLinkId("pt interaction", egressItem.getCenterStop().getLinkId());
            activity.setStartTime(departureTime);
            activity.setEndTime(departureTime);
            plan.add(activity);
            Leg leg = this.createAbstractAccessLeg(egressItem, false, routingRequest.getToFacility().getLinkId(), departureTime, routingRequest.getPerson());
            plan.add(leg);
            routingCost += calcAbstractAccessLegRoutingCost((DefaultAbstractAccessRoute) leg.getRoute(), routingRequest.getPerson());
            routingCost += transferCost;
        } else {
            // We are sure that the accessStopFacility was not null, so we route from there to the destination
            List<? extends PlanElement> ptRoute = this.transitRoutingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(accessItem.getCenterStop(), routingRequest.getToFacility(), departureTime, routingRequest.getPerson()));
            for(PlanElement element: ptRoute) {
                if(element instanceof Leg leg && leg.getMode().equals("pt")) {
                    foundPtLeg = true;
                    break;
                }
            }
            routingCost += calcPtRoutingCost(ptRoute, accessItem.getCenterStop(), routingRequest.getToFacility(), routingRequest.getPerson());
            plan.addAll(ptRoute);
        }
        if(!foundPtLeg) {
            routingCost -= transferCost;
        }
        return new TransitWithAbstractAccessRouteAlternative(plan, routingCost, accessItem, egressItem);
    }

    private static class TransitWithAbstractAccessRouteAlternative {
        private final List<PlanElement> elements;
        private final double totalRoutingCost;
        private final AbstractAccessItem accessItem;
        private final AbstractAccessItem egressItem;

        private TransitWithAbstractAccessRouteAlternative(List<PlanElement> elements, double totalRoutingCost, AbstractAccessItem accessItem, AbstractAccessItem egressItem) {
            this.elements = elements;
            this.totalRoutingCost = totalRoutingCost;
            this.accessItem = accessItem;
            this.egressItem = egressItem;
        }
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

        AbstractAccessItem accessItem = accessTransitStopFacility == null ? null : bestAccessItemForOrigin.get(accessTransitStopFacility.getId());
        AbstractAccessItem egressItem = egresssTransitStopFacility == null ? null : bestAccessItemForDestination.get(egresssTransitStopFacility.getId());

        PriorityQueue<TransitWithAbstractAccessRouteAlternative> alternatives = new PriorityQueue<>(Comparator.comparingDouble(alternative -> alternative.totalRoutingCost));

        alternatives.add(calcRoutingAlternative(routingRequest, null, null));
        if(accessItem != null) {
            alternatives.add(calcRoutingAlternative(routingRequest, accessItem, null));
            if(egressItem != null) {
                alternatives.add(calcRoutingAlternative(routingRequest, accessItem, egressItem));
            }
        }
        if(egressItem != null) {
            alternatives.add(calcRoutingAlternative(routingRequest, null, egressItem));
        }

        TransitWithAbstractAccessRouteAlternative bestAlternative = alternatives.poll();

        // We put the total routing cost of the mixed route in the first abstract access route of the chain
        for(PlanElement planElement: bestAlternative.elements) {
            if(planElement instanceof Leg leg && leg.getRoute() instanceof DefaultAbstractAccessRoute defaultAbstractAccessRoute) {
                defaultAbstractAccessRoute.setTotalRoutingCost(bestAlternative.totalRoutingCost);
                break;
            }
        }

        return bestAlternative.elements;
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
