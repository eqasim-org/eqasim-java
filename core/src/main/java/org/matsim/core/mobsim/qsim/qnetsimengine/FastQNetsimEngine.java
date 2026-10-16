package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * Scheduling adaptation for MATSim 2026.0-2025w33. Native network, node, lane,
 * vehicle and capacity implementations are retained, with both phase barriers.
 */
public final class FastQNetsimEngine extends AbstractQNetsimEngine<FastQNetsimEngine.Runner> {
    private static final Logger log = LogManager.getLogger(FastQNetsimEngine.class);
    public static final String ENGINE_PROPERTY = "eqasim.qsim.engine";
    private final QSim sim;
    @Inject(optional = true) private FastQSimPartitionCache partitionCache;
    private final int blocksPerThread;
    private final boolean weighted;
    private final long minWorkPerThreadNanos;
    private QSimWorkScheduler scheduler;
    private Runner[] nodeOrder, linkOrder;
    private Runnable[] work;
    private boolean movingNodes, sample;
    private long steps, nodePhaseNanos, linkPhaseNanos, directPhases, parallelPhases;
    private static final Comparator<Runner> NODE_COST = Comparator.comparingDouble((Runner r) -> r.nodeCost).reversed();
    private static final Comparator<Runner> LINK_COST = Comparator.comparingDouble((Runner r) -> r.linkCost).reversed();

    @Inject public FastQNetsimEngine(QSim sim, QNetworkFactory factory, NetworkModeDepartureHandler departureHandler) {
        super(sim, factory, departureHandler);
        this.sim = sim;
        blocksPerThread = Integer.getInteger("eqasim.qsim.blocksPerThread", 4);
        if (blocksPerThread < 1 || blocksPerThread > 64) throw new IllegalArgumentException("blocksPerThread must be 1..64");
        String partitioning = System.getProperty("eqasim.qsim.partitioning", "weighted");
        if (!partitioning.equals("weighted") && !partitioning.equals("roundRobin")) {
            throw new IllegalArgumentException("eqasim.qsim.partitioning must be weighted or roundRobin");
        }
        weighted = partitioning.equals("weighted");
        minWorkPerThreadNanos = Long.getLong("eqasim.qsim.minWorkPerThreadNanos", 8_000L);
        if (minWorkPerThreadNanos < 0) throw new IllegalArgumentException("minWorkPerThreadNanos must be nonnegative");
    }

    @Override protected List<Runner> initQSimEngineRunners() {
        int blocks = Math.max(1, Math.min(qNetwork.getNetsimNodes().size(),
                numOfThreads == 1 ? 1 : Math.min(QSimWorkScheduler.MAX_TASKS, Math.multiplyExact(numOfThreads, blocksPerThread))));
        var runners = new ArrayList<Runner>(blocks);
        for (int i = 0; i < blocks; i++) runners.add(new Runner());
        return runners;
    }

    @Override protected void initMultiThreading() {
        // AbstractQNetsimEngine installs its private round-robin assignment first.
        // Reassign before any agent sources run: no active queues may exist yet.
        var runners = getQnetsimEngineRunner();
        for (var runner : runners) {
            if (runner.hasNodesToMove() || runner.hasLinksToMove()) {
                throw new IllegalStateException("Cannot repartition an active QSim network");
            }
        }
        Node[] nodes = qNetwork.getNetsimNodes().values().stream().map(QNodeI::getNode).toArray(Node[]::new);
        long start = System.nanoTime();
        if (weighted && nodes.length > 0) {
            // Standalone QSimBuilder users need not install the controller module.
            if (partitionCache == null) partitionCache = new FastQSimPartitionCache();
            int[] parts = partitionCache.get(nodes, sim.getScenario(), runners.size());
            for (int i = 0; i < nodes.length; i++) {
                Runner owner = runners.get(parts[i]);
                var node = qNetwork.getNetsimNodes().get(nodes[i].getId());
                if (!(node instanceof AbstractQNode qNode)) throw new IllegalArgumentException("Unsupported QNode: " + node.getClass());
                qNode.setNetElementActivationRegistry(owner);
                for (var link : nodes[i].getOutLinks().values()) {
                    ((AbstractQLink) qNetwork.getNetsimLink(link.getId())).setNetElementActivationRegistry(owner);
                }
            }
        }
        nodeOrder = runners.toArray(Runner[]::new);
        linkOrder = nodeOrder.clone();
        work = new Runnable[runners.size()];
        scheduler = new QSimWorkScheduler(Math.min(numOfThreads, runners.size()));
        log.info("Fast QSim: {} threads, {} blocks, {} partitioning; initialized in {} ms. Native node/link phases retained.",
                Math.min(numOfThreads, runners.size()), runners.size(), weighted ? "traffic-weighted spatial" : "round-robin",
                (System.nanoTime() - start) / 1_000_000);
    }

    @Override protected void run(double time) {
        for (var runner : nodeOrder) runner.setTime(time);
        sample = (steps & 15) == 0;
        if ((steps & 127) == 0) {
            Arrays.sort(nodeOrder, NODE_COST);
            Arrays.sort(linkOrder, LINK_COST);
        }
        movingNodes = true;
        long start = System.nanoTime();
        try { phase(nodeOrder); }
        catch (RuntimeException | Error error) { scheduler.close(); throw error; }
        nodePhaseNanos += System.nanoTime() - start;
        movingNodes = false;
        start = System.nanoTime();
        try { phase(linkOrder); }
        catch (RuntimeException | Error error) { scheduler.close(); throw error; }
        linkPhaseNanos += System.nanoTime() - start;
        steps++;
    }

    private void phase(Runner[] order) {
        int size = 0;
        double cost = 0;
        boolean unknown = false;
        for (var runner : order) {
            if (movingNodes ? runner.hasNodesToMove() : runner.hasLinksToMove()) {
                work[size++] = runner;
                double estimate = movingNodes ? runner.nodeCost : runner.linkCost;
                unknown |= estimate == 0;
                cost += estimate;
            }
        }
        int threads = numOfThreads;
        if (!unknown && minWorkPerThreadNanos > 0) {
            threads = Math.max(1, (int) Math.min(numOfThreads, Math.ceil(cost / minWorkPerThreadNanos)));
        }
        if (size > 1 && threads > 1) parallelPhases++; else directPhases++;
        scheduler.execute(work, size, sample, threads);
        Arrays.fill(work, 0, size, null);
    }

    @Override protected void finishMultiThreading() {
        if (scheduler != null) scheduler.close();
        double nodeWork = 0, linkWork = 0;
        for (var runner : getQnetsimEngineRunner()) {
            nodeWork += runner.sampledNodeNanos;
            linkWork += runner.sampledLinkNanos;
        }
        log.info("Fast QSim summary: steps={}, node phases={} s, link phases={} s, parallel phases={}, direct/empty phases={}, sampled node work={} ms, sampled link work={} ms (1/16 steps).",
                steps, nodePhaseNanos / 1e9, linkPhaseNanos / 1e9, parallelPhases, directPhases,
                nodeWork / 1e6, linkWork / 1e6);
        log.info("Fast QSim sampled parallel-phase worker occupancy: {}% (elapsed time processing blocks, including waits inside blocks; not CPU utilization).",
                100.0 * scheduler.sampledOccupancy());
    }

    final class Runner extends AbstractQNetsimEngineRunner implements Runnable {
        private double nodeCost, linkCost;
        private long sampledNodeNanos, sampledLinkNanos;

        Runner() { super(true); }

        @Override public void run() {
            long start = sample ? System.nanoTime() : 0;
            if (movingNodes) moveNodes(); else moveLinks();
            if (sample) {
                long elapsed = System.nanoTime() - start;
                if (movingNodes) {
                    nodeCost = nodeCost == 0 ? elapsed : 0.8 * nodeCost + 0.2 * elapsed;
                    sampledNodeNanos += elapsed;
                } else {
                    linkCost = linkCost == 0 ? elapsed : 0.8 * linkCost + 0.2 * elapsed;
                    sampledLinkNanos += elapsed;
                }
            }
        }

        @Override public void afterSim() { }
    }
}
