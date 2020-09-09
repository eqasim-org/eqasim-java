/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides public transport route search capabilities using an implementation of the
 * RAPTOR algorithm underneath.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptor implements TransitRouter {

    private static final Logger log = Logger.getLogger(SwissRailRaptor.class);

    private final SwissRailRaptorData data;
    private final SwissRailRaptorCore raptor;
    private final RaptorParametersForPerson parametersForPerson;
    private final RaptorRouteSelector defaultRouteSelector;
    private final RaptorStopFinder stopFinder;
    private final String subpopulationAttribute;
    private final ObjectAttributes personAttributes;

    private boolean treeWarningShown = false;

    public SwissRailRaptor(final SwissRailRaptorData data, RaptorParametersForPerson parametersForPerson,
                           RaptorRouteSelector routeSelector, RaptorStopFinder stopFinder) {
        this(data, parametersForPerson, routeSelector, stopFinder, null, null);
        log.info("SwissRailRaptor was initialized without support for subpopulations or intermodal access/egress legs.");
    }

    public SwissRailRaptor(final SwissRailRaptorData data, RaptorParametersForPerson parametersForPerson,
                           RaptorRouteSelector routeSelector,
                           RaptorStopFinder stopFinder,
                           String subpopulationAttribute, ObjectAttributes personAttributes) {
        this.data = data;
        this.raptor = new SwissRailRaptorCore(data);
        this.parametersForPerson = parametersForPerson;
        this.defaultRouteSelector = routeSelector;
        this.stopFinder = stopFinder;
        this.subpopulationAttribute = subpopulationAttribute;
        this.personAttributes = personAttributes;
    }

    @Override
    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        if (parameters.getConfig().isUseRangeQuery()) {
            return this.performRangeQuery(fromFacility, toFacility, departureTime, person, parameters);
        }
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, departureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, departureTime, parameters);

        RaptorRoute foundRoute = this.raptor.calcLeastCostRoute(departureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, departureTime, person, parameters);

        if (foundRoute == null || DIRECT_WALK_FACTOR * directWalk.getTotalCosts() < foundRoute.getTotalCosts()) {
            foundRoute = directWalk;
        }
        List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute);
        return legs;
    }

    static public double DIRECT_WALK_FACTOR = 1e6;
    
    private List<Leg> performRangeQuery(Facility fromFacility, Facility toFacility, double desiredDepartureTime, Person person, RaptorParameters parameters) {
        SwissRailRaptorConfigGroup srrConfig = parameters.getConfig();

        Object attr = this.personAttributes.getAttribute(person.getId().toString(), this.subpopulationAttribute);
        String subpopulation = attr == null ? null : attr.toString();
        SwissRailRaptorConfigGroup.RangeQuerySettingsParameterSet rangeSettings = srrConfig.getRangeQuerySettings(subpopulation);

        double earliestDepartureTime = desiredDepartureTime - rangeSettings.getMaxEarlierDeparture();
        double latestDepartureTime = desiredDepartureTime + rangeSettings.getMaxLaterDeparture();

        if (this.defaultRouteSelector instanceof ConfigurableRaptorRouteSelector) {
            ConfigurableRaptorRouteSelector selector = (ConfigurableRaptorRouteSelector) this.defaultRouteSelector;

            SwissRailRaptorConfigGroup.RouteSelectorParameterSet params = srrConfig.getRouteSelector(subpopulation);

            selector.setBetaTransfer(params.getBetaTransfers());
            selector.setBetaTravelTime(params.getBetaTravelTime());
            selector.setBetaDepartureTime(params.getBetaDepartureTime());
        }

        return this.calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        return calcRoute(fromFacility, toFacility, earliestDepartureTime, desiredDepartureTime, latestDepartureTime, person, this.defaultRouteSelector);
    }

    public List<Leg> calcRoute(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person, RaptorRouteSelector selector) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute foundRoute = selector.selectOne(foundRoutes, desiredDepartureTime);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoute == null || directWalk.getTotalCosts() < foundRoute.getTotalCosts()) {
            foundRoute = directWalk;
        }
        List<Leg> legs = RaptorUtils.convertRouteToLegs(foundRoute);
        // TODO adapt the activity end time of the activity right before this trip
        /* Sadly, it's not that easy to find the previous activity, as we only have from- and to-facility
         * and the departure time. One would have to search through the person's selectedPlan to find
         * a matching activity, but what if an agent travels twice a day between from- and to-activity
         * and it only sets the activity duration, but not the end-time?
         * One could try to come up with some heuristic, but that would be very error-prone and
         * not satisfying. The clean solution would be to implement our own PlanRouter which
         * uses our own TripRouter which would take care of adapting the departure time,
         * but sadly PlanRouter is hardcoded in several places (e.g. PrepareForSimImpl), so it
         * cannot easily be replaced. So I fear I currently don't see a simple solution for that.
         * mrieser / march 2018.
         */
        return legs;
    }

    public List<RaptorRoute> calcRoutes(Facility fromFacility, Facility toFacility, double earliestDepartureTime, double desiredDepartureTime, double latestDepartureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, desiredDepartureTime, parameters);
        List<InitialStop> egressStops = findEgressStops(toFacility, person, desiredDepartureTime, parameters);

        List<RaptorRoute> foundRoutes = this.raptor.calcRoutes(earliestDepartureTime, desiredDepartureTime, latestDepartureTime, fromFacility, toFacility, accessStops, egressStops, parameters);
        RaptorRoute directWalk = createDirectWalk(fromFacility, toFacility, desiredDepartureTime, person, parameters);

        if (foundRoutes == null) {
            foundRoutes = new ArrayList<>(1);
        }
        if (foundRoutes.isEmpty() || directWalk.getTotalCosts() < foundRoutes.get(0).getTotalCosts()) {
            foundRoutes.add(directWalk); // add direct walk if it seems plausible
        }
        return foundRoutes;
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(TransitStopFacility fromStop, double departureTime, RaptorParameters parameters) {
        return this.calcTree(Collections.singletonList(fromStop), departureTime, parameters);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Collection<TransitStopFacility> fromStops, double departureTime, RaptorParameters parameters) {
        if (this.data.config.getOptimization() != RaptorStaticConfig.RaptorOptimization.OneToAllRouting && !this.treeWarningShown) {
            log.warn("SwissRailRaptorData was not initialized with full support for tree calculations and may result in unexpected results. Use `RaptorStaticConfig.setOptimization(RaptorOptimization.OneToAllRouting)` to fix this issue.");
            this.treeWarningShown = true;
        }
        List<InitialStop> accessStops = new ArrayList<>();
        for (TransitStopFacility stop : fromStops) {
            accessStops.add(new InitialStop(stop, 0, 0, 0, null));
        }
        return this.calcLeastCostTree(accessStops, departureTime, parameters);
    }

    public Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcTree(Facility fromFacility, double departureTime, Person person) {
        RaptorParameters parameters = this.parametersForPerson.getRaptorParameters(person);
        List<InitialStop> accessStops = findAccessStops(fromFacility, person, departureTime, parameters);
        return this.calcLeastCostTree(accessStops, departureTime, parameters);
    }

    private Map<Id<TransitStopFacility>, SwissRailRaptorCore.TravelInfo> calcLeastCostTree(Collection<InitialStop> accessStops, double departureTime, RaptorParameters parameters) {
        return this.raptor.calcLeastCostTree(departureTime, accessStops, parameters);
    }

    public SwissRailRaptorData getUnderlyingData() {
        return this.data;
    }

    private List<InitialStop> findAccessStops(Facility facility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.ACCESS);
    }

    private List<InitialStop> findEgressStops(Facility facility, Person person, double departureTime, RaptorParameters parameters) {
        return this.stopFinder.findStops(facility, person, departureTime, parameters, this.data, RaptorStopFinder.Direction.EGRESS);
    }

    private RaptorRoute createDirectWalk(Facility fromFacility, Facility toFacility, double departureTime, Person person, RaptorParameters parameters) {
        double beelineDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord());
        double walkTime = beelineDistance / parameters.getBeelineWalkSpeed();
        double walkCost_per_s = -parameters.getMarginalUtilityOfTravelTime_utl_s(TransportMode.transit_walk);
        double walkCost = walkTime * walkCost_per_s;
        double beelineDistanceFactor = this.data.config.getBeelineWalkDistanceFactor();

        RaptorRoute route = new RaptorRoute(fromFacility, toFacility, walkCost);
        route.addNonPt(null, null, departureTime, walkTime, beelineDistance * beelineDistanceFactor, TransportMode.transit_walk);
        return route;
    }

}
