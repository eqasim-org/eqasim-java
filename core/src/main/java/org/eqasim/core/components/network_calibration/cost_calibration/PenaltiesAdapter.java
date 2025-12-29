package org.eqasim.core.components.network_calibration.cost_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.capacities_calibration.CountsProcessor;
import org.eqasim.core.components.network_calibration.capacities_calibration.FlowProcessor;
import org.eqasim.core.components.network_calibration.capacities_calibration.NetworkCalibrationUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.core.utils.io.IOUtils.getBufferedReader;
import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

public class PenaltiesAdapter implements IterationStartsListener, IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(PenaltiesAdapter.class);

    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final double sampleSize;
    private final List<Integer> categoriesToCalibrate;
    private final Network network;
    private final Map<Integer, Double> penalties = new HashMap<>();
    private final double beta;
    private final int updateInterval;
    private final int saveNetworkInterval;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final double minPenalty;
    private final double maxPenalty;
    private final double rampFactor;
    private final double trunkFactor;
    private final String penaltiesFile;
    private boolean calibrate;
    public PenaltiesAdapter(CountsProcessor countsProcessor, FlowProcessor flowProcessor, Network network,
                            NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                            EqasimConfigGroup eqasimConfig) {
        this.countsProcessor = countsProcessor;
        this.flowProcessor = flowProcessor;
        this.network = network;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.categoriesToCalibrate = config.getCategoriesToCalibrationAsList();
        this.updateInterval = config.getUpdateInterval();
        this.saveNetworkInterval = config.getSaveNetworkInterval();
        this.beta = config.getBeta();
        this.outputHierarchy = outputHierarchy;
        this.minPenalty = config.getMinPenalty();
        this.maxPenalty = config.getMaxPenalty();
        this.rampFactor = config.getRampFactor();
        this.trunkFactor = config.getTrunkFactor();
        this.penaltiesFile = config.getPenaltiesFile();
        if (penaltiesFile == null || penaltiesFile.isEmpty() || penaltiesFile.equals("none")) {
            calibrate = true;
        } else {
            readPenaltiesFromFile();
            calibrate = false;
        }

        // Adjust network capacities initially
        NetworkCalibrationUtils.adjustNetworkCapacities(network, config.getMinCapacity(), config.getMaxCapacity(), sampleSize,
                config.getCorrectCapacities(), config.getMinSpeed());
    }

    public double computePenalty(Link link) {
        Integer category = countsProcessor.getLinkCategory(link.getId());
        if (category==null || !categoriesToCalibrate.contains(category)) {
            return 0.0;
        }
        double penalty = penalties.getOrDefault(category, 0.0);
        double travelTime = link.getLength() / link.getFreespeed();

        // Correct for ramps
        if (NetworkCalibrationUtils.isRamp(link)) {
            penalty = penalty * rampFactor; // for ramps, multiply penalty by a factor
        }
        // Correct for trunks
        if (NetworkCalibrationUtils.isTrunk(link)) {
            penalty = penalty * trunkFactor; // for trunks, multiply penalty by a factor
        }
        return travelTime * penalty;
    }

    double getEffectiveBeta(double percentageError, int iteration) {
        // double error = Math.abs(percentageError);
        // Linear scaling from 0.2 to 2.0 as error goes from 0 to 0.2, capped beyond
        // double factor = 0.2 + 1.4 * Math.min(1.0, Math.pow(error,1.5) / 0.2);
        double factor;
        if (iteration <= 20) {
            factor = 2.0;
        } else if (iteration <= 40) {
            factor = 1.2;
        } else if (iteration <= 60) {
            factor = 0.8;
        } else {
            factor = 0.5;
        }
        return beta * factor;
    }

    public void updatePenalties(int iteration) {
        for (Integer category : categoriesToCalibrate) {
            // get current penalty for this category
            double penalty = penalties.getOrDefault(category, 0.0);
            // Get average count for this category
            double count = countsProcessor.getAverageCountForCategory(category);
            if (count > 0.0) {
                // Get flow for this category
                double flow = flowProcessor.getFlowByCategory(category, sampleSize);
                // compute percentage difference
                double percentageDifference = (flow - count) / count;
                // get effective beta
                double effectiveBeta = getEffectiveBeta(percentageDifference, iteration);
                // update penalty
                penalty += (effectiveBeta * percentageDifference);
                // ensure penalty is non-negative
                penalty = Math.min(Math.max(penalty, minPenalty),maxPenalty);
            }
            penalties.put(category, penalty);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        flowProcessor.updateAndSaveCounts(iterationEndsEvent);

        // if penalties are provided in a file, do not calibrate, else update penalties
        if (calibrate) {
            int iteration = iterationEndsEvent.getIteration();
            if (updateInterval > 0 && iteration % updateInterval == 0 && iteration > 0) {
                updatePenalties(iteration);
                savePenalties(iteration);
            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        flowProcessor.resetCounts(iterationStartsEvent.getIteration());
    }

    private void savePenalties(int iteration) {
        String filename = outputHierarchy.getIterationFilename(iteration, "link_category_penalties.csv");
        try (BufferedWriter writer = getBufferedWriter(filename)) {
            writer.write("Category;Penalty(%)\n");
            for (int category : penalties.keySet()) {
                writer.write(category + ";" + String.format("%.2f", penalties.get(category)) + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing link category capacities to file: " + filename);
        }
    }

    private void readPenaltiesFromFile() {
        String filename = penaltiesFile;
        try (BufferedReader reader = getBufferedReader(filename)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                int category = Integer.parseInt(parts[0]);
                double penalty = Double.parseDouble(parts[1]);
                penalties.put(category, penalty);
            }
            logger.info("Read link category penalties from file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error reading link category penalties from file: " + filename);
        }
    }

}