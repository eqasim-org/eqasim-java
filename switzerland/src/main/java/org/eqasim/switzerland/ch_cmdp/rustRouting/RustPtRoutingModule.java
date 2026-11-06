// Converts native journeys to fresh MATSim passenger routes, walk legs, and PT stage activities.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

final class RustPtRoutingModule implements RoutingModule {
    private final RustRoutingRuntime runtime;
    private final RoutingModule walk;

    RustPtRoutingModule(RustRoutingRuntime runtime, RoutingModule walk) {
        this.runtime = runtime;
        this.walk = walk;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        var legs = transitLegs(request);
        if (legs == null) return walk.calcRoute(request);
        List<PlanElement> result = new ArrayList<>(legs.size() * 2);
        Leg previous = null;
        for (var element : legs) {
            Leg leg = (Leg) element;
            if (previous != null) {
                TransitStopFacility stop;
                var schedule = runtime.prepared().snapshot().scenario.getTransitSchedule();
                if (previous.getRoute() instanceof TransitPassengerRoute route)
                    stop = schedule.getFacilities().get(route.getEgressStopId());
                else if (leg.getRoute() instanceof TransitPassengerRoute route)
                    stop = schedule.getFacilities().get(route.getAccessStopId());
                else stop = null;
                var coord =
                        stop == null
                                ? runtime.prepared()
                                        .snapshot()
                                        .scenario
                                        .getNetwork()
                                        .getLinks()
                                        .get(previous.getRoute().getEndLinkId())
                                        .getToNode()
                                        .getCoord()
                                : stop.getCoord();
                result.add(
                        PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(
                                coord, leg.getRoute().getStartLinkId(), TransportMode.pt));
            }
            result.add(leg);
            previous = leg;
        }
        return result;
    }

    /**
     * A null transit result follows MATSim's normal walk fallback, rather than raising no-path
     * errors.
     */
    List<? extends PlanElement> transitLegs(RoutingRequest request) {
        var ready = runtime.prepared();
        var snapshot = ready.snapshot();
        double radius = snapshot.parameters.getSearchRadius();
        double[] journey = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            if (attempt > 0) runtime.recordExpansion();
            var access = candidates(snapshot, request.getFromFacility(), radius, true);
            var egress = candidates(snapshot, request.getToFacility(), radius, false);
            journey =
                    runtime.routeNative(
                            ready.handle(),
                            NativePtRouter.millis(request.getDepartureTime()),
                            runtime.settings.maxBoardings,
                            NativePtRouter.millis(runtime.settings.maxDurationSeconds),
                            access,
                            egress);
            if (journey.length > 0) break;
            double explored = radius;
            for (int i = 0; i < access.length; i += 2)
                explored =
                        Math.max(
                                explored,
                                RustTimetableSnapshot.distance(
                                        request.getFromFacility().getCoord(),
                                        snapshot.stops[access[i]].getCoord()));
            for (int i = 0; i < egress.length; i += 2)
                explored =
                        Math.max(
                                explored,
                                RustTimetableSnapshot.distance(
                                        request.getToFacility().getCoord(),
                                        snapshot.stops[egress[i]].getCoord()));
            radius =
                    Math.max(
                            explored * 2,
                            explored + Math.max(1, snapshot.parameters.getExtensionRadius()));
        }
        if (journey == null || journey.length == 0) return null;
        double directTime =
                RustTimetableSnapshot.distance(
                                request.getFromFacility().getCoord(),
                                request.getToFacility().getCoord())
                        / snapshot.parameters.getBeelineWalkSpeed();
        double directCost =
                directTime * -snapshot.parameters.getMarginalUtilityOfTravelTime_utl_s("walk");
        if (directCost * snapshot.parameters.getDirectWalkFactor() < journey[0]) return null;
        return convert(request, snapshot, journey);
    }

    private static int[] candidates(
            RustTimetableSnapshot snapshot, Facility facility, double radius, boolean access) {
        var coord = facility.getCoord();
        Collection<Integer> stops = snapshot.spatial.getDisk(coord.getX(), coord.getY(), radius);
        if (stops.size() < 2) {
            int closest = snapshot.spatial.getClosest(coord.getX(), coord.getY());
            double nearest =
                    RustTimetableSnapshot.distance(coord, snapshot.stops[closest].getCoord());
            stops =
                    snapshot.spatial.getDisk(
                            coord.getX(),
                            coord.getY(),
                            nearest + snapshot.parameters.getExtensionRadius());
        }
        // Stable integer order makes ties reproducible regardless of QuadTree traversal order.
        int[] ids = new int[stops.size()];
        int position = 0;
        for (int id : stops) ids[position++] = id;
        Arrays.sort(ids);
        int[] pairs = new int[ids.length * 2];
        for (int i = 0; i < ids.length; i++) {
            var stop = snapshot.stops[ids[i]];
            double extra =
                    access ? snapshot.stopAccessSeconds[ids[i]] : snapshot.stopEgressSeconds[ids[i]];
            pairs[2 * i] = ids[i];
            pairs[2 * i + 1] =
                    Math.toIntExact(
                            NativePtRouter.millis(
                                    Math.ceil(
                                                    RustTimetableSnapshot.distance(
                                                                    coord, stop.getCoord())
                                                            / snapshot.parameters
                                                                    .getBeelineWalkSpeed())
                                            + extra));
        }
        return pairs;
    }

    private static List<Leg> convert(
            RoutingRequest request, RustTimetableSnapshot snapshot, double[] data) {
        List<Leg> legs = new ArrayList<>();
        var first = snapshot.stops[(int) data[1]];
        double time = request.getDepartureTime();
        legs.add(walk(request.getFromFacility(), first, time, data[2] / 1000, snapshot));
        time += data[2] / 1000;
        boolean boarded = false;
        for (int i = 5; i < data.length; i += 6) {
            var from = snapshot.stops[(int) data[i + 2]];
            var to = snapshot.stops[(int) data[i + 3]];
            double leave = data[i + 4] / 1000, arrival = data[i + 5] / 1000;
            if (data[i] == 0) {
                Leg transfer = walk(from, to, leave, arrival - leave, snapshot);
                // Raptor subtracts its transfer margin only from walks followed by another PT ride.
                boolean anotherRide = false;
                for (int j = i + 6; j < data.length; j += 6)
                    if (data[j] == 1) {
                        anotherRide = true;
                        break;
                    }
                if (boarded && anotherRide) {
                    double duration =
                            Math.max(
                                    0,
                                    transfer.getTravelTime().seconds()
                                            - snapshot.walking.getTransferWalkMargin());
                    transfer.setTravelTime(duration);
                    transfer.getRoute().setTravelTime(duration);
                }
                legs.add(transfer);
            } else {
                var trip = snapshot.trips.get((int) data[i + 1]);
                var profile = trip.profile();
                var stops = profile.route().getStops();
                int start = -1, end = -1;
                for (int k = 0; k < stops.size(); k++) {
                    var stop = stops.get(k);
                    if (stop.getStopFacility() == from
                            && Math.abs(
                                            trip.departure()
                                                    + stop.getDepartureOffset()
                                                            .orElse(
                                                                    stop.getArrivalOffset()
                                                                            .orElse(0))
                                                    - leave)
                                    < .002) {
                        start = k;
                        break;
                    }
                }
                for (int k = start + 1; start >= 0 && k < stops.size(); k++) {
                    var stop = stops.get(k);
                    if (stop.getStopFacility() == to
                            && Math.abs(
                                            trip.departure()
                                                    + stop.getArrivalOffset()
                                                            .orElse(
                                                                    stop.getDepartureOffset()
                                                                            .orElse(0))
                                                    - arrival)
                                    < .002) {
                        end = k;
                        break;
                    }
                }
                if (start < 0 || end < 0)
                    throw new IllegalStateException(
                            "Native journey does not match schedule route "
                                    + profile.route().getId());
                var route =
                        new DefaultTransitPassengerRoute(from, profile.line(), profile.route(), to);
                double vehicleArrival =
                        trip.departure()
                                + stops.get(start)
                                        .getArrivalOffset()
                                        .orElse(stops.get(start).getDepartureOffset().orElse(0));
                route.setBoardingTime(Math.max(time, vehicleArrival));
                route.setTravelTime(arrival - time);
                route.setDistance(profile.distances()[end] - profile.distances()[start]);
                // Timetable submodes affect search costs; MATSim's passenger engine handles mode
                // "pt".
                Leg leg = PopulationUtils.createLeg(TransportMode.pt);
                leg.setDepartureTime(time);
                leg.setTravelTime(arrival - time);
                leg.setRoute(route);
                legs.add(leg);
                boarded = true;
            }
            time = arrival;
        }
        legs.add(
                walk(
                        snapshot.stops[(int) data[3]],
                        request.getToFacility(),
                        time,
                        data[4] / 1000,
                        snapshot));
        for (var leg : legs)
            leg.getAttributes().putAttribute(RaptorUtils.TOTAL_ROUTE_COST_ATTR_NAME, data[0]);
        return legs;
    }

    private static Leg walk(
            Facility from,
            Facility to,
            double departure,
            double duration,
            RustTimetableSnapshot snapshot) {
        Leg leg = PopulationUtils.createLeg("walk");
        leg.setDepartureTime(departure);
        leg.setTravelTime(duration);
        var route = RouteUtils.createGenericRouteImpl(from.getLinkId(), to.getLinkId());
        route.setTravelTime(duration);
        route.setDistance(
                RustTimetableSnapshot.distance(from.getCoord(), to.getCoord())
                        * snapshot.walking.getBeelineWalkDistanceFactor());
        leg.setRoute(route);
        return leg;
    }
}
