// Exports a compact scenario snapshot and retains only metadata needed to reconstruct MATSim
// routes.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.eqasim.core.components.raptor.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.transitSchedule.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class RustTimetableSnapshot {
    record Profile(TransitLine line, TransitRoute route, double[] distances) {}

    record Trip(Profile profile, double departure) {}

    final TransitStopFacility[] stops;
    final List<Trip> trips = new ArrayList<>();
    final Map<Id<TransitStopFacility>, Integer> indices = new HashMap<>();
    final QuadTree<Integer> spatial;
    final RaptorParameters parameters;
    final RaptorStaticConfig walking;
    final Scenario scenario;

    RustTimetableSnapshot(Scenario scenario) {
        this.scenario = scenario;
        parameters =
                EqasimRaptorUtils.createParameters(
                        scenario.getConfig(),
                        ConfigUtils.addOrGetModule(
                                scenario.getConfig(), EqasimRaptorConfigGroup.class),
                        scenario.getTransitSchedule(),
                        false);
        walking = RaptorUtils.createStaticConfig(scenario.getConfig());
        // Ignore unused facilities, just as SwissRailRaptor's stop index does.
        Map<Id<TransitStopFacility>, TransitStopFacility> served =
                new TreeMap<>(Comparator.comparing(Id::toString));
        for (var line : scenario.getTransitSchedule().getTransitLines().values())
            for (var route : line.getRoutes().values())
                if (route.getStops().size() >= 2 && !route.getDepartures().isEmpty())
                    for (var stop : route.getStops())
                        served.put(stop.getStopFacility().getId(), stop.getStopFacility());
        stops = served.values().toArray(TransitStopFacility[]::new);
        if (stops.length == 0)
            throw new IllegalArgumentException("Rust PT routing needs a nonempty served schedule");
        double minX = Double.POSITIVE_INFINITY, minY = minX, maxX = -minX, maxY = -minX;
        for (int i = 0; i < stops.length; i++) {
            indices.put(stops[i].getId(), i);
            Coord c = stops[i].getCoord();
            if (!Double.isFinite(c.getX()) || !Double.isFinite(c.getY()))
                throw new IllegalArgumentException("Invalid stop coordinates: " + stops[i].getId());
            minX = Math.min(minX, c.getX());
            minY = Math.min(minY, c.getY());
            maxX = Math.max(maxX, c.getX());
            maxY = Math.max(maxY, c.getY());
        }
        spatial = new QuadTree<>(minX - 1, minY - 1, maxX + 1, maxY + 1);
        for (int i = 0; i < stops.length; i++)
            spatial.put(stops[i].getCoord().getX(), stops[i].getCoord().getY(), i);
    }

    /** Snapshotting is streamed once, without a second Java copy of all departure calls. */
    void write(Path path) throws IOException {
        try (var out =
                new DataOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(path), 1024 * 1024))) {
            out.writeBytes("MMJNI001");
            out.writeInt(stops.length);
            for (var stop : stops) {
                out.writeDouble(stop.getCoord().getX());
                out.writeDouble(stop.getCoord().getY());
                // The core has a dedicated same-stop buffer; leaving it zero bypasses the
                // self-transfer edge.
                out.writeInt(
                        transferMillis(
                                scenario.getTransitSchedule()
                                        .getMinimalTransferTimes()
                                        .get(
                                                stop.getId(),
                                                stop.getId(),
                                                walking.getMinimalTransferTime())));
            }
            int count = 0;
            for (var line : scenario.getTransitSchedule().getTransitLines().values())
                for (var route : line.getRoutes().values())
                    if (route.getStops().size() >= 2)
                        count = Math.addExact(count, route.getDepartures().size());
            out.writeInt(count);
            for (var line : scenario.getTransitSchedule().getTransitLines().values()) {
                for (var route : line.getRoutes().values()) {
                    if (route.getStops().size() < 2 || route.getDepartures().isEmpty()) continue;
                    Profile profile = new Profile(line, route, distances(route));
                    for (var departure : route.getDepartures().values()) {
                        if (!departure.getChainedDepartures().isEmpty())
                            throw new IllegalArgumentException(
                                    "Rust routing does not yet support chained departures: "
                                            + route.getId());
                        trips.add(new Trip(profile, departure.getDepartureTime()));
                        byte[] mode = route.getTransportMode().getBytes(StandardCharsets.UTF_8);
                        out.writeInt(mode.length);
                        out.write(mode);
                        out.writeInt(route.getStops().size());
                        double previous = Double.NEGATIVE_INFINITY;
                        for (int i = 0; i < route.getStops().size(); i++) {
                            var stop = route.getStops().get(i);
                            if ((i > 0 && stop.getArrivalOffset().isUndefined())
                                    || (i < route.getStops().size() - 1
                                            && stop.getDepartureOffset().isUndefined()))
                                throw new IllegalArgumentException(
                                        "Missing used PT offset: line "
                                                + line.getId()
                                                + ", route "
                                                + route.getId()
                                                + ", stop "
                                                + i);
                            double arrival =
                                    stop.getArrivalOffset()
                                            .orElse(stop.getDepartureOffset().orElse(Double.NaN));
                            double leave = stop.getDepartureOffset().orElse(arrival);
                            // The terminal departure is never used. Some feeds write 0 for this
                            // unused offset.
                            if (i == route.getStops().size() - 1) leave = arrival;
                            if (arrival < previous
                                    || leave < arrival
                                    || !Double.isFinite(arrival)
                                    || !Double.isFinite(leave))
                                throw new IllegalArgumentException(
                                        "Invalid PT chronology: line "
                                                + line.getId()
                                                + ", route "
                                                + route.getId()
                                                + ", stop "
                                                + i
                                                + " (arrival="
                                                + arrival
                                                + ", departure="
                                                + leave
                                                + ")");
                            previous = leave;
                            out.writeInt(indices.get(stop.getStopFacility().getId()));
                            out.writeInt(
                                    (int)
                                            NativePtRouter.millis(
                                                    departure.getDepartureTime() + arrival));
                            out.writeInt(
                                    (int)
                                            NativePtRouter.millis(
                                                    departure.getDepartureTime() + leave));
                            out.writeByte(
                                    (stop.isAllowBoarding() ? 1 : 0)
                                            | (stop.isAllowAlighting() ? 2 : 0));
                        }
                    }
                }
            }
            writeTransfers(out);
        }
    }

    /** Physical stop pairs replace Raptor's much larger route-stop pair expansion. */
    private void writeTransfers(DataOutputStream out) throws IOException {
        Map<Long, Integer> edges = new TreeMap<>();
        var minimal = scenario.getTransitSchedule().getMinimalTransferTimes();
        for (int from = 0; from < stops.length; from++) {
            Coord origin = stops[from].getCoord();
            for (int to :
                    spatial.getDisk(
                            origin.getX(),
                            origin.getY(),
                            walking.getBeelineWalkConnectionDistance())) {
                if (from == to) continue;
                double time =
                        Math.max(
                                walking.getMinimalTransferTime(),
                                distance(stops[from].getCoord(), stops[to].getCoord())
                                        / walking.getBeelineWalkSpeed());
                time = minimal.get(stops[from].getId(), stops[to].getId(), time);
                edges.put(((long) from << 32) | to, transferMillis(time));
            }
        }
        var iterator = minimal.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            Integer from = indices.get(iterator.getFromStopId()),
                    to = indices.get(iterator.getToStopId());
            if (from != null && to != null && !from.equals(to))
                edges.put(((long) from << 32) | to, transferMillis(iterator.getSeconds()));
        }
        out.writeInt(edges.size());
        for (var edge : edges.entrySet()) {
            out.writeInt((int) (edge.getKey() >>> 32));
            out.writeInt((int) (long) edge.getKey());
            out.writeInt(edge.getValue());
        }
    }

    private static int transferMillis(double seconds) {
        return Math.toIntExact(NativePtRouter.millis(Math.ceil(seconds)));
    }

    static double distance(Coord a, Coord b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }

    /** Match Raptor's cumulative network distance without rescanning network links per query. */
    private double[] distances(TransitRoute route) {
        double[] result = new double[route.getStops().size()];
        var networkRoute = route.getRoute();
        if (networkRoute == null)
            throw new IllegalArgumentException("PT route has no network route: " + route.getId());
        List<Id<Link>> links = new ArrayList<>();
        links.add(networkRoute.getStartLinkId());
        links.addAll(networkRoute.getLinkIds());
        if (links.size() > 1
                || !Objects.equals(networkRoute.getStartLinkId(), networkRoute.getEndLinkId()))
            links.add(networkRoute.getEndLinkId());
        int current = 0;
        double distance = 0;
        for (int i = 0; i < result.length; i++) {
            Id<Link> target = route.getStops().get(i).getStopFacility().getLinkId();
            while (current < links.size() && !Objects.equals(links.get(current), target)) {
                if (++current < links.size()) {
                    Link link = scenario.getNetwork().getLinks().get(links.get(current));
                    if (link == null)
                        throw new IllegalArgumentException(
                                "PT link missing from network: " + links.get(current));
                    distance += link.getLength();
                }
            }
            if (current == links.size())
                throw new IllegalArgumentException(
                        "Stop link " + target + " missing from route " + route.getId());
            result[i] = distance;
        }
        return result;
    }

    String costsJson() throws IOException {
        Map<String, Object> costs = new LinkedHashMap<>();
        costs.put("walk_per_hour", -3600 * parameters.getMarginalUtilityOfTravelTime_utl_s("walk"));
        costs.put("wait_per_hour", -3600 * parameters.getMarginalUtilityOfWaitingPt_utl_s());
        Map<String, Double> modes = new TreeMap<>();
        for (var trip : trips)
            modes.put(
                    trip.profile.route.getTransportMode(),
                    -3600
                            * parameters.getMarginalUtilityOfTravelTime_utl_s(
                                    trip.profile.route.getTransportMode()));
        costs.put("modes_per_hour", modes);
        costs.put("transfer_base", parameters.getTransferPenaltyFixCostPerTransfer());
        costs.put("transfer_per_travel_hour", parameters.getTransferPenaltyPerTravelTimeHour());
        if (Double.isFinite(parameters.getTransferPenaltyMinimum()))
            costs.put("transfer_min", parameters.getTransferPenaltyMinimum());
        if (Double.isFinite(parameters.getTransferPenaltyMaximum()))
            costs.put("transfer_max", parameters.getTransferPenaltyMaximum());
        var config =
                ConfigUtils.addOrGetModule(scenario.getConfig(), SwissRailRaptorConfigGroup.class);
        costs.put(
                "mode_transfers",
                config.getModeToModeTransferPenaltyParameterSets().stream()
                        .map(
                                p ->
                                        Map.of(
                                                "from",
                                                p.fromMode,
                                                "to",
                                                p.toMode,
                                                "cost",
                                                p.transferPenalty))
                        .toList());
        return new ObjectMapper().writeValueAsString(costs);
    }
}
