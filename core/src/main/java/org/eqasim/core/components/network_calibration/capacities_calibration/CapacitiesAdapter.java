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
import java.util.Map;

public class CapacitiesAdapter implements IterationEndsListener, IterationStartsListener {

    private final Network network;
    private final FlowByLinkCategory flowsEstimator;
    private final double sampleSize;
    private final int updateInterval;
    private final int saveNetworkInterval;
    private final boolean correctCapacities;
    private final Map<Integer, Double> linkCatCapacities = new HashMap<>();
    private final Map<Integer, Double> linkCatActualFlows = new HashMap<>();
    private final OutputDirectoryHierarchy outputHierarchy;

    private final double minSpeed; // km/h (used in capacity correction)
    private final double maxCapacity; // veh/h/lane (for the highest category, used to scale all capacities)
    private final double minCapacity = 300.0; // veh/h/lane (minimum capacity allowed for any category)

    public CapacitiesAdapter(Network network, FlowByLinkCategory flowsEstimator, NetworkCalibrationConfigGroup config,
                             EqasimConfigGroup eqasimConfig, OutputDirectoryHierarchy outputHierarchy) {
        this.network = network;
        this.flowsEstimator = flowsEstimator;
        this.updateInterval = config.getUpdateInterval();
        this.saveNetworkInterval = config.getSaveNetworkInterval();
        this.sampleSize = eqasimConfig.getSampleSize();
        this.minSpeed = config.getMinSpeed();
        this.correctCapacities = config.getCorrectCapacities();
        this.maxCapacity = config.getMaxCapacity();

        linkCatActualFlows.put(1,config.getCat1Flow());
        linkCatActualFlows.put(2,config.getCat2Flow());
        linkCatActualFlows.put(3,config.getCat3Flow());
        linkCatActualFlows.put(4,config.getCat4Flow());
        linkCatActualFlows.put(5,config.getCat5Flow());

        this.outputHierarchy = outputHierarchy;
        initLinkCatCapacities();
    }

    private void initLinkCatCapacities() {
        Map<Integer, Double> totalCapacities = new HashMap<>();
        Map<Integer, Integer> categoryCounts = new HashMap<>();

        for (Link link : network.getLinks().values()) {
            int category = Utils.getCategory(link);

            if (category == Utils.unknownCategory) {
                continue; // skip links with unknown category
            }

            double capacity = link.getCapacity();
            double numberOfLanes = link.getNumberOfLanes();
            double length = link.getLength();
            double freeSpeed = link.getFreespeed();
            double correctedCapacity = Utils.minimumSpeedBasedCapacity(length, minSpeed, sampleSize);

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
            double averageCapacity = entry.getValue() / categoryCounts.get(category);
            linkCatCapacities.put(category, averageCapacity);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        flowsEstimator.updateAndSaveCounts(iterationEndsEvent);

        int iteration = iterationEndsEvent.getIteration();
        if (iteration % updateInterval == 0 && iteration > 0) {
            updateCapacities();
            saveCapacities(iteration);
        }
        if (iteration % saveNetworkInterval == 0 && iteration > 0) {
            saveNetwork(iteration);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        flowsEstimator.resetCounts(iterationStartsEvent.getIteration());
    }

    private void updateCapacities() {
        // the updated link capacity is computed as:
        // newCapacity = oldCapacity * (targetFlow / simulatedFlow) * ratio
        // where ratio is maximumCapacity / cat1NewCapacity to ensure cat1 links have the highest capacity,
        // all other categories are scaled accordingly, otherwise we might end up with unrealistic capacities and non-unique solutions
        double ratio = computeAdvancedRation();

        // compute new capacities for all categories and scale them using ratio
        Map<Integer, Double> capacities = new HashMap<>();
        for (int category = 1; category <= 5; category++) {
            double newCapacity = getNewCapacity(category, ratio, true);
            // smooth the capacities to avoid large jumps
            double currentCapacity = linkCatCapacities.get(category);
            newCapacity = 0.5 * currentCapacity + 0.5 * newCapacity;
            // store the new capacity
            capacities.put(category, newCapacity);
        }

        // apply the new capacities to the network
        applyCapacity(capacities);

        // update the capacities that are stored
        for (int category : capacities.keySet()) {
            linkCatCapacities.put(category, capacities.get(category));
        }
    }

    private double computeSimpleRatio() {
        double cat1NewCapacity = getNewCapacity(1, 1.0, true);
        return maxCapacity/cat1NewCapacity;
    }

    private double computeAdvancedRation() {
        // first compute ration corresponding to capping the highest category to maxCapacity
        double cat1NewCapacity = getNewCapacity(1, 1.0, false);
        double cat2NewCapacity = getNewCapacity(2, 1.0, false);
        double maxNewCapacity = Math.max(cat1NewCapacity, cat2NewCapacity);
        if (maxNewCapacity > maxCapacity){
            return maxCapacity/maxNewCapacity;
        }
        // if the highest capacity is already below maxCapacity, compute the ratio that ensures that the lowest category does not go below minCapacity
        double cat5NewCapacity = getNewCapacity(5, 1.0, false);
        double cat4NewCapacity = getNewCapacity(4, 1.0, false);
        double minNewCapacity = Math.min(cat5NewCapacity, cat4NewCapacity);
        if (minNewCapacity < minCapacity){
            return minCapacity/minNewCapacity;
        }
        // otherwise, return 1.0
        return 1.0;
    }

    private double getNewCapacity(int category, double ratio, boolean clipIt) {
        double targetFlow = linkCatActualFlows.get(category);
        double simulatedFlow = flowsEstimator.getFlowByCategory(category, sampleSize);
        double currentCapacity = linkCatCapacities.get(category);
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
            int linkCategory = Utils.getCategory(link);
            if (linkCategory > 0) {
                double numberOfLanes = link.getNumberOfLanes();
                double linkCapacity = Math.min(capacities.get(linkCategory), 1800.0) * numberOfLanes;

                // Correct for ramps
                if (Utils.isRamp(link)) {
                    linkCapacity = linkCapacity * 0.75; // reduce capacity by 25% for ramps
                }

                // correct capacity for very short links, similar to how it is done in eqasim-python/Switzerland
                if (correctCapacities) {
                    double correctedCapacity = Utils.minimumSpeedBasedCapacity(link.getLength(), minSpeed, sampleSize);
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
            for (int category : linkCatCapacities.keySet()) {
                writer.write(category + ";" + String.format("%.2f", linkCatCapacities.get(category)) + "\n");
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
