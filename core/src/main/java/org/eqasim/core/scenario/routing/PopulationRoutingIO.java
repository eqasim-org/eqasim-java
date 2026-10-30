package org.eqasim.core.scenario.routing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.databind.MappingIterator;

/** Streaming trip export and route import. Only matched activity references are retained for skip checks. */
public final class PopulationRoutingIO {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(PopulationRoutingIO.class);
    private PopulationRoutingIO() {}

    public static void enrichTrips(Scenario scenario) {
        for (var person : scenario.getPopulation().getPersons().values()) {
            for (int p = 0; p < person.getPlans().size(); p++) {
                List<Trip> trips = TripStructureUtils.getTrips(person.getPlans().get(p));
                for (int t = 0; t < trips.size(); t++) {
                    var attributes = trips.get(t).getTripAttributes();
                    attributes.putAttribute("person_id", person.getId().toString());
                    attributes.putAttribute("trip_index", t);
                    if (attributes.getAttribute("trip_id") == null) attributes.putAttribute("trip_id", Integer.toString(t));
                }
            }
        }
    }

    public static long exportTrips(Scenario scenario, Path path, Set<String> modes) throws IOException {
        CsvSchema schema = CsvSchema.builder().addColumn("person_id").addColumn("trip_id")
                .addColumn("trip_index").addColumn("origin_x").addColumn("origin_y").addColumn("destination_x")
                .addColumn("destination_y").addColumn("departure_time").addColumn("mode")
                .addColumn("origin_link_id").addColumn("destination_link_id").build().withHeader();
        long count = 0;
        try (var writer = new CsvMapper().writer(schema).writeValues(path.toFile())) {
            for (var person : scenario.getPopulation().getPersons().values()) {
                if (person.getSelectedPlan() != null) {
                    var trips = TripStructureUtils.getTrips(person.getSelectedPlan());
                    for (int t = 0; t < trips.size(); t++) {
                        Trip trip = trips.get(t);
                        String mode = TripStructureUtils.identifyMainMode(trip.getTripElements());
                        if (!modes.isEmpty() && !modes.contains(mode)) continue;
                        Facility from = facility(scenario, trip.getOriginActivity());
                        Facility to = facility(scenario, trip.getDestinationActivity());
                        if (from.getCoord() == null || to.getCoord() == null || trip.getOriginActivity().getEndTime().isUndefined())
                            throw new IllegalArgumentException("Trip coordinates/end time missing for person " + person.getId());
                        boolean linked = from.getLinkId() != null && to.getLinkId() != null;
                        writer.write(Map.ofEntries(Map.entry("person_id", person.getId().toString()),
                                Map.entry("trip_id", tripId(trip, t)), Map.entry("trip_index", t),
                                Map.entry("origin_x", from.getCoord().getX()), Map.entry("origin_y", from.getCoord().getY()),
                                Map.entry("destination_x", to.getCoord().getX()), Map.entry("destination_y", to.getCoord().getY()),
                                Map.entry("departure_time", trip.getOriginActivity().getEndTime().seconds()), Map.entry("mode", mode),
                                Map.entry("origin_link_id", linked ? from.getLinkId().toString() : ""),
                                Map.entry("destination_link_id", linked ? to.getLinkId().toString() : "")));
                        count++;
                    }
                }
            }
        }
        if (count == 0) {
            var names = new java.util.ArrayList<String>(); schema.forEach(column -> names.add(column.getName()));
            java.nio.file.Files.writeString(path, String.join(",", names) + "\n");
        }
        return count;
    }

    public static Map<Plan, Set<Activity>> importRoutes(Scenario scenario, Path path, Set<String> modes) throws IOException {
        Map<Plan, Set<Activity>> reused = new IdentityHashMap<>();
        Map<Plan, Set<Activity>> rejected = new IdentityHashMap<>();
        var csv = new CsvMapper();
        long line = 1, skipped = 0, invalid = 0;
        try (MappingIterator<Map<String, String>> rows = csv.readerFor(csv.getTypeFactory()
                .constructMapType(Map.class, String.class, String.class)).with(CsvSchema.emptySchema().withHeader()).readValues(path.toFile())) {
            while (rows.hasNextValue()) {
                Map<String, String> row = rows.nextValue(); line++;
                if (emptyRoute(row.get("route"))) { skipped++; continue; }
                Plan plan = null;
                Trip trip = null;
                try {
                    String personId = required(row, "person_id");
                    var person = scenario.getPopulation().getPersons().get(Id.createPersonId(personId));
                    if (person == null) { skipped++; continue; }
                    plan = person.getSelectedPlan();
                    if (plan == null) throw new IllegalArgumentException("Person has no selected plan");
                    List<Trip> trips = TripStructureUtils.getTrips(plan);
                    if (!blank(row.get("trip_index"))) {
                        int tripIndex = index(row.get("trip_index")); trip = trips.get(tripIndex);
                        if (!blank(row.get("trip_id")) && !row.get("trip_id").equals(tripId(trip, tripIndex)))
                            throw new IllegalArgumentException("trip_id and trip_index identify different trips");
                    } else {
                        String tripId = required(row, "trip_id"); trip = null;
                        for (int t = 0; t < trips.size(); t++) if (tripId.equals(tripId(trips.get(t), t))) {
                            if (trip != null) throw new IllegalArgumentException("Ambiguous trip_id " + tripId);
                            trip = trips.get(t);
                        }
                        if (trip == null) throw new IllegalArgumentException("Unknown trip_id " + tripId);
                    }
                    String mode = TripStructureUtils.identifyMainMode(trip.getTripElements());
                    if ((!modes.isEmpty() && !modes.contains(mode)) || !mode.equals(required(row, "mode"))) { skipped++; continue; }
                    if (rejected.getOrDefault(plan, Set.of()).contains(trip.getOriginActivity()))
                        throw new IllegalArgumentException("Trip already has an invalid or conflicting route; it will be rerouted");
                    checkMetric(row, "travel_time"); checkMetric(row, "travel_distance");
                    var itinerary = ReusableRoute.read(row.get("route"));
                    if (!mode.equals(itinerary.mode())) throw new IllegalArgumentException("Route mode differs from CSV/population mode");
                    if (trip.getOriginActivity().getEndTime().isUndefined()
                            || Math.abs(trip.getOriginActivity().getEndTime().seconds() - itinerary.departureTime()) > 1e-6)
                        throw new IllegalArgumentException("Route departure time differs from population trip");
                    checkCoord(row, "origin", facility(scenario, trip.getOriginActivity()));
                    checkCoord(row, "destination", facility(scenario, trip.getDestinationActivity()));
                    Set<Activity> origins = reused.computeIfAbsent(plan, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()));
                    if (origins.contains(trip.getOriginActivity())) throw new IllegalArgumentException("Duplicate route for the same person/trip/mode");
                    var elements = ReusableRoute.restore(itinerary, scenario.getPopulation().getFactory());
                    if (!mode.equals(TripStructureUtils.identifyMainMode(elements)))
                        throw new IllegalArgumentException("Exported legs have a different routing mode");
                    if (elements.size() == 1 && !mode.equals("pt") && !((Leg) elements.getFirst()).getMode().equals(mode))
                        throw new IllegalArgumentException("Exported leg mode differs from population trip");
                    validate(scenario, trip, elements);
                    TripRouter.insertTrip(plan, trip.getOriginActivity(), elements, trip.getDestinationActivity());
                    // Set absent activity links only after the entire itinerary passes validation.
                    if (elements.size() == 1 && ((Leg) elements.getFirst()).getRoute() instanceof NetworkRoute route) {
                        if (facility(scenario, trip.getOriginActivity()).getLinkId() == null)
                            trip.getOriginActivity().setLinkId(route.getStartLinkId());
                        if (facility(scenario, trip.getDestinationActivity()).getLinkId() == null)
                            trip.getDestinationActivity().setLinkId(route.getEndLinkId());
                    }
                    origins.add(trip.getOriginActivity());
                } catch (RuntimeException e) {
                    invalid++;
                    if (plan != null && trip != null) {
                        rejected.computeIfAbsent(plan, ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                                .add(trip.getOriginActivity());
                        Set<Activity> origins = reused.get(plan);
                        if (origins != null) origins.remove(trip.getOriginActivity());
                    }
                    // Bound warning volume for large or stale route files. Unmarked trips route normally.
                    if (invalid <= 10) LOGGER.warn("Ignoring routes CSV row {}: {}. Falling back to routing.", line, e.getMessage());
                }
            }
        }
        reused.values().removeIf(Set::isEmpty);
        long applied = reused.values().stream().mapToLong(Set::size).sum();
        LOGGER.info("Reused {} routes; skipped {} empty/unmatched rows; {} invalid rows fall back to routing", applied, skipped, invalid);
        return reused;
    }

    private static void validate(Scenario scenario, Trip trip, List<PlanElement> elements) {
        for (var element : elements) if (element instanceof Leg leg) {
            var route = leg.getRoute();
            if ((scenario.getConfig().routing().getNetworkModes().contains(leg.getMode()) || leg.getMode().equals("truck"))
                    && !(route instanceof NetworkRoute)) throw new IllegalArgumentException("Network mode needs a network route");
            for (var id : java.util.Arrays.asList(route.getStartLinkId(), route.getEndLinkId()))
                if (id != null && !scenario.getNetwork().getLinks().containsKey(id)) throw new IllegalArgumentException("Unknown route endpoint link " + id);
            if (route instanceof NetworkRoute network) {
                var ids = new java.util.ArrayList<Id<org.matsim.api.core.v01.network.Link>>();
                ids.add(network.getStartLinkId()); ids.addAll(network.getLinkIds());
                if (!network.getStartLinkId().equals(network.getEndLinkId()) || !network.getLinkIds().isEmpty()) ids.add(network.getEndLinkId());
                org.matsim.api.core.v01.network.Link previous = null;
                for (var id : ids) {
                    var link = scenario.getNetwork().getLinks().get(id);
                    String roadMode = leg.getMode().equals("car_passenger") ? "car" : leg.getMode();
                    if (link == null || !link.getAllowedModes().contains(roadMode)) throw new IllegalArgumentException("Invalid/forbidden link " + id);
                    if (previous != null && !previous.getToNode().getId().equals(link.getFromNode().getId()))
                        throw new IllegalArgumentException("Disconnected exported route at " + id);
                    previous = link;
                }
                if (elements.size() == 1) {
                    checkEndpoint(scenario, trip.getOriginActivity(), route.getStartLinkId());
                    checkEndpoint(scenario, trip.getDestinationActivity(), route.getEndLinkId());
                }
            } else if (route instanceof TransitPassengerRoute pt) {
                var schedule = scenario.getTransitSchedule();
                var line = schedule.getTransitLines().get(pt.getLineId());
                var service = line == null ? null : line.getRoutes().get(pt.getRouteId());
                if (service == null || !schedule.getFacilities().containsKey(pt.getAccessStopId())
                        || !schedule.getFacilities().containsKey(pt.getEgressStopId())
                        || service.getStops().stream().noneMatch(stop -> stop.getStopFacility().getId().equals(pt.getAccessStopId()))
                        || service.getStops().stream().noneMatch(stop -> stop.getStopFacility().getId().equals(pt.getEgressStopId())))
                    throw new IllegalArgumentException("Transit itinerary is absent from the loaded schedule");
            }
        }
    }

    private static void checkEndpoint(Scenario scenario, Activity activity, Id<org.matsim.api.core.v01.network.Link> link) {
        var expected = facility(scenario, activity).getLinkId();
        if (expected != null && !expected.equals(link))
            throw new IllegalArgumentException("Activity/route endpoint links differ; route CSV trips with origin_link_id and destination_link_id");
    }

    public static boolean needsRouting(Scenario scenario, Set<String> modes, Map<Plan, Set<Activity>> reused) {
        for (var person : scenario.getPopulation().getPersons().values()) for (Plan plan : person.getPlans())
            for (Trip trip : TripStructureUtils.getTrips(plan))
                if (!reused.getOrDefault(plan, Set.of()).contains(trip.getOriginActivity())
                        && (modes.isEmpty() || modes.contains(TripStructureUtils.identifyMainMode(trip.getTripElements())))) return true;
        return false;
    }

    private static Facility facility(Scenario scenario, Activity activity) {
        return FacilitiesUtils.toFacility(activity, scenario.getActivityFacilities());
    }
    private static String tripId(Trip trip, int index) {
        Object value = trip.getTripAttributes().getAttribute("trip_id");
        return value == null ? Integer.toString(index) : value.toString();
    }
    private static void checkCoord(Map<String, String> row, String prefix, Facility facility) {
        if (blank(row.get(prefix + "_x")) || blank(row.get(prefix + "_y"))) return;
        double x = Double.parseDouble(row.get(prefix + "_x")), y = Double.parseDouble(row.get(prefix + "_y"));
        if (!Double.isFinite(x) || !Double.isFinite(y) || facility.getCoord() == null
                || Math.hypot(x - facility.getCoord().getX(), y - facility.getCoord().getY()) > 1)
            throw new IllegalArgumentException(prefix + " coordinates differ from population trip");
    }
    private static String required(Map<String, String> row, String name) {
        if (blank(row.get(name))) throw new IllegalArgumentException("Missing " + name);
        return row.get(name);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static boolean emptyRoute(String value) {
        if (blank(value)) return true;
        String sentinel = value.trim();
        return sentinel.equalsIgnoreCase("nan") || sentinel.equalsIgnoreCase("null") || sentinel.equalsIgnoreCase("none");
    }
    private static void checkMetric(Map<String, String> row, String name) {
        if (blank(row.get(name))) return;
        double value = Double.parseDouble(row.get(name));
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid " + name);
    }
    private static int index(String value) {
        int index = Integer.parseInt(value); if (index < 0) throw new IllegalArgumentException("Indices must be zero-based and non-negative");
        return index;
    }
}
