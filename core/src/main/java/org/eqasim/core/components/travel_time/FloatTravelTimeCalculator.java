/* Adapted from MATSim 2026.0-2025w33 TravelTimeCalculator/TravelTimeDataArray.
 * Original authors: dgrether, mrieser and MATSim contributors.
 * Distributed under GNU GPL v2 or later, without warranty; see /LICENSE.
 */
package org.eqasim.core.components.travel_time;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/** Event collector with shared float averages and temporary observation counts.
 * Events/reset update data during the mobsim. After all events have been processed,
 * prepareForRouting consolidates rows and releases counts. Routing queries never
 * create rows, cache free-flow values, consolidate data, or modify the tables.
 * As with MATSim's collector, event delivery to this handler must be serialized.
 */
public final class FloatTravelTimeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        VehicleArrivesAtFacilityEventHandler, VehicleAbortsEventHandler {
    private final Network network;
    private final double binSize;
    private final int bins;
    private final boolean calculateLinks, calculateTurns, filterModes, interpolate;
    private final Set<String> analyzedModes;
    private final double simulationEndTime;
    private final IdMap<Link, Row> links = new IdMap<>(Link.class);
    private final Map<Tuple<Id<Link>, Id<Link>>, Row> turns = new HashMap<>();
    private final Map<Id<Vehicle>, LinkEnterEvent> enters = new HashMap<>();
    private final Set<Id<Vehicle>> ignoredVehicles = new HashSet<>();
    private final TravelTime linkTravelTimes = this::linkTravelTime;
    private final LinkToLinkTravelTime turnTravelTimes = this::turnTravelTime;
    private volatile boolean prepared = true;

    public FloatTravelTimeCalculator(Network network, TravelTimeCalculatorConfigGroup config) {
        this(network, config, config.isFilterModes(), config.getAnalyzedModes(), Double.POSITIVE_INFINITY);
    }

    FloatTravelTimeCalculator(Network network, TravelTimeCalculatorConfigGroup config,
            boolean filterModes, Set<String> analyzedModes, double simulationEndTime) {
        this.network = network;
        binSize = config.getTraveltimeBinSize();
        if (!(binSize > 0) || !Double.isFinite(binSize) || config.getMaxTime() < 0) {
            throw new IllegalArgumentException("Travel-time bin size must be finite/positive and maxTime nonnegative");
        }
        bins = TimeBinUtils.getTimeBinCount(config.getMaxTime(), binSize);
        calculateLinks = config.isCalculateLinkTravelTimes();
        calculateTurns = config.isCalculateLinkToLinkTravelTimes();
        this.filterModes = filterModes;
        this.analyzedModes = Set.copyOf(analyzedModes);
        this.simulationEndTime = simulationEndTime;
        interpolate = switch (config.getTravelTimeGetterType()) {
            case "average" -> false;
            case "linearinterpolation" -> true;
            default -> throw new IllegalArgumentException("Unknown travel-time getter: " + config.getTravelTimeGetterType());
        };
    }

    public TravelTime getLinkTravelTimes() { return linkTravelTimes; }
    public LinkToLinkTravelTime getLinkToLinkTravelTimes() { return turnTravelTimes; }

    private Row row(Id<Link> id) {
        Link link = network.getLinks().get(id);
        if (link == null) throw new IllegalArgumentException("Unknown link: " + id);
        return new Row(link, bins);
    }

    @Override public void handleEvent(LinkEnterEvent event) {
        if (filterModes && ignoredVehicles.contains(event.getVehicleId())) return;
        LinkEnterEvent previous = enters.put(event.getVehicleId(), event);
        if (previous != null && calculateTurns) {
            var key = new Tuple<>(previous.getLinkId(), event.getLinkId());
            Row row = turns.computeIfAbsent(key, k -> row(k.getFirst()));
            row.add(bin(previous.getTime()), event.getTime() - previous.getTime());
        }
        prepared = false;
    }

    @Override public void handleEvent(LinkLeaveEvent event) {
        if (!calculateLinks) return;
        LinkEnterEvent previous = enters.get(event.getVehicleId());
        if (previous == null) return;
        Row row = links.get(event.getLinkId());
        if (row == null) {
            row = row(event.getLinkId());
            links.put(event.getLinkId(), row);
        }
        row.add(bin(previous.getTime()), event.getTime() - previous.getTime());
        prepared = false;
    }

    @Override public void handleEvent(VehicleEntersTrafficEvent event) {
        if (filterModes && !analyzedModes.contains(event.getNetworkMode())) ignoredVehicles.add(event.getVehicleId());
    }

    @Override public void handleEvent(VehicleLeavesTrafficEvent event) {
        enters.remove(event.getVehicleId());
        ignoredVehicles.remove(event.getVehicleId());
    }

    @Override public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        enters.remove(event.getVehicleId());
    }

    @Override public void handleEvent(VehicleAbortsEvent event) {
        LinkEnterEvent previous = enters.remove(event.getVehicleId());
        if (previous != null) {
            Row row = links.get(previous.getLinkId());
            if (calculateLinks && row == null) {
                row = row(previous.getLinkId());
                links.put(previous.getLinkId(), row);
            }
            if (row != null) row.dirty = true;
            if (calculateTurns && event.getTime() < simulationEndTime) {
                throw new IllegalStateException("Stuck vehicles with link-to-link travel times are unsupported by MATSim");
            }
        }
        ignoredVehicles.remove(event.getVehicleId());
        prepared = false;
    }

    @Override public void reset(int iteration) {
        links.values().forEach(Row::reset);
        turns.values().forEach(Row::reset);
        enters.clear();
        ignoredVehicles.clear();
        prepared = false;
    }

    /** Call once after event processing ends, before launching routing workers. */
    public void prepareForRouting() {
        links.values().forEach(r -> r.prepare(binSize));
        turns.values().forEach(r -> r.prepare(binSize));
        enters.clear();
        ignoredVehicles.clear();
        prepared = true;
    }

    private int bin(double time) { return TimeBinUtils.getTimeBinIndex(time, binSize, bins); }

    private double linkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        if (!calculateLinks) throw new IllegalStateException("Link travel-time calculation is disabled");
        return Math.max(vehicleMinimum(link, vehicle), read(links.get(link.getId()), link, time));
    }

    private double turnTravelTime(Link from, Link to, double time, Person person, Vehicle vehicle) {
        if (!calculateTurns) throw new IllegalStateException("Link-to-link travel-time calculation is disabled");
        return Math.max(vehicleMinimum(from, vehicle), read(turns.get(new Tuple<>(from.getId(), to.getId())), from, time));
    }

    private static double vehicleMinimum(Link link, Vehicle vehicle) {
        return vehicle == null || vehicle.getType() == null ? 0 : link.getLength() / vehicle.getType().getMaximumVelocity();
    }

    private double read(Row row, Link link, double time) {
        int slot = bin(time);
        if (!interpolate || time <= binSize / 2 || time >= bins * binSize - binSize / 2) {
            return readBin(row, link, slot, time);
        }
        int first = slot * binSize + binSize / 2 > time ? slot - 1 : slot;
        double a = readBin(row, link, first, time), b = readBin(row, link, first + 1, time);
        return a + (b - a) * (time - (first * binSize + binSize / 2)) / binSize;
    }

    private static double readBin(Row row, Link link, int bin, double time) {
        float value = row == null ? -1 : row.averages[bin];
        return value >= 0 ? value : link.getLength() / link.getFreespeed(time);
    }

    public StorageStatistics getStorageStatistics() {
        long values = 4L * bins * (links.size() + turns.size());
        long counts = links.values().stream().filter(r -> r.counts != null).count()
                + turns.values().stream().filter(r -> r.counts != null).count();
        return new StorageStatistics(links.size(), turns.size(), bins, values, 4L * bins * counts, prepared);
    }

    public record StorageStatistics(int linkRows, int turnRows, int bins, long valueBytes,
            long observationCountBytes, boolean preparedForRouting) { }

    private static final class Row {
        final Link link;
        final float[] averages;
        int[] counts;
        boolean dirty;

        Row(Link link, int bins) {
            this.link = link;
            averages = new float[bins];
            reset();
        }

        void reset() {
            Arrays.fill(averages, -1);
            counts = null;
            dirty = false;
        }

        void add(int bin, double time) {
            if (counts == null) counts = new int[averages.length];
            int count = counts[bin];
            if (count == Integer.MAX_VALUE) throw new IllegalStateException("Too many travel-time observations in one bin");
            double sum = (count == 0 ? 0 : (double) averages[bin] * count) + time;
            averages[bin] = (float) (sum / (count + 1));
            counts[bin] = count + 1;
            dirty = true;
        }

        void prepare(double binSize) {
            if (dirty) {
                double previous = 0;
                for (int bin = 0; bin < averages.length; bin++) {
                    double value = averages[bin] >= 0 ? averages[bin] : link.getLength() / link.getFreespeed(bin * binSize);
                    double minimum = bin == 0 ? 0 : previous - binSize;
                    float stored = (float) Math.max(value, minimum);
                    if (stored < minimum) stored = Math.nextUp(stored); // preserve congestion spillover after rounding
                    averages[bin] = stored;
                    previous = stored;
                }
            }
            dirty = false;
            counts = null;
        }
    }
}
