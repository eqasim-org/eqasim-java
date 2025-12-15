package org.eqasim.core.components.network_calibration.capacities_calibration;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapacitiesAdapter implements IterationEndsListener, IterationStartsListener {

    private final Network network;
    private final FlowProcessor flowsEstimator;
    private final double sampleSize;
    private final int updateInterval;
    private final int saveNetworkInterval;
    private final boolean correctCapacities;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final CountsProcessor countsProcessor;
    private final Map<Integer, Double> capacityPerCategory = new HashMap<>();
    private final double minSpeed; // km/h (used in capacity correction)
    private final double maxCapacity; // veh/h/lane (for the highest category, used to scale all capacities)
    private final double minCapacity; // veh/h/lane (minimum capacity allowed for any category)
    private final double beta;
    private final List<Integer> categoriesToCalibrate;
    private final double rampCapacityFactor;
    private final double trunkCapacityFactor;

    public CapacitiesAdapter(Network network, FlowProcessor flowsEstimator, CountsProcessor countsProcessor,
                             NetworkCalibrationConfigGroup config,
                             EqasimConfigGroup eqasimConfig, OutputDirectoryHierarchy outputHierarchy) {
        this.network = network;
        this.flowsEstimator = flowsEstimator;
        this.countsProcessor = countsProcessor;
        this.updateInterval = config.getUpdateInterval();
        this.saveNetworkInterval = config.getSaveNetworkInterval();
        this.sampleSize = eqasimConfig.getSampleSize();
        this.minSpeed = config.getMinSpeed();
        this.correctCapacities = config.getCorrectCapacities();
        this.maxCapacity = config.getMaxCapacity();
        this.minCapacity = config.getMinCapacity();
        this.beta = config.getBeta();
        this.rampCapacityFactor = config.getRampCapacityFactor();
        this.trunkCapacityFactor = config.getTrunkCapacityFactor();
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.outputHierarchy = outputHierarchy;
        initCapacityPerCategory();
    }

    private void initCapacityPerCategory() {
        Map<Integer, Double> totalCapacities = new HashMap<>();
        Map<Integer, Integer> categoryCounts = new HashMap<>();

        for (Link link : network.getLinks().values()) {
            Integer category = countsProcessor.getLinkCategory(link.getId());

            if ((category == null)||(category == NetworkCalibrationUtils.UNKNOWN_CATEGORY)) {
                continue; // skip links with unknown category
            }

            double capacity = link.getCapacity();
            double numberOfLanes = link.getNumberOfLanes();
            double length = link.getLength();
            double freeSpeed = link.getFreespeed();
            double correctedCapacity = NetworkCalibrationUtils.getMinimumSpeedBasedCapacity(length, minSpeed, sampleSize);

            // skip links that have a corrected capacity based on their length
            // since very short lengths are corrected at some stage we need to avoid them too, these links have a free speed equal to their length
            if (Math.abs(capacity - correctedCapacity)<5.0 || Math.abs(length-freeSpeed)<0.1){
                continue;
            }

            // capacity per lane
            capacity = capacity / numberOfLanes;

            // also ignore high capacity links
            if (capacity > 2000.0) { // 2000 veh/h/lane is the value used by default for motorways in PT2MATSim
                continue;
            }

            totalCapacities.put(category, totalCapacities.getOrDefault(category, 0.0) + capacity);
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        for (Map.Entry<Integer, Double> entry : totalCapacities.entrySet()) {
            int category = entry.getKey();
            if (considersCategory(category)) {
                double averageCapacity = entry.getValue() / categoryCounts.get(category);
                capacityPerCategory.put(category, averageCapacity);
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        flowsEstimator.updateAndSaveCounts(iterationEndsEvent);

        int iteration = iterationEndsEvent.getIteration();
        if (updateInterval>0 && iteration % updateInterval == 0 && iteration > 0) {
            updateCapacities();
            saveCapacities(iteration);
        }
        if (saveNetworkInterval>0 && iteration % saveNetworkInterval == 0 && iteration > 0) {
            saveNetwork(iteration);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        flowsEstimator.resetCounts(iterationStartsEvent.getIteration());
    }

    public boolean considersCategory(int category) {
        return categoriesToCalibrate.contains(category);
    }

    private void updateCapacities() {
        // the updated link capacity is computed as:
        // newCapacity = oldCapacity * (targetFlow / simulatedFlow) * ratio
        // the ratio is computed so that the highest category does not exceed maxCapacity, and lowest category does not go below minCapacity
        // all other categories are scaled accordingly, otherwise we might end up with unrealistic capacities and non-unique solutions
        double ratio = computeRatio();

        // compute new capacities for all categories and scale them using ratio
        Map<Integer, Double> capacities = new HashMap<>();
        for (int category: categoriesToCalibrate) {
            double newCapacity = getNewCapacity(category, ratio, true);
            // smooth the capacities to avoid large jumps
            double currentCapacity = capacityPerCategory.get(category);
            newCapacity = beta * currentCapacity + (1.0-beta) * newCapacity;
            // store the new capacity
            capacities.put(category, newCapacity);
        }

        // apply the new capacities to the network
        applyCapacity(capacities);

        // update the capacities that are stored
        for (int category : capacities.keySet()) {
            capacityPerCategory.put(category, capacities.get(category));
        }
    }

    private double computeRatio() {
        // first compute ration corresponding to capping the highest category to maxCapacity
        double maxNewCapacity = 0.0;
        for (int category: categoriesToCalibrate) {
            double catNewCapacity = getNewCapacity(category, 1.0, false);
            if (catNewCapacity > maxNewCapacity){
                maxNewCapacity = catNewCapacity;
            }
        }

        if (maxNewCapacity > maxCapacity){
            return maxCapacity/maxNewCapacity;
        }
        // if the highest capacity is already below maxCapacity, compute the ratio that ensures that the lowest category does not go below minCapacity
        double minNewCapacity = Double.MAX_VALUE;
        for (int category: categoriesToCalibrate) {
            double catNewCapacity = getNewCapacity(category, 1.0, false);
            if (catNewCapacity < minNewCapacity){
                minNewCapacity = catNewCapacity;
            }
        }
        if (minNewCapacity < minCapacity){
            return minCapacity/minNewCapacity;
        }
        // otherwise, return 1.0
        return 1.0;
    }

    private double getNewCapacity(int category, double ratio, boolean clipIt) {
        double targetFlow = countsProcessor.getAverageCountForCategory(category);
        double simulatedFlow = flowsEstimator.getFlowByCategory(category, sampleSize);
        double currentCapacity = capacityPerCategory.get(category);
        // if the difference between target and simulated flow is within 3%, do not change the capacity
        if (Math.abs(targetFlow - simulatedFlow)/targetFlow <= 0.03) {
            return currentCapacity; // no change if within tolerance
        }
        // else, compute the new capacity
        double newCapacity = currentCapacity * (targetFlow / simulatedFlow) * ratio;
        if (clipIt){
            newCapacity = Math.max(Math.min(newCapacity, maxCapacity), minCapacity); // keep capacities within reasonable bounds
        }
        return newCapacity;
    }

    private void applyCapacity(Map<Integer, Double> capacities) {
        for (Link link : network.getLinks().values()) {
            Integer linkCategory = countsProcessor.getLinkCategory(link.getId());
            if ((linkCategory != null) &&
                (linkCategory != NetworkCalibrationUtils.UNKNOWN_CATEGORY) &&
                (capacities.containsKey(linkCategory))) {
                double numberOfLanes = link.getNumberOfLanes();
                double linkCapacity = capacities.get(linkCategory) * numberOfLanes;

                // Correct for ramps
                if (NetworkCalibrationUtils.isRamp(link)) {
                    linkCapacity = linkCapacity * rampCapacityFactor; // reduce capacity by 30% for ramps
                }
                // Correct for trunks
                if (NetworkCalibrationUtils.isTrunk(link)) {
                    linkCapacity = linkCapacity * trunkCapacityFactor; // reduce capacity by 10% for trunks compared to motorways
                }

                // correct capacity for very short links, similar to how it is done in eqasim-python/Switzerland
                if (correctCapacities) {
                    double correctedCapacity = NetworkCalibrationUtils.getMinimumSpeedBasedCapacity(link.getLength(), minSpeed, sampleSize);
                    linkCapacity = Math.max(linkCapacity, correctedCapacity);
                }

                link.setCapacity(linkCapacity);
            }
        }
    }

    private void saveCapacities(int iteration) {
        String filename = outputHierarchy.getIterationFilename(iteration, "link_category_capacities.csv");
        try (java.io.BufferedWriter writer = org.matsim.core.utils.io.IOUtils.getBufferedWriter(filename)) {
            writer.write("Category;Capacity(veh/h/lane)\n");
            for (int category : capacityPerCategory.keySet()) {
                writer.write(category + ";" + String.format("%.2f", capacityPerCategory.get(category)) + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing link category capacities to file: " + filename);
        }
    }

    private void saveNetwork(int iteration) {
        String filename = outputHierarchy.getIterationFilename(iteration, "network_calibrated.xml.gz");
        new NetworkWriter(network).write(filename);
    }
}
