package org.eqasim.core.components.flow;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FlowBinManager {
    // Use a single static logger instance per-class (conventional practice)
    private static final Logger logger = LogManager.getLogger(FlowBinManager.class);

    private final double startTime;
    private final double endTime;
    private final double binSize;
    private final int numberOfBins;

    public FlowBinManager(FlowConfigGroup config) {
        // Validate inputs for predictable behaviour
        this.startTime = config.getStartTime();
        this.endTime = config.getEndTime();
        this.binSize = config.getBinSize();

        if (binSize <= 0.0) {
            throw new IllegalArgumentException("binSize must be > 0. Provided: " + binSize);
        }
        if (endTime <= startTime) {
            throw new IllegalArgumentException("endTime must be > startTime. Provided startTime="
                    + startTime + ", endTime=" + endTime);
        }

        this.numberOfBins = getNumberOfBins(startTime, endTime, binSize);
        if (this.numberOfBins <= 0) {
            throw new IllegalStateException("Computed numberOfBins <= 0. Check start/end times and bin size.");
        }

        logger.info("Traffic counting start time: {}", startTime);
        logger.info("Traffic counting end time: {}", endTime);
        logger.info("Bin size: {}", binSize);
        logger.info("Number of bins: {}", numberOfBins);
    }

    public int getNumberOfBins() {
        return numberOfBins;
    }
    public int getNumberOfBins(double startTimeL, double endTimeL, double binSizeL) {
        return (int) Math.ceil((endTimeL - startTimeL) / binSizeL);
    }

    public double[] getBinsCenters(int numberOfBinsL, double startTimeL, double binSizeL) {
        double[] centers = new double[numberOfBinsL];
        for (int i = 0; i < numberOfBinsL; i++) {
            centers[i] = startTimeL + (i + 0.5) * binSizeL;
        }
        return centers;
    }
    public double[] getBinsCenters() {
        return getBinsCenters(numberOfBins, startTime, binSize);
    }


    public int getBinIndex(double time) {
        return Math.min(Math.max(0, (int) Math.floor((time - startTime) / binSize)), numberOfBins - 1);
    }

    public boolean timeInBounds(double time) {
        return (time >= startTime && time <= endTime);
    }

    public double getBinSize() {
        return binSize;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public double[] getBinInterval(int binIndex) {
        if (binIndex < 0 || binIndex >= numberOfBins) {
            throw new IndexOutOfBoundsException("Bin index out of bounds: " + binIndex);
        }
        double start = startTime + binIndex * binSize;
        double end   = start + binSize;
        return new double[]{start, end};
    }

}
