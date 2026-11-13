package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.RegionalSubscription;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

public class SwissPtRoutePredictor extends CachedVariablePredictor<SwissPtVariables> {
    private static final Logger LOG = LogManager.getLogger(SwissPtRoutePredictor.class);

    private static final Pattern LINK_SUFFIX = Pattern.compile("\\.link.*$");
    private static final Pattern ID_SEPARATOR = Pattern.compile("[:\\.]");
    private static final Pattern SLOID = Pattern.compile("^(?:Parent)?ch:1:sloid:(\\d+)");

    // Each anomaly is logged once: this predictor runs for every PT trip of every mode choice
    private static final AtomicBoolean warnedStopsNotOnRoute = new AtomicBoolean();
    private static final AtomicBoolean warnedMissingSbbDistance = new AtomicBoolean();
    private static final AtomicBoolean warnedNoTransitRoute = new AtomicBoolean();

    private final TransitSchedule schedule;
    private final ZonalRegistry zonalRegistry;
    private final NetworkOfDistances networkOfDistances;
    private final Map<String, Set<String>> networkZones = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sameZones = new ConcurrentHashMap<>();

    @Inject
	public SwissPtRoutePredictor(TransitSchedule schedule, ZonalRegistry zonalRegistry, NetworkOfDistances sbbNetwork) {
		this.schedule      = schedule;
        this.zonalRegistry = zonalRegistry;
        this.networkOfDistances = sbbNetwork;
	}

    @SuppressWarnings("null")
    private Map<String, List<Zone>> cleanZoneInfo(Map<String, List<Zone>> zonesByStop) {
        List<String> stopIds = new ArrayList<>(zonesByStop.keySet());

        if (stopIds.isEmpty()) return new HashMap<>();

        // STEP 1: For each stop, deduplicate zones and group by authority
        Map<String, Map<Authority, List<Zone>>> zonesByStopAndAuth = new HashMap<>();

        for (String stopId : stopIds) {
            List<Zone> zonesAtStop = zonesByStop.get(stopId);

            // Dedup zones. Zone ids are only unique within an authority (e.g. zone "110" can exist
            // for several authorities), so the key must contain both.
            Map<String, Zone> uniqueZones = zonesAtStop.stream()
            .collect(Collectors.toMap(
                z -> z.getAuthority().getId() + ":" + z.getZoneId(),
                z -> z,
                (e1, e2) -> e1
            ));

            // Group by authority
            Map<Authority, List<Zone>> byAuth = uniqueZones.values().stream()
                .collect(Collectors.groupingBy(Zone::getAuthority));
            zonesByStopAndAuth.put(stopId, byAuth);
        }

        // STEP 2: Find common authorities across ALL stops, then pick highest priority among them
        Set<Authority> authoritiesAtEveryStop = zonesByStopAndAuth.values().stream()
            .map(Map::keySet)
            .reduce((a, b) -> {
                Set<Authority> intersection = new HashSet<>(a);
                intersection.retainAll(b);
                return intersection;
            })
            .orElse(new HashSet<>());

        if (authoritiesAtEveryStop.isEmpty()) {
            //System.out.println("WARNING: No common authority found across all stops!");
            return new HashMap<>();
        }

        // Keep every network shared by all the stops, whatever its priority: SwissPtVariables.getPricingStrategy
        // chooses between them once it knows the other legs (ride_zones in eqasim-switzerland, pricing.py). An S-Bahn
        // Zürich HB - Altstetten (ZVV, ZPassAwelle, SBB) and a train Altstetten - Baden (ZPassAwelle, SBB) are then
        // one ZPassAwelle ticket rather than a ZVV and a ZPassAwelle ticket.
        Set<Authority> selectedAuthorities = authoritiesAtEveryStop;

        // STEP 3: Collect zones for selected authorities (globally deduplicated)
        Map<String, Set<String>> seenZonesByAuth = new HashMap<>();
        Map<String, List<Zone>> filtered = new HashMap<>();

        for (String stopId : stopIds) {
            Map<Authority, List<Zone>> authsAtStop = zonesByStopAndAuth.get(stopId);

            for (Authority auth : selectedAuthorities) {
                List<Zone> zones = authsAtStop.getOrDefault(auth, List.of());

                for (Zone zone : zones) {
                    String authId = auth.getId();
                    String zoneId = zone.getZoneId();
                    if (seenZonesByAuth.computeIfAbsent(authId, k -> new HashSet<>()).add(zoneId)) {
                        filtered.computeIfAbsent(authId, k -> new ArrayList<>()).add(zone);
                    }
                }
            }
        }

        return filtered;
    }


    private Stream<Zone> getZones(ZonalRegistry registry, String stopId) {
        // Direct match
        Stream<Zone> direct = registry.getZones(stopId).stream();

        // Simplified match
        String simplifiedId = simplifyStopId(stopId);
        Stream<Zone> simplified = registry.getZones(simplifiedId).stream();

        // Merge both streams
        return Stream.concat(direct, simplified).distinct();
    }


    /**
     * The station of a stop id, the key of the SBB distances and of the station rows of gtfs_zones.csv:
     * the part before the first ':' or '.' (8503000:0:3 -> 8503000). A SLOID (the Swiss stop ids since
     * 2026, ch:1:sloid:3000:2:3) gives its DiDok number: the SLOID number with the country code 85 when it
     * has at most 5 digits (-> 8503000), else the number itself (ch:1:sloid:1100008 -> 1100008). The same
     * rule as station_id in eqasim-switzerland (utils/gtfs_router/pricing.py).
     */
    public static String simplifyStopId(String stopId) {
        Matcher sloid = SLOID.matcher(stopId);
        if (sloid.find()) {
            String number = sloid.group(1);
            return number.length() > 5 ? number : "85" + "0".repeat(5 - number.length()) + number;
        }
        return stopId.contains(":") ? ID_SEPARATOR.split(stopId, 2)[0] : stopId;
    }

    private static String stripLinkSuffix(String stopId) {
        return LINK_SUFFIX.matcher(stopId).replaceAll("");
    }

    /**
     * SBB tariff distance in km, or -1 if the leg is not priced by SBB. If the SBB distance table has
     * no entry for the stop pair, the routed network distance is used instead (with a warning), so
     * that the -1 marker never reaches the SBB price calculator, which sums the distances of all legs.
     */
    private double computeSBBDistance(Map<String, List<Zone>> zones, String fromId, String toId,
            double networkDistance_m) {
        if (!zones.containsKey("SBB")) {
            return -1.0;
        }

        double distance = networkOfDistances.getDistance(simplifyStopId(fromId), simplifyStopId(toId));

        if (distance >= 0.0) {
            return distance;
        }

        double fallback_km = Double.isFinite(networkDistance_m) && networkDistance_m > 0.0 ? networkDistance_m * 1e-3 : 0.0;

        if (warnedMissingSbbDistance.compareAndSet(false, true)) {
            LOG.warn("No SBB distance between {} and {}; using the network distance ({} km) instead."
                    + " Further occurrences are not logged.", fromId, toId, fallback_km);
        }

        return fallback_km;
    }

    /**
     * Stops from boarding to alighting, in driving direction. For routes serving a stop twice (loops),
     * this is the shortest part of the route from the access to the egress stop: the first egress
     * after the first access, then the last access before that egress. If the stops cannot be found
     * in that order, only the access and egress stops are used.
     */
    static List<TransitStopFacility> getVisitedStops(TransitRoute transitRoute,
            TransitStopFacility accessStop, TransitStopFacility egressStop) {
        List<TransitStopFacility> routeStops = transitRoute.getStops().stream()
            .map(x -> x.getStopFacility())
            .collect(Collectors.toList());

        int start = routeStops.indexOf(accessStop);
        int end = -1;

        if (start >= 0) {
            for (int i = start + 1; i < routeStops.size(); i++) {
                if (routeStops.get(i).equals(egressStop)) {
                    end = i;
                    break;
                }
            }
        }

        if (end >= 0) {
            start = routeStops.subList(0, end).lastIndexOf(accessStop);
        }

        if (start >= 0 && end >= 0) {
            return routeStops.subList(start, end + 1);
        }

        if (warnedStopsNotOnRoute.compareAndSet(false, true)) {
            LOG.warn("Stops {} -> {} not found in this order on transit route {}; using only these two stops"
                    + " for the zones. Further occurrences are not logged.", accessStop.getId(),
                    egressStop.getId(), transitRoute.getId());
        }

        return List.of(accessStop, egressStop);
    }


    public SwissPtVariables predictPtVariables(List<? extends PlanElement> elements){
        SwissPtVariables tripDescription = new SwissPtVariables();

        if (elements == null || elements.isEmpty()){
            return tripDescription;
        }

        for (PlanElement element : elements) {
            if (!(element instanceof Leg leg)) {
                continue;
            }

            if (!("pt".equals(leg.getMode()) || leg.getRoute() instanceof TransitPassengerRoute)) {
                continue;
            }

            Route route = leg.getRoute();

            if (!(route instanceof TransitPassengerRoute ptRoute)) {
                if (warnedNoTransitRoute.compareAndSet(false, true)) {
                    LOG.warn("PT leg has no TransitPassengerRoute (route is {}); it is not priced."
                            + " Further occurrences are not logged.",
                            route == null ? "null" : route.getClass().getSimpleName());
                }
                continue;
            }

            double departureTime = leg.getDepartureTime().seconds();
            double arrivalTime   = departureTime + leg.getTravelTime().seconds();

            TransitLine transitLine   = schedule.getTransitLines().get(ptRoute.getLineId());
            TransitRoute transitRoute = transitLine.getRoutes().get(ptRoute.getRouteId());

            TransitStopFacility accessStop = schedule.getFacilities().get(ptRoute.getAccessStopId());
            TransitStopFacility egressStop = schedule.getFacilities().get(ptRoute.getEgressStopId());

            String accessStopId = stripLinkSuffix(ptRoute.getAccessStopId().toString());
            String egressStopId = stripLinkSuffix(ptRoute.getEgressStopId().toString());

            // The visited stops include the access and egress stops, in driving direction
            List<TransitStopFacility> visitedStops = getVisitedStops(transitRoute, accessStop, egressStop);
            List<String> stopIds = new ArrayList<>(visitedStops.size());
            List<Collection<Zone>> stopZones = new ArrayList<>(visitedStops.size());
            Map<String, List<Zone>> stopsAndZones = new LinkedHashMap<>();

            for (TransitStopFacility stop : visitedStops) {
                String stopId = stripLinkSuffix(stop.getId().toString());
                List<Zone> zonesAtStop = stopsAndZones.computeIfAbsent(stopId, id -> getZones(zonalRegistry, id).collect(Collectors.toList()));
                stopIds.add(stopId);
                stopZones.add(zonesAtStop);
            }

            Map<String, List<Zone>> zones = cleanZoneInfo(stopsAndZones);

            double networkDistance = route.getDistance();
            double sbbDistance = computeSBBDistance(zones, accessStopId, egressStopId, networkDistance);

            SwissPtLegVariables legVariables = new SwissPtLegVariables(zones, departureTime, arrivalTime, networkDistance, sbbDistance, accessStopId, egressStopId, accessStop.getName(), egressStop.getName(),
                    stopIds, stopZones, stopDistances(visitedStops, networkDistance));
            legVariables.mode = transitRoute.getTransportMode() == null ? "" : transitRoute.getTransportMode();
            tripDescription.addStage(legVariables);
        }

        return tripDescription;
    }

    /**
     * The distance from the first stop to each stop, in metres: the straight lines between the stops, scaled so that
     * the last one is the distance of the route (the stops evenly spread if they have no length between them).
     */
    static double[] stopDistances(List<TransitStopFacility> stops, double networkDistance_m) {
        double[] distances = new double[stops.size()];
        for (int i = 1; i < stops.size(); i++) {
            distances[i] = distances[i - 1] + CoordUtils.calcEuclideanDistance(stops.get(i - 1).getCoord(), stops.get(i).getCoord());
        }

        int last = stops.size() - 1;
        double total = Double.isFinite(networkDistance_m) && networkDistance_m > 0 ? networkDistance_m : 0.0;
        for (int i = 1; i <= last; i++) {
            distances[i] = distances[last] > 0 ? total * distances[i] / distances[last] : total * i / last;
        }
        return distances;
    }

    // ------------------------------------------------------------------ regional subscriptions

    /**
     * The legs of a trip the traveller still has to pay with a regional subscription (the whole trip without one): a
     * leg the subscription covers is left out, and a leg that leaves its zones is replaced by the part to pay
     * (uncoveredPart).
     */
    public SwissPtVariables uncovered(SwissPtVariables trip, RegionalSubscription subscription) {
        if (subscription == null || subscription.isEmpty()) {
            return trip;
        }

        SwissPtVariables result = new SwissPtVariables();
        for (SwissPtLegVariables part : uncoveredLegs(trip, subscription)) {
            if (part != null) {
                result.addStage(part);
            }
        }
        return result;
    }

    /**
     * The part of each leg of a trip still to pay with a regional subscription (uncoveredPart), in the order of the
     * legs: null for a leg it covers, the leg itself without a subscription.
     */
    public List<SwissPtLegVariables> uncoveredLegs(SwissPtVariables trip, RegionalSubscription subscription) {
        List<SwissPtLegVariables> parts = new ArrayList<>(trip.legVariables.size());
        for (SwissPtLegVariables leg : trip.legVariables) {
            parts.add(subscription == null || subscription.isEmpty() ? leg : uncoveredPart(leg, subscription));
        }
        return parts;
    }

    /**
     * The part of a leg still to pay with a regional subscription, or null if it covers the leg (uncovered_ride in
     * eqasim-switzerland, utils/gtfs_router/pricing.py). A stop is covered if one of its zones is in the subscription.
     *
     * If all the stops are covered, nothing is paid. If the leg leaves the zones of the subscription once, it is paid
     * from the last covered stop before it leaves to the first covered stop after, without the covered zones: an
     * extension ticket. A leg that leaves the zones and comes back into them is paid in full, but for the covered
     * zones. A leg whose stops are unknown is paid in full.
     */
    public SwissPtLegVariables uncoveredPart(SwissPtLegVariables leg, RegionalSubscription subscription) {
        int n = leg.stopIds.size();
        if (subscription == null || subscription.isEmpty() || n == 0) {
            return leg;
        }

        boolean[] covered = new boolean[n];
        int first = -1;
        int last = -1;
        for (int i = 0; i < n; i++) {
            covered[i] = isCovered(leg.stopZones.get(i), subscription);
            if (!covered[i]) {
                if (first < 0) first = i;
                last = i;
            }
        }

        if (first < 0) {
            return null;
        }

        boolean comesBack = false;
        for (int i = first; i <= last; i++) {
            comesBack |= covered[i];
        }

        int begin = comesBack ? 0 : Math.max(first - 1, 0);
        int end = comesBack ? n - 1 : Math.min(last + 1, n - 1);

        Map<String, List<Zone>> stopsAndZones = new LinkedHashMap<>();
        for (int i = begin; i <= end; i++) {
            stopsAndZones.putIfAbsent(leg.stopIds.get(i), new ArrayList<>(leg.stopZones.get(i)));
        }
        Map<String, List<Zone>> partZones = cleanZoneInfo(stopsAndZones);

        Map<String, List<Zone>> unpaid = new LinkedHashMap<>();
        for (Map.Entry<String, List<Zone>> entry : partZones.entrySet()) {
            List<Zone> zones = entry.getValue().stream()
                .filter(zone -> !isZoneCovered(entry.getKey(), zone.getZoneId(), subscription))
                .collect(Collectors.toList());
            if (!zones.isEmpty()) {
                unpaid.put(entry.getKey(), zones);
            }
        }

        if (!partZones.isEmpty() && unpaid.isEmpty()) {
            return null;
        }

        String fromId = leg.stopIds.get(begin);
        String toId = leg.stopIds.get(end);
        double distance_m = leg.stopDistances_m[end] - leg.stopDistances_m[begin];
        double sbbDistance = computeSBBDistance(unpaid, fromId, toId, distance_m);
        double[] distances = new double[end - begin + 1];
        for (int i = begin; i <= end; i++) {
            distances[i - begin] = leg.stopDistances_m[i] - leg.stopDistances_m[begin];
        }

        SwissPtLegVariables part = new SwissPtLegVariables(unpaid, leg.departureTime, leg.arrivalTime, distance_m, sbbDistance,
                fromId, toId, begin == 0 ? leg.fromNodeName : fromId, end == n - 1 ? leg.toNodeName : toId,
                leg.stopIds.subList(begin, end + 1), leg.stopZones.subList(begin, end + 1), distances);
        part.mode = leg.mode;
        return part;
    }

    /** Whether a regional subscription is valid at a stop: one of its zones is in it (covering in pricing.py). */
    static boolean isCovered(Collection<Zone> stopZones, RegionalSubscription subscription) {
        for (Zone zone : stopZones) {
            if (subscription.covers(zone.getAuthority().getId(), zone.getZoneId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a zone of a network is in the subscription (zone_covered in pricing.py). A Z-Pass (a pass across two
     * networks) and the networks it joins share their zone numbers (Zürich HB: ZVV 110 and ZPassAwelle 110), so a zone
     * can be covered by a subscription of another of them: only if the two are the same zone, at a same stop
     * (ZonalRegistry.sameZone), not for another network that happens to use the same number.
     */
    boolean isZoneCovered(String network, String zone, RegionalSubscription subscription) {
        for (Map.Entry<String, Set<String>> entry : subscription.zones().entrySet()) {
            String other = entry.getKey();
            Set<String> zones = entry.getValue() == null ? networkZones(other) : entry.getValue();
            if (!zones.contains(zone)) {
                continue;
            }
            if (other.equals(network)) {
                return true;
            }
            if ((other.contains("ZPass") || network.contains("ZPass")) && sameZone(network, other, zone)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameZone(String network, String other, String zone) {
        return sameZones.computeIfAbsent(network + "|" + other + "|" + zone,
                key -> zonalRegistry.sameZone(network, other, zone));
    }

    private Set<String> networkZones(String network) {
        return networkZones.computeIfAbsent(network, zonalRegistry::getZoneIds);
    }

    @Override
    protected SwissPtVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        return predictPtVariables(elements);
    }
    
}
