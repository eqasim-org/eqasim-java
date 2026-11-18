package org.eqasim.core.scenario.cutter.population.trips;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder.NoDepartureFoundException;
import org.eqasim.core.scenario.cutter.extent.LinkRegionClassifier;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkRouteCrossingPointFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

/** Cuts individual routed legs, preserving the other legs and their interaction activities. */
public class RoutedTripProcessor implements TripProcessor {
    private final ScenarioExtent extent;
    private final Network network;
    private final NetworkRouteCrossingPointFinder networkFinder;
    private final TransitSchedule schedule;
    private final DepartureFinder departureFinder;

    @Inject
    public RoutedTripProcessor(ScenarioExtent extent, Network network,
            NetworkRouteCrossingPointFinder networkFinder, TransitSchedule schedule, DepartureFinder departureFinder) {
        this.extent = extent;
        this.network = network;
        this.networkFinder = networkFinder;
        this.schedule = schedule;
        this.departureFinder = departureFinder;
    }

    private record Part(Activity from, Activity to, Leg leg, double departure, double arrival) { }

    @Override
    public List<PlanElement> process(Id<Person> personId, int firstLegIndex, Activity origin,
            List<PlanElement> trip, Activity destination, String routingMode) {
        List<Part> parts = new ArrayList<>();
        int legIndex = firstLegIndex;
        for (int i = 0; i < trip.size(); i += 2) {
            Leg leg = (Leg) trip.get(i);
            if (leg.getRoute() == null) {
                throw new IllegalArgumentException("Scenario cutting requires routed input: person " + personId
                        + ", leg " + legIndex);
            }
            Activity from = i == 0 ? origin : (Activity) trip.get(i - 1);
            Activity to = i == trip.size() - 1 ? destination : (Activity) trip.get(i + 1);
            if (leg.getRoute() instanceof TransitPassengerRoute transitRoute) {
                cutTransit(parts, from, to, leg, transitRoute);
            } else if (leg.getRoute() instanceof NetworkRoute networkRoute) {
                cutNetwork(parts, personId, legIndex, from, to, leg, networkRoute);
            } else {
                cutTeleportation(parts, from, to, leg);
            }
            legIndex++;
        }
        List<PlanElement> result = assemble(parts, origin, destination, trip);
        // A retained access/egress fragment can genuinely be walk-only after the vehicle journey is removed.
        List<PlanElement> plan = new ArrayList<>();
        plan.add(origin); plan.addAll(result); plan.add(destination);
        for (var retainedTrip : TripStructureUtils.getTrips(plan)) {
            if (retainedTrip.getLegsOnly().stream().allMatch(l -> l.getMode().equals("walk"))) {
                retainedTrip.getLegsOnly().forEach(l -> l.setRoutingMode("walk"));
            }
        }
        return result;
    }

    /** Interpolate only the clipped access/egress leg, never the main transport leg. */
    private void cutTeleportation(List<Part> parts, Activity from, Activity to, Leg leg) {
        Coord a = from.getCoord(), b = to.getCoord();
        double dx = b.getX() - a.getX(), dy = b.getY() - a.getY();
        double lengthSquared = dx * dx + dy * dy;
        double departure = leg.getDepartureTime().seconds(), duration = leg.getTravelTime().seconds();
        if (lengthSquared == 0) {
            if (extent.isInside(a)) parts.add(whole(from, to, leg));
            return;
        }
        List<Double> cuts = new ArrayList<>(List.of(0.0, 1.0));
        for (Coord crossing : extent.computeEuclideanIntersections(a, b)) {
            double t = ((crossing.getX() - a.getX()) * dx + (crossing.getY() - a.getY()) * dy) / lengthSquared;
            if (t > 1e-10 && t < 1 - 1e-10) cuts.add(t);
        }
        cuts.sort(Comparator.naturalOrder());
        for (int i = 0; i < cuts.size() - 1; i++) {
            double start = cuts.get(i), end = cuts.get(i + 1);
            if (end - start < 1e-10 || !extent.isInside(interpolate(a, b, (start + end) / 2))) continue;
            if (start == 0 && end == 1) {
                parts.add(whole(from, to, leg));
                continue;
            }
            Activity clippedFrom = start == 0 ? from : boundary(interpolate(a, b, start), null, departure + duration * start);
            Activity clippedTo = end == 1 ? to : boundary(interpolate(a, b, end), null, departure + duration * end);
            Leg clipped = PopulationUtils.createLeg(leg);
            clipped.setRoute(leg.getRoute().clone());
            clipped.setDepartureTime(departure + duration * start);
            clipped.setTravelTime(duration * (end - start));
            clipped.getRoute().setTravelTime(clipped.getTravelTime().seconds());
            clipped.getRoute().setDistance(leg.getRoute().getDistance() * (end - start));
            clipped.getRoute().setStartLinkId(clippedFrom.getLinkId());
            clipped.getRoute().setEndLinkId(clippedTo.getLinkId());
            parts.add(new Part(clippedFrom, clippedTo, clipped, departure + duration * start, departure + duration * end));
        }
    }

    /** Retain crossing links and slice by position, so repeated link IDs are handled correctly. */
    private void cutNetwork(List<Part> parts, Id<Person> personId, int legIndex, Activity from, Activity to,
            Leg leg, NetworkRoute route) {
        List<Id<Link>> links = new ArrayList<>();
        links.add(route.getStartLinkId());
        links.addAll(route.getLinkIds());
        if (links.size() > 1 || !route.getStartLinkId().equals(route.getEndLinkId())) links.add(route.getEndLinkId());
        var crossings = networkFinder.findCrossingPoints(personId, legIndex, leg.getMode(), route,
                leg.getDepartureTime().seconds());
        if (crossings.isEmpty()) {
            if (networkFinder.isInside(route)) parts.add(whole(from, to, leg));
            return;
        }
        int start = LinkRegionClassifier.isFromNodeInside(network.getLinks().get(links.getFirst())) ? 0 : -1;
        Activity startActivity = from;
        double startTime = leg.getDepartureTime().seconds();
        // Some route implementations represent a same-link trip with identical start/end IDs.
        int previousCrossing = -1;
        for (var crossing : crossings) {
            if (crossing.index >= links.size() || crossing.index == previousCrossing) continue;
            previousCrossing = crossing.index;
            // A previous cut already placed this endpoint on the crossing link. Do not cut it again.
            if (!crossing.isOutgoing && crossing.index == 0 && anchoredAt(from, crossing.link)) {
                start = 0;
                continue;
            }
            if (crossing.isOutgoing && crossing.index == links.size() - 1 && anchoredAt(to, crossing.link)) continue;
            Activity anchor = boundary(crossing.link.getCoord(), crossing.link.getId(), crossing.leaveTime);
            if (crossing.isOutgoing) {
                if (start >= 0) parts.add(networkPart(from, to, leg, route, links, start, crossing.index,
                        startActivity, anchor, startTime, crossing.leaveTime));
                start = -1;
            } else {
                start = crossing.index;
                startActivity = anchor;
                startTime = crossing.leaveTime;
            }
        }
        if (start >= 0) parts.add(networkPart(from, to, leg, route, links, start, links.size() - 1,
                startActivity, to, startTime, leg.getDepartureTime().seconds() + leg.getTravelTime().seconds()));
    }

    private static boolean anchoredAt(Activity activity, Link link) {
        return link.getId().equals(activity.getLinkId()) && link.getCoord().equals(activity.getCoord());
    }

    private Part networkPart(Activity originalFrom, Activity originalTo, Leg leg, NetworkRoute route,
            List<Id<Link>> links, int start, int end, Activity from, Activity to, double departure, double arrival) {
        if (from == originalFrom && to == originalTo) return whole(from, to, leg);
        NetworkRoute clippedRoute = route.clone();
        clippedRoute.setLinkIds(links.get(start), start == end ? List.of() : links.subList(start + 1, end), links.get(end));
        clippedRoute.setDistance(links.subList(start, end + 1).stream()
                .mapToDouble(id -> network.getLinks().get(id).getLength()).sum());
        clippedRoute.setTravelTime(Math.max(0, arrival - departure));
        clippedRoute.setTravelCost(Double.NaN);
        Leg clipped = PopulationUtils.createLeg(leg);
        clipped.setRoute(clippedRoute);
        clipped.setDepartureTime(departure);
        clipped.setTravelTime(Math.max(0, arrival - departure));
        return new Part(from, to, clipped, departure, departure + clipped.getTravelTime().seconds());
    }

    /** Keep the first/last adjacent outside stop and the full sequence across exits and re-entries. */
    private void cutTransit(List<Part> parts, Activity from, Activity to, Leg leg, TransitPassengerRoute route) {
        if (route.getChainedRoute() != null) {
            throw new IllegalArgumentException("Cutting chained PT routes requires separate passenger legs");
        }
        var line = schedule.getTransitLines().get(route.getLineId());
        var service = line.getRoutes().get(route.getRouteId());
        try {
            var departure = departureFinder.findNextDeparture(service, route.getAccessStopId(), route.getEgressStopId(),
                    route.getBoardingTime().orElse(leg.getDepartureTime().seconds()));
            List<TransitRouteStop> stops = service.getStops();
            int access = stops.indexOf(departure.stop), egress = -1;
            for (int i = access + 1; i < stops.size(); i++) {
                if (stops.get(i).getStopFacility().getId().equals(route.getEgressStopId())) { egress = i; break; }
            }
            if (access < 0 || egress < 0) throw new IllegalArgumentException("Invalid PT stop sequence: " + route.getRouteId());
            int first = -1, last = -1;
            for (int i = access; i <= egress; i++) {
                if (extent.isInside(stops.get(i).getStopFacility().getCoord())) {
                    if (first < 0) first = i;
                    last = i;
                }
            }
            if (first < 0) return;
            int start = Math.max(access, first - 1), end = Math.min(egress, last + 1);
            if (start == access && end == egress) { addTransitPart(parts, whole(from, to, leg)); return; }
            var firstStop = stops.get(start); var lastStop = stops.get(end);
            double baseTime = departure.departure.getDepartureTime();
            double boarding = baseTime + departureOffset(firstStop);
            double arrival = end == egress ? leg.getDepartureTime().seconds() + leg.getTravelTime().seconds()
                    : baseTime + lastStop.getArrivalOffset().orElse(departureOffset(lastStop));
            double startTime = start == access ? leg.getDepartureTime().seconds() : boarding;
            Activity clippedFrom = start == access ? from : boundary(firstStop.getStopFacility().getCoord(),
                    firstStop.getStopFacility().getLinkId(), startTime);
            Activity clippedTo = end == egress ? to : boundary(lastStop.getStopFacility().getCoord(),
                    lastStop.getStopFacility().getLinkId(), arrival);
            var clippedRoute = new DefaultTransitPassengerRoute(firstStop.getStopFacility(), line, service, lastStop.getStopFacility());
            clippedRoute.setBoardingTime(boarding);
            clippedRoute.setTravelTime(Math.max(0, arrival - startTime));
            clippedRoute.setDistance(transitDistance(service.getRoute(), stops, start, end));
            Leg clipped = PopulationUtils.createLeg(leg);
            clipped.setRoute(clippedRoute);
            clipped.setDepartureTime(startTime);
            clipped.setTravelTime(Math.max(0, arrival - startTime));
            addTransitPart(parts, new Part(clippedFrom, clippedTo, clipped, startTime, arrival));
        } catch (NoDepartureFoundException e) {
            throw new IllegalArgumentException("No scheduled departure for " + route.getLineId() + "/" + route.getRouteId(), e);
        }
    }

    /** Outside PT stops can be on rail-only links: keep a stage activity between the road anchor and PT. */
    private void addTransitPart(List<Part> parts, Part part) {
        Activity from = part.from, to = part.to;
        boolean outsideStart = !extent.isInside(from.getCoord()), outsideEnd = !extent.isInside(to.getCoord());
        if (outsideStart) {
            from = transitInteraction(part.from, part.leg.getRoute().getStartLinkId(), part.departure);
            Activity anchor = boundary(part.from.getCoord(), part.from.getLinkId(), part.departure);
            parts.add(new Part(anchor, from, zeroWalk(anchor, from, part.leg.getRoutingMode(), part.departure),
                    part.departure, part.departure));
        }
        if (outsideEnd) to = transitInteraction(part.to, part.leg.getRoute().getEndLinkId(), part.arrival);
        parts.add(new Part(from, to, part.leg, part.departure, part.arrival));
        if (outsideEnd) {
            Activity anchor = boundary(part.to.getCoord(), part.to.getLinkId(), part.arrival);
            parts.add(new Part(to, anchor, zeroWalk(to, anchor, part.leg.getRoutingMode(), part.arrival),
                    part.arrival, part.arrival));
        }
    }

    private static Activity transitInteraction(Activity endpoint, Id<Link> link, double time) {
        Activity stage = PopulationUtils.createActivityFromCoordAndLinkId("pt interaction", endpoint.getCoord(), link);
        stage.setMaximumDuration(0); stage.setEndTime(time);
        return stage;
    }

    private static Leg zeroWalk(Activity from, Activity to, String routingMode, double time) {
        Leg leg = outsideLeg(from, to, time, time);
        leg.setMode("walk"); leg.setRoutingMode(routingMode);
        return leg;
    }

    /** Resolve stops in traversal order rather than matching the first occurrence of a repeated link. */
    private double transitDistance(NetworkRoute route, List<TransitRouteStop> stops, int start, int end) {
        List<Id<Link>> links = new ArrayList<>();
        links.add(route.getStartLinkId()); links.addAll(route.getLinkIds()); links.add(route.getEndLinkId());
        int cursor = 0, firstLink = -1;
        for (int i = 0; i <= end; i++) {
            Id<Link> stopLink = stops.get(i).getStopFacility().getLinkId();
            if (i > 0 && stops.get(i - 1).getStopFacility().getId().equals(stops.get(i).getStopFacility().getId())) cursor++;
            while (cursor < links.size() && !links.get(cursor).equals(stopLink)) cursor++;
            if (cursor == links.size()) throw new IllegalArgumentException("PT stop is not on its network route: " + stopLink);
            if (i == start) firstLink = cursor;
        }
        return links.subList(firstLink + 1, cursor + 1).stream()
                .mapToDouble(id -> network.getLinks().get(id).getLength()).sum();
    }

    private static double departureOffset(TransitRouteStop stop) {
        return stop.getDepartureOffset().isDefined() ? stop.getDepartureOffset().seconds() : stop.getArrivalOffset().seconds();
    }

    private static Part whole(Activity from, Activity to, Leg leg) {
        return new Part(from, to, leg, leg.getDepartureTime().seconds(),
                leg.getDepartureTime().seconds() + leg.getTravelTime().seconds());
    }

    private static Coord interpolate(Coord a, Coord b, double t) {
        return new Coord(a.getX() + t * (b.getX() - a.getX()), a.getY() + t * (b.getY() - a.getY()));
    }

    private static Activity boundary(Coord coord, Id<Link> link, double time) {
        Activity activity = PopulationUtils.createActivityFromCoordAndLinkId("outside", coord, link);
        activity.setEndTime(time);
        return activity;
    }

    /** Bridge excluded portions with outside legs, leaving adjacent retained stage activities untouched. */
    private List<PlanElement> assemble(List<Part> parts, Activity origin, Activity destination, List<PlanElement> original) {
        double departure = ((Leg) original.getFirst()).getDepartureTime().seconds();
        Leg last = (Leg) original.getLast();
        double arrival = last.getDepartureTime().seconds() + last.getTravelTime().seconds();
        if (parts.isEmpty()) return List.of(outsideLeg(origin, destination, departure, arrival));
        List<PlanElement> result = new ArrayList<>();
        Part first = parts.getFirst();
        if (first.from != origin) {
            Activity anchor = boundary(first.from.getCoord(), first.from.getLinkId(), first.departure);
            result.add(outsideLeg(origin, anchor, departure, first.departure)); result.add(anchor);
        }
        for (int i = 0; i < parts.size(); i++) {
            Part part = parts.get(i);
            result.add(part.leg);
            if (i + 1 < parts.size()) {
                Part next = parts.get(i + 1);
                if (part.to == next.from) {
                    result.add(part.to);
                } else {
                    Activity exit = boundary(part.to.getCoord(), part.to.getLinkId(), part.arrival);
                    Activity entry = boundary(next.from.getCoord(), next.from.getLinkId(), next.departure);
                    result.add(exit); result.add(outsideLeg(exit, entry, part.arrival, next.departure)); result.add(entry);
                }
            }
        }
        Part tail = parts.getLast();
        if (tail.to != destination) {
            Activity anchor = boundary(tail.to.getCoord(), tail.to.getLinkId(), tail.arrival);
            result.add(anchor); result.add(outsideLeg(anchor, destination, tail.arrival, arrival));
        }
        return result;
    }

    private static Leg outsideLeg(Activity from, Activity to, double departure, double arrival) {
        Leg leg = PopulationUtils.createLeg("outside");
        leg.setRoutingMode("outside"); leg.setDepartureTime(departure); leg.setTravelTime(Math.max(0, arrival - departure));
        var route = RouteUtils.createGenericRouteImpl(from.getLinkId(), to.getLinkId());
        route.setTravelTime(leg.getTravelTime().seconds());
        route.setDistance(Math.hypot(from.getCoord().getX() - to.getCoord().getX(), from.getCoord().getY() - to.getCoord().getY()));
        leg.setRoute(route);
        return leg;
    }
}
