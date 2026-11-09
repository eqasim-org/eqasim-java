package org.eqasim.switzerland.ch_cmdp.utils.routing.multimodalRouter.cars;

import java.util.ArrayList;
import java.util.List;

import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.travel_time.function.BPRFunction;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;

/** Static departure-bin incremental assignment. Only the request coordinator mutates flows. */
public final class MarouterAssignment {
    public final VDFScope scope;
    public final double increment;
    public final long seed;
    public final double truckPcu;
    private final double truckMaxSpeed;
    private final double sampleSize;
    private final double capacityFactor;
    private final double minimumSpeed;
    private final double capacityPeriod;
    private final BPRFunction bpr;
    private final TravelTime freeflow;
    private final Link[] links;
    private volatile Bin active;
    private double[] previousFlows;
    private double[] previousTimes;
    private int completedIteration = 1;

    private static final class Bin {
        final double time;
        final double[] counts;
        final double[] times;
        final List<Integer> touched = new ArrayList<>();
        final boolean[] seen;

        Bin(double time, int slots) {
            this.time = time;
            seen = new boolean[slots];
            counts = new double[slots];
            times = new double[slots];
        }
    }

    public MarouterAssignment(Network network, VDFConfigGroup config, double sampleSize, double increment,
            long seed, double truckPcu, double truckMaxSpeed) {
        this(network, config, sampleSize, increment, seed, truckPcu, truckMaxSpeed,
                new CalibratedFreeSpeedTravelTime(network));
    }

    public MarouterAssignment(Network network, VDFConfigGroup config, double sampleSize, double increment,
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

    public int bin(double departureTime) {
        return scope.getIntervalIndex(departureTime);
    }

    public void begin(int bin) {
        Bin state = new Bin(scope.getStartTime() + (bin + 0.5) * scope.getIntervalTime(), links.length);
        for (int i = 0; i < links.length; i++) {
            if (links[i] != null) state.times[i] = freeflow(links[i], state.time);
        }
        active = state;
        previousFlows = null; previousTimes = state.times.clone(); completedIteration = 1;
    }

    /** Retain the previous travel-time snapshot while rebuilding this iteration's demand. */
    public void nextIteration() {
        if (previousFlows == null) previousFlows = active.counts.clone();
        else for (int i : active.touched)
            previousFlows[i] += (active.counts[i] - previousFlows[i]) / completedIteration;
        completedIteration++;
        previousTimes = active.times.clone();
        java.util.Arrays.fill(active.counts, 0);
        // Keep all previously used links in touched, including links abandoned by the new routes.
    }

    /**
     * Update after each increment. The unassigned fraction retains its previous flow;
     * assigned demand gradually replaces it. Successive averaging damps the replacement.
     * Counts always contain only this pass's freshly routed weighted PCU.
     */
    public void updateIncrement(int iteration, double assignedFraction) {
        if (iteration < 2 || !Double.isFinite(assignedFraction) || assignedFraction < 0 || assignedFraction > 1)
            throw new IllegalArgumentException("Invalid iteration or assigned fraction");
        update(iteration, assignedFraction);
    }

    public void finishIteration(int iteration) { updateIncrement(iteration, 1); }

    public String summary() {
        double flow = 0, change = 0, maxTime = 0;
        int used = 0;
        for (int i = 0; i < links.length; i++) if (links[i] != null && effectiveFlow(i, completedIteration, 1) > 0) {
            flow += effectiveFlow(i, completedIteration, 1); used++;
            maxTime = Math.max(maxTime, active.times[i]);
            if (previousTimes != null) change += Math.abs(active.times[i] - previousTimes[i]) / Math.max(1, previousTimes[i]);
        }
        return String.format(java.util.Locale.ROOT, "used links=%d, link-PCU=%.2f, mean relative time change=%.4f, max link time=%.1f s",
                used, flow, used == 0 ? 0 : change / used, maxTime);
    }

    public void reset() {
        active = null;
        previousFlows = null; previousTimes = null;
    }

    public TravelTime travelTime(boolean truck) {
        return (link, time, person, vehicle) -> {
            Bin state = active;
            double roadTime = state == null ? freeflow(link, time) : state.times[link.getId().index()];
            return truck ? Math.max(roadTime, link.getLength() / truckMaxSpeed) : roadTime;
        };
    }

    public void add(int[] path, boolean truck) {
        add(path, truck, 1);
    }

    public void add(int[] path, boolean truck, double weight) {
        if (!Double.isFinite(weight) || weight < 0 || !Double.isFinite(weight * (truck ? truckPcu : 1)))
            throw new IllegalArgumentException("MARouter weight * vehicle PCU must be finite and non-negative");
        Bin state = active;
        if (path == null || weight == 0) return; // Unreachable/zero-weight trips contribute no link flow.
        double pcu = weight * (truck ? truckPcu : 1);
        for (int index : path) {
            if (!state.seen[index]) { state.seen[index] = true; state.touched.add(index); }
            state.counts[index] += pcu;
        }
    }

    public void update() { update(1, 1); }

    private void update(int iteration, double assignedFraction) {
        Bin state = active;
        for (int index : state.touched) {
            Link link = links[index];
            if (link.getLength() == 0) { state.times[index] = 0; continue; }
            // Counts include trip weights; population sampling independently scales capacity.
            // This is equivalent to VDFTravelTime's flow/sampleSize; apply sampling only once.
            double capacity = sampleSize * capacityFactor * scope.getIntervalTime() * link.getCapacity(state.time) / capacityPeriod;
            positive(capacity, "Interval capacity on " + link.getId());
            double flow = effectiveFlow(index, iteration, assignedFraction);
            double time = bpr.calculateTravelTime(state.time, flow, capacity, link);
            // Same BPR/units as VDFTravelTime. Never make a slow link faster than freeflow.
            state.times[index] = Math.max(freeflow(link, state.time),
                    Math.max(1, Math.min(link.getLength() / minimumSpeed, time)));
        }
    }

    public double finalTime(int[] path, boolean truck) {
        if (path == null) return Double.NaN;
        double time = 0;
        Bin state = active;
        for (int index : path) {
            double roadTime = state.times[index];
            time += truck ? Math.max(roadTime, links[index].getLength() / truckMaxSpeed) : roadTime;
        }
        return time;
    }

    private double effectiveFlow(int index, int iteration, double assignedFraction) {
        return previousFlows == null ? active.counts[index]
                : previousFlows[index] + (active.counts[index] - assignedFraction * previousFlows[index]) / iteration;
    }

    private double freeflow(Link link, double time) {
        return freeflow.getLinkTravelTime(link, time, null, null);
    }

    private static void positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException(name + " must be positive and finite");
    }
}
