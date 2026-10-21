package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.travel_time.function.BPRFunction;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;

/** Static departure-bin incremental assignment. Only the request coordinator mutates flows. */
final class MarouterAssignment {
    final VDFScope scope;
    final double increment;
    final long seed;
    final double truckPcu;
    private final double truckMaxSpeed;
    private final double sampleSize;
    private final double capacityFactor;
    private final double minimumSpeed;
    private final double capacityPeriod;
    private final BPRFunction bpr;
    private final TravelTime freeflow;
    private final Link[] links;
    private volatile Bin active;

    private static final class Bin {
        final double time;
        final double[] counts;
        final double[] times;
        final List<Integer> touched = new ArrayList<>();

        Bin(double time, int slots) {
            this.time = time;
            counts = new double[slots];
            times = new double[slots];
        }
    }

    MarouterAssignment(Network network, VDFConfigGroup config, double sampleSize, double increment,
            long seed, double truckPcu, double truckMaxSpeed) {
        this(network, config, sampleSize, increment, seed, truckPcu, truckMaxSpeed,
                new CalibratedFreeSpeedTravelTime(network));
    }

    MarouterAssignment(Network network, VDFConfigGroup config, double sampleSize, double increment,
            long seed, double truckPcu, double truckMaxSpeed, TravelTime freeflow) {
        positive(config.getInterval(), "VDF interval");
        positive(config.getMinimumSpeed(), "VDF minimumSpeed");
        positive(config.getCapacityFactor(), "VDF capacityFactor");
        positive(sampleSize, "MARouter sample size");
        positive(truckPcu, "MARouter truck PCU");
        positive(truckMaxSpeed, "Truck maximum speed");
        if (!Double.isFinite(config.getStartTime()) || !Double.isFinite(config.getEndTime())
                || config.getStartTime() < 0 || config.getEndTime() <= config.getStartTime()
                || Math.ceil((config.getEndTime() - config.getStartTime()) / config.getInterval()) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid VDF startTime/endTime/interval");
        }
        if (!Double.isFinite(config.getBprFactor()) || config.getBprFactor() < 0) {
            throw new IllegalArgumentException("VDF bpr:factor must be finite and non-negative");
        }
        positive(config.getBprExponent(), "VDF bpr:exponent");
        if (!Double.isFinite(increment) || increment <= 0 || increment > 1 || Math.ceil(1 / increment) > 10_000) {
            throw new IllegalArgumentException("marouter-increment must be in [0.0001, 1]");
        }
        scope = new VDFScope(config.getStartTime(), config.getEndTime(), config.getInterval());
        this.increment = increment;
        this.seed = seed;
        this.truckPcu = truckPcu;
        this.truckMaxSpeed = truckMaxSpeed;
        this.sampleSize = sampleSize;
        this.freeflow = freeflow;
        capacityFactor = config.getCapacityFactor();
        minimumSpeed = config.getMinimumSpeed();
        capacityPeriod = network.getCapacityPeriod();
        positive(capacityPeriod, "Network capacity period");
        bpr = new BPRFunction(config.getBprFactor(), config.getBprExponent());
        int slots = network.getLinks().keySet().stream().mapToInt(id -> id.index()).max().orElse(-1) + 1;
        links = new Link[slots];
        for (Link link : network.getLinks().values()) {
            if (!link.getAllowedModes().contains("car") && !link.getAllowedModes().contains("truck")) continue;
            if (!Double.isFinite(link.getLength()) || link.getLength() < 0) {
                throw new IllegalArgumentException("Invalid road link length: " + link.getId());
            }
            positive(link.getFreespeed(), "Road freespeed on " + link.getId());
            positive(link.getCapacity(), "Road capacity on " + link.getId());
            links[link.getId().index()] = link;
        }
    }

    int bin(double departureTime) {
        return scope.getIntervalIndex(departureTime);
    }

    void begin(int bin) {
        Bin state = new Bin(scope.getStartTime() + (bin + 0.5) * scope.getIntervalTime(), links.length);
        for (int i = 0; i < links.length; i++) {
            if (links[i] != null) state.times[i] = freeflow(links[i], state.time);
        }
        active = state;
    }

    void reset() {
        active = null;
    }

    TravelTime travelTime(boolean truck) {
        return (link, time, person, vehicle) -> {
            Bin state = active;
            double roadTime = state == null ? freeflow(link, time) : state.times[link.getId().index()];
            return truck ? Math.max(roadTime, link.getLength() / truckMaxSpeed) : roadTime;
        };
    }

    void add(int[] path, boolean truck) {
        Bin state = active;
        if (path == null) return; // Unreachable trips contribute no link flow.
        double pcu = truck ? truckPcu : 1;
        for (int index : path) {
            if (state.counts[index] == 0) state.touched.add(index);
            state.counts[index] += pcu;
        }
    }

    void update() {
        Bin state = active;
        for (int index : state.touched) {
            Link link = links[index];
            if (link.getLength() == 0) { state.times[index] = 0; continue; }
            // Counts are sampled demand: reduce interval capacity by the same sample fraction.
            // This is equivalent to VDFTravelTime's flow/sampleSize; apply sampling only once.
            double capacity = sampleSize * capacityFactor * scope.getIntervalTime() * link.getCapacity(state.time) / capacityPeriod;
            positive(capacity, "Interval capacity on " + link.getId());
            double time = bpr.calculateTravelTime(state.time, state.counts[index], capacity, link);
            // BPRFunction uses the raw network speed: replace its baseline with calibrated freeflow.
            time *= freeflow(link, state.time) / (link.getLength() / link.getFreespeed(state.time));
            // Same BPR/units as VDFTravelTime. Never make a slow link faster than freeflow.
            state.times[index] = Math.max(freeflow(link, state.time),
                    Math.max(1, Math.min(link.getLength() / minimumSpeed, time)));
        }
    }

    double finalTime(int[] path, boolean truck) {
        if (path == null) return Double.NaN;
        double time = 0;
        Bin state = active;
        for (int index : path) {
            double roadTime = state.times[index];
            time += truck ? Math.max(roadTime, links[index].getLength() / truckMaxSpeed) : roadTime;
        }
        return time;
    }

    private double freeflow(Link link, double time) {
        return freeflow.getLinkTravelTime(link, time, null, null);
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException(name + " must be positive and finite");
    }
}
