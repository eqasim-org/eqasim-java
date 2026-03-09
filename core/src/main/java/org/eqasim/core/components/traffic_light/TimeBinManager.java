package org.eqasim.core.components.traffic_light;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowConfigGroup;

public class TimeBinManager {
        private final Logger logger = LogManager.getLogger(TimeBinManager.class);

        private final double startTime;
        private final double endTime;
        private final double tlStartTime;
        private final double tlEndTime;
        private final double binSize;
        private final int numberOfBins;
        private final int numberOfTlBins;

        public TimeBinManager(DelaysConfigGroup config, FlowConfigGroup flowConfig) {
            checkCompatibility(config, flowConfig);
            this.startTime = config.getStartTime();
            this.endTime = config.getEndTime();
            this.tlStartTime = config.getTlStartTime();
            this.tlEndTime = config.getTlEndTime();
            this.binSize = config.getBinSize();

            this.numberOfBins = (int) Math.ceil((endTime - startTime) / binSize);
            this.numberOfTlBins = (int) Math.ceil((tlEndTime - tlStartTime) / binSize);

            logger.info("starting time is {}", startTime);
            logger.info("ending time is {}", endTime);
            logger.info("traffic light starting time is {}", tlStartTime);
            logger.info("traffic light ending time is {}", tlEndTime);
            logger.info("bin size is {}", binSize);
            logger.info("number of bins is {}", numberOfBins);
            logger.info("number of traffic light bins is {}", numberOfTlBins);
        }

        public int getNumberOfBins() {
            return numberOfBins;
        }

        public int getNumberOfTlBins() {
            return numberOfTlBins;
        }

        public double[] getBinsCenters() {
            double[] centers = new double[numberOfBins];
            for (int i = 0; i < numberOfBins; i++) {
                centers[i] = startTime + (i + 0.5) * binSize;
            }
            return centers;
        }

        public double[] getTlBinsCenters() {
            double[] centers = new double[numberOfTlBins];
            for (int i = 0; i < numberOfTlBins; i++) {
                centers[i] = tlStartTime + (i + 0.5) * binSize;
            }
            return centers;
        }

        public int getBinIndex(double time) {
            if (!timeInBounds(time)) {
                throw new IllegalArgumentException("Time is out of bounds: " + time + ". Valid range is [" + startTime + ", " + endTime + "]");
            }
            return Math.min(Math.max(0, (int) Math.floor((time - startTime) / binSize)), numberOfBins - 1);
        }

        public int getTlBinIndex(double time) {
            if (!timeInTlBounds(time)) {
                throw new IllegalArgumentException("Time is out of bounds: " + time + ". Valid range is [" + startTime + ", " + endTime + "]");
            }
            return Math.min(Math.max(0, (int) Math.floor((time - tlStartTime) / binSize)), numberOfTlBins - 1);
        }

        public boolean timeInBounds(double time) {
            return (time >= startTime && time <= endTime);
        }

        public boolean timeInTlBounds(double time) {
            return (time >= tlStartTime && time <= tlEndTime);
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

        public double getTlStartTime() {
            return tlStartTime;
        }

        public double getTlEndTime() {
            return tlEndTime;
        }

        public double[] getBinInterval(int binIndex) {
            if (binIndex < 0 || binIndex >= numberOfBins) {
                throw new IndexOutOfBoundsException("Bin index out of bounds: " + binIndex);
            }
            double start = startTime + binIndex * binSize;
            double end   = start + binSize;
            return new double[]{start, end};
        }

        public double[] getTlBinInterval(int binIndex) {
            if (binIndex < 0 || binIndex >= numberOfTlBins) {
                throw new IndexOutOfBoundsException("Bin index out of bounds: " + binIndex);
            }
            double start = tlStartTime + binIndex * binSize;
            double end   = start + binSize;
            return new double[]{start, end};
        }

        private void checkCompatibility(DelaysConfigGroup config, FlowConfigGroup flowConfig) {
            final double epsilon = 1e-3;
            // 1. They should have the same bin size, or the delay bin size should be a multiple of the flow bin size
            if (Math.abs(config.getBinSize() - flowConfig.getBinSize()) > epsilon) {
                if (config.getBinSize() < flowConfig.getBinSize() + epsilon || Math.abs(config.getBinSize() % flowConfig.getBinSize()) > epsilon) {
                    throw new IllegalArgumentException(String.format("Incompatible bin sizes: DelaysConfigGroup bin size is %f, FlowConfigGroup bin size is %f. They should be the same or the delay bin size should be a multiple of the flow bin size.", config.getBinSize(), flowConfig.getBinSize()));
                }
            }
            // 2. The delay time range should be within the flow time range
            if (config.getStartTime() < flowConfig.getStartTime() - epsilon || config.getEndTime() > flowConfig.getEndTime() + epsilon) {
                throw new IllegalArgumentException(String.format("Incompatible time ranges: DelaysConfigGroup time range is [%f, %f], FlowConfigGroup time range is [%f, %f]. The delay time range should be within the flow time range.", config.getStartTime(), config.getEndTime(), flowConfig.getStartTime(), flowConfig.getEndTime()));
            }
            // 3. The traffic light delay time range should be within the flow time range
            if (config.getTlStartTime() < flowConfig.getStartTime() - epsilon || config.getTlEndTime() > flowConfig.getEndTime() + epsilon) {
                throw new IllegalArgumentException(String.format("Incompatible time ranges: DelaysConfigGroup traffic light time range is [%f, %f], FlowConfigGroup time range is [%f, %f]. The traffic light delay time range should be within the flow time range.", config.getTlStartTime(), config.getTlEndTime(), flowConfig.getStartTime(), flowConfig.getEndTime()));
            }
            // 4. All start times and end times should be multiples of the flow bin size
            if (Math.abs(config.getStartTime() % flowConfig.getBinSize()) > epsilon || Math.abs(config.getEndTime() % flowConfig.getBinSize()) > epsilon || Math.abs(config.getTlStartTime() % flowConfig.getBinSize()) > epsilon || Math.abs(config.getTlEndTime() % flowConfig.getBinSize()) > epsilon) {
                throw new IllegalArgumentException(String.format("Incompatible time settings: All start times and end times should be multiples of the flow bin size. DelaysConfigGroup start time is %f, end time is %f, traffic light start time is %f, traffic light end time is %f, FlowConfigGroup bin size is %f.", config.getStartTime(), config.getEndTime(), config.getTlStartTime(), config.getTlEndTime(), flowConfig.getBinSize()));
            }
        }

    }

