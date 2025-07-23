package org.eqasim.core.components.flow;


public class TimeBinManager {

        private final double startTime;
        private final double endTime;
        private final double binSize;
        private final int numberOfBins;

        public TimeBinManager(double startTime, double endTime, double binSize) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.binSize = binSize;
            this.numberOfBins = (int) Math.floor((endTime - startTime) / binSize) + 1;
        }

        public int getNumberOfBins() {
            return numberOfBins;
        }

        public int getBinIndex(double time) {
            return Math.min(Math.max(0, (int) Math.floor((time - startTime) / binSize)), numberOfBins - 1);
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

    }

