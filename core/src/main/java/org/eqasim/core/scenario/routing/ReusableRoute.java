package org.eqasim.core.scenario.routing;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Versioned, lossless routing essentials, shared by CSV routers and population routing. */
public final class ReusableRoute {
    private static final ObjectMapper JSON = new ObjectMapper();

    public record Itinerary(int version, String mode, double departureTime, List<Part> parts) {}
    public record RouteData(String type, String startLink, String endLink, String description,
            double distance, Double travelTime, List<String> links) {}
    public record Part(String kind, String mode, String routingMode, Double departureTime, Double travelTime,
            RouteData route, String link, String facility, Double x, Double y,
            Double startTime, Double endTime, Double maximumDuration) {}

    private ReusableRoute() {}

    public static String encode(String mode, double departureTime, List<? extends PlanElement> elements) {
        List<Part> parts = new ArrayList<>();
        for (PlanElement element : elements) {
            if (element instanceof Leg leg) {
                Route route = leg.getRoute();
                if (route == null) throw new IllegalArgumentException("Cannot export a leg without a route");
                List<String> links = null;
                if (route instanceof NetworkRoute network) {
                    links = new ArrayList<>();
                    links.add(network.getStartLinkId().toString());
                    for (var id : network.getLinkIds()) links.add(id.toString());
                    if (!network.getStartLinkId().equals(network.getEndLinkId()) || !network.getLinkIds().isEmpty())
                        links.add(network.getEndLinkId().toString());
                }
                var data = new RouteData(route.getRouteType(), string(route.getStartLinkId()), string(route.getEndLinkId()),
                        route.getRouteDescription(), route.getDistance(), seconds(route.getTravelTime()), links);
                parts.add(new Part("leg", leg.getMode(), TripStructureUtils.getRoutingMode(leg),
                        seconds(leg.getDepartureTime()), seconds(leg.getTravelTime()), data,
                        null, null, null, null, null, null, null));
            } else if (element instanceof Activity activity) {
                Coord coord = activity.getCoord();
                parts.add(new Part("activity", activity.getType(), null, null, null, null,
                        string(activity.getLinkId()), string(activity.getFacilityId()),
                        coord == null ? null : coord.getX(), coord == null ? null : coord.getY(),
                        seconds(activity.getStartTime()), seconds(activity.getEndTime()), seconds(activity.getMaximumDuration())));
            } else throw new IllegalArgumentException("Unsupported exported plan element " + element);
        }
        return write(new Itinerary(1, mode, departureTime, parts));
    }

    public static Itinerary read(String value) {
        try {
            Itinerary result = JSON.readValue(value, Itinerary.class);
            if (result == null || result.version != 1 || result.mode == null || result.parts == null
                    || result.parts.isEmpty() || !Double.isFinite(result.departureTime) || result.departureTime < 0)
                throw new IllegalArgumentException("Invalid reusable route or unsupported version");
            return result;
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid reusable route JSON", e);
        }
    }

    public static String write(Itinerary value) {
        try { return JSON.writeValueAsString(value); }
        catch (java.io.IOException e) { throw new IllegalArgumentException("Cannot serialize reusable route", e); }
    }

    /** MARouter reports every assigned path at the final bin times, including exported leg times. */
    public static String withTravelTime(String value, double travelTime) {
        Itinerary itinerary = read(value);
        if (itinerary.parts.size() != 1 || !itinerary.parts.getFirst().kind.equals("leg"))
            throw new IllegalArgumentException("Expected one network leg for incremental assignment");
        Part part = itinerary.parts.getFirst();
        RouteData route = part.route;
        var updatedRoute = new RouteData(route.type, route.startLink, route.endLink, route.description,
                route.distance, travelTime, route.links);
        var updatedPart = new Part(part.kind, part.mode, part.routingMode, part.departureTime, travelTime,
                updatedRoute, part.link, part.facility, part.x, part.y, part.startTime, part.endTime, part.maximumDuration);
        return write(new Itinerary(itinerary.version, itinerary.mode, itinerary.departureTime, List.of(updatedPart)));
    }

    public static List<PlanElement> restore(Itinerary itinerary, PopulationFactory factory) {
        List<PlanElement> elements = new ArrayList<>();
        for (Part part : itinerary.parts) {
            if (part == null || part.kind == null || part.mode == null) throw new IllegalArgumentException("Invalid route part");
            if (part.kind.equals("leg")) {
                RouteData data = part.route;
                if (data == null || data.type == null || !Double.isFinite(data.distance) || data.distance < 0)
                    throw new IllegalArgumentException("Invalid exported leg route");
                var routes = factory.getRouteFactories();
                Route route = routes.createRoute(routes.getRouteClassForType(data.type),
                        data.startLink == null ? null : Id.createLinkId(data.startLink),
                        data.endLink == null ? null : Id.createLinkId(data.endLink));
                if (!route.getRouteType().equals(data.type)) throw new IllegalArgumentException("Unsupported route type " + data.type);
                if (route instanceof NetworkRoute network) {
                    if (data.links == null || data.links.isEmpty() || !data.links.getFirst().equals(data.startLink)
                            || !data.links.getLast().equals(data.endLink)) throw new IllegalArgumentException("Invalid network link sequence");
                    network.setLinkIds(route.getStartLinkId(), data.links.size() < 3 ? List.of()
                            : data.links.subList(1, data.links.size() - 1).stream().map(Id::createLinkId).toList(), route.getEndLinkId());
                } else route.setRouteDescription(data.description);
                route.setDistance(data.distance);
                if (data.travelTime != null) { nonnegative(data.travelTime); route.setTravelTime(data.travelTime); }
                Leg leg = factory.createLeg(part.mode);
                leg.setRoute(route);
                if (part.departureTime != null) { nonnegative(part.departureTime); leg.setDepartureTime(part.departureTime); }
                if (part.travelTime == null) throw new IllegalArgumentException("Exported leg needs a travel time");
                nonnegative(part.travelTime); leg.setTravelTime(part.travelTime);
                TripStructureUtils.setRoutingMode(leg, part.routingMode == null ? itinerary.mode : part.routingMode);
                elements.add(leg);
            } else if (part.kind.equals("activity")) {
                if (!TripStructureUtils.isStageActivityType(part.mode)) throw new IllegalArgumentException("Only stage activities may occur inside a route");
                Activity activity = factory.createActivityFromCoord(part.mode,
                        part.x == null || part.y == null ? null : new Coord(part.x, part.y));
                if (part.link != null) activity.setLinkId(Id.createLinkId(part.link));
                if (part.facility != null) activity.setFacilityId(Id.create(part.facility, org.matsim.facilities.ActivityFacility.class));
                if (part.startTime != null) { nonnegative(part.startTime); activity.setStartTime(part.startTime); }
                if (part.endTime != null) { nonnegative(part.endTime); activity.setEndTime(part.endTime); }
                if (part.maximumDuration != null) { nonnegative(part.maximumDuration); activity.setMaximumDuration(part.maximumDuration); }
                elements.add(activity);
            } else throw new IllegalArgumentException("Unknown exported part kind " + part.kind);
        }
        if (!(elements.getFirst() instanceof Leg) || !(elements.getLast() instanceof Leg))
            throw new IllegalArgumentException("Exported itinerary must start and end with a leg");
        return elements;
    }

    private static void nonnegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid exported route time");
    }
    private static Double seconds(OptionalTime time) { return time.isDefined() ? time.seconds() : null; }
    private static String string(Object value) { return value == null ? null : value.toString(); }
}
