package org.eqasim.core.components.network_calibration.Processors;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;

import java.io.BufferedWriter;
import java.util.*;

import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

public class FlowProcessor {

    private final static Logger logger = LogManager.getLogger(FlowProcessor.class);
    private final Network network;
    private final LinkFlowCounter linkFlowCounter;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final CountsProcessor countsProcessor;

    private final Map<PenaltyGroupKey, Double> flowPerGroup = new HashMap<>();
    private final Map<PenaltyGroupKey, Integer> linksPerGroup = new HashMap<>();
    private final Map<PenaltyGroupKey, FloatArrayList> simulatedFlows = new HashMap<>();
    private final Map<PenaltyGroupKey, FloatArrayList> observedCounts = new HashMap<>();
    private final Map<PenaltyGroupKey, FloatArrayList> observationWeights = new HashMap<>();
    private final double totalNumberOfHours;
    private final double sampleSize;

    public FlowProcessor(Network network, LinkFlowCounter linkFlowCounter, FlowBinManager flowBinManager,
                         CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy,
                         double sampleSize) {
        this.network = network;
        this.linkFlowCounter = linkFlowCounter;
        this.outputHierarchy = outputHierarchy;
        this.countsProcessor = countsProcessor;
        this.sampleSize = sampleSize;
        this.totalNumberOfHours = flowBinManager.getTotalTime_h();
    }

    public void updateAndSaveCounts(IterationEndsEvent iterationEndsEvent) {
        update();
        saveCounts(iterationEndsEvent.getIteration());
    }

    private void update() {
        // aggregate the counts by link category
        for (Id<Link> linkId : network.getLinks().keySet()) {
            if (!countsProcessor.contains(linkId)) {
                continue; // skip links not in the counts processor
            }

            PenaltyGroupKey groupKey = countsProcessor.getLinkGroup(linkId);
            if (groupKey == null) {
                continue; // skip links with unknown category
            }

            // sum the flow of the day (normalized, by hour, by lane)
            double totalFlow = getTotalLinkFlow(linkId);

            // Every observed link must contribute to the group average, including
            // links with zero simulated flow. Otherwise simulated and observed
            // group averages would use different denominators.
            flowPerGroup.put(groupKey, flowPerGroup.getOrDefault(groupKey, 0.0) + totalFlow);
            linksPerGroup.put(groupKey, linksPerGroup.getOrDefault(groupKey, 0) + 1);

            double linkCounts = countsProcessor.getLinkCounts(linkId);
            if (linkCounts > 0.0) {
                double fullSampleFlow = totalFlow / sampleSize;
                double weight = countsProcessor.getWeight(linkId);
                if (!Double.isFinite(weight) || weight <= 0.0) {
                    weight = 1.0;
                }

                simulatedFlows.computeIfAbsent(groupKey, k -> new FloatArrayList(32)).add((float) fullSampleFlow);
                observedCounts.computeIfAbsent(groupKey, k -> new FloatArrayList(32)).add((float) linkCounts);
                observationWeights.computeIfAbsent(groupKey, k -> new FloatArrayList(32)).add((float) weight);
            }
        }

        // get average flow per link group
        for (PenaltyGroupKey key : flowPerGroup.keySet()) {
            double totalFlow = flowPerGroup.get(key);
            int numLinks = linksPerGroup.get(key);
            if (numLinks == 0) {
                continue;
            }
            double avgFlow = (totalFlow / numLinks);
            flowPerGroup.put(key, avgFlow);
        }
    }

    public double getTotalLinkFlow(Id<Link> linkId) {
        double totalFlow = linkFlowCounter.getDailyCounts(linkId);
        if (totalFlow>1) {
            totalFlow = totalFlow / Math.max(1.0, totalNumberOfHours); // normalize by number of hours
            totalFlow = totalFlow / Math.max(network.getLinks().get(linkId).getNumberOfLanes(),1.0); // normalize by number of lanes
            return totalFlow;
        }
        return 0.0;
    }

    /**
     * Returns the change in the reported link flow caused by one additional
     * vehicle passage in the simulated sample.
     *
     * <p>{@link #getTotalLinkFlow(Id)} reports average hourly, per-lane flow,
     * while the demand calibrator manipulates whole daily passages. Keeping this
     * conversion here ensures that incremental calibration updates use exactly
     * the same units as the measured simulation flow.</p>
     */
    public double getFlowContributionPerPassage(Id<Link> linkId) {
        Link link = network.getLinks().get(linkId);
        if (link == null) {
            return 0.0;
        }

        double hours = Math.max(1.0, totalNumberOfHours);
        double lanes = Math.max(1.0, link.getNumberOfLanes());
        return 1.0 / (hours * lanes);
    }

    public void resetCounts(int iteration) {
        flowPerGroup.clear();
        linksPerGroup.clear();
        simulatedFlows.clear();
        observedCounts.clear();
        observationWeights.clear();
        // linkFlowCounter.reset(iteration); // no need to call reset, it will be called anyway before mobsim
    }

    public double getFlowByGroup(PenaltyGroupKey key, double inputSampleSize) {
        // this method scale back the flow to 100% sample size
        return flowPerGroup.getOrDefault(key, 0.0) / inputSampleSize;
    }

    public double getFlowByGroup(PenaltyGroupKey key) {
        // this method scale back the flow to 100% sample size
        return flowPerGroup.getOrDefault(key, 0.0) / sampleSize;
    }

    private void saveCounts(int iteration) {
        String outputFile = outputHierarchy.getIterationFilename(iteration, "flow_by_link_category.csv");

        try (BufferedWriter writer = getBufferedWriter(outputFile)) {
            writer.write("linkCategory;isUrban;specialRegion;averageFlow(veh/h/lane)\n");
            for (PenaltyGroupKey key : flowPerGroup.keySet()) {
                writer.write(key.getLinkCategory() + ";" + key.isUrban() + ";" + key.getSpecialRegion() + ";" + flowPerGroup.get(key) + "\n");
            }
            logger.info("Saved flow by link category to {}", outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Error writing flow by link category to file: " + outputFile);
        }
    }

    /**
     * Computes a robust median-oriented group error.
     *
     * <p>Each link contributes {@code tanh(log((flow + epsilon) / (count + epsilon)) / h)}.
     * Large count outliers therefore have bounded influence, while small errors vary
     * continuously instead of producing the quantized sign imbalance used previously.
     * The score is shrunk towards zero according to the effective sample size so that
     * sparse groups update more cautiously.</p>
     */
    public RobustGroupError getRobustGroupError(PenaltyGroupKey key, double h, double epsilon,
                                                 double sampleSizeShrinkage) {
        FloatArrayList flows = simulatedFlows.get(key);
        FloatArrayList counts = observedCounts.get(key);
        FloatArrayList weights = observationWeights.get(key);

        if (flows == null || counts == null || weights == null || flows.isEmpty()) {
            return new RobustGroupError(0.0, 0.0, 0.0, 0);
        }
        return computeRobustGroupError(flows, counts, weights, h, epsilon, sampleSizeShrinkage);
    }

    static RobustGroupError computeRobustGroupError(FloatArrayList flows, FloatArrayList counts,
                                                     FloatArrayList weights, double h, double epsilon,
                                                     double sampleSizeShrinkage) {
        if (h <= 0.0 || epsilon <= 0.0 || sampleSizeShrinkage < 0.0) {
            throw new IllegalArgumentException("Robust error parameters must satisfy h > 0, epsilon > 0 and shrinkage >= 0.");
        }
        if (flows.size() != counts.size() || flows.size() != weights.size()) {
            throw new IllegalArgumentException("Flow, count and weight arrays must have the same size.");
        }

        double weightedScore = 0.0;
        double sumWeights = 0.0;
        double sumSquaredWeights = 0.0;

        for (int i = 0; i < flows.size(); i++) {
            double weight = weights.getFloat(i);
            double logRatio = Math.log((flows.getFloat(i) + epsilon) / (counts.getFloat(i) + epsilon));
            weightedScore += weight * Math.tanh(logRatio / h);
            sumWeights += weight;
            sumSquaredWeights += weight * weight;
        }

        if (sumWeights <= 0.0 || sumSquaredWeights <= 0.0) {
            return new RobustGroupError(0.0, 0.0, 0.0, flows.size());
        }

        double rawScore = weightedScore / sumWeights;
        double effectiveSampleSize = sumWeights * sumWeights / sumSquaredWeights;
        double reliability = effectiveSampleSize / (effectiveSampleSize + sampleSizeShrinkage);
        return new RobustGroupError(rawScore * reliability, rawScore, effectiveSampleSize, flows.size());
    }

    public record RobustGroupError(double score, double rawScore, double effectiveSampleSize,
                                   int observations) { }

}
