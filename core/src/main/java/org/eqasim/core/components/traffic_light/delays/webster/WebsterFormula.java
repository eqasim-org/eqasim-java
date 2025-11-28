package org.eqasim.core.components.traffic_light.delays.webster;

public class WebsterFormula {
    private final double totalLostTime; // default 5.0
    // Total lost time in seconds, this default value is obtained from:
    // Automated generation of traffic signals and lanes for MATSim based on OpenStreetMap
    // Theresa Ziemke, Sohnke Braun, 2021, section 3.4
    private final double minimumGreenTime;// default 5.0 - same source as above
    private final double maximumGreenTime; // default 60.0 - same source as above
    private final double maximumCycleLength;

    // source: //https://www.apsed.in/post/traffic-signal-design-webster-s-formula-for-optimum-cycle-length#:~:text=All%20red%20time%20is%20usually,ratio%20at%20all%20the%20phases
    // same formula used in: Investigating Parameter Interactions with the Factorial Design Method: Webster’s Optimal Cycle Length Model, Ali Payıdar AKGÜNGÖR, Ersin KORKMAZ, 2018
    private final double lostTimePerPhase; //default 4.0; usually between 2 and 5 seconds per phase
    private final double allRedTime; // default 3.0; usually 0

    private final double maximumSaturatedRatio; // default 0.98; Maximum saturated ratio, this value is used to avoid division by zero in the cOpt function
    private final double minimumFlowRate; // default 1.0/3600; Minimum flow rate, this value is used to avoid division by zero in the delay function

    public WebsterFormula(WebsterConfigGroup config) {
        this.totalLostTime = config.getTotalLostTime();
        this.minimumGreenTime = config.getMinimumGreenTime();
        this.maximumGreenTime = config.getMaximumGreenTime();
        this.maximumCycleLength = config.getMaximumCycleLength();
        this.lostTimePerPhase = config.getLostTimePerPhase();
        this.allRedTime =  config.getAllRedTime();
        this.maximumSaturatedRatio = config.getMaximumSaturatedFlow();
        this.minimumFlowRate = config.getMinimumFlowRate();
    }

    public double getMaximumGreenTime() {
        return maximumGreenTime;
    }

    public double getMinimumGreenTime() {
        return minimumGreenTime;
    }

    public double getMaximumCycleLength() {
        return maximumCycleLength;
    }

    public double getLostTimePerPhase() {
        return lostTimePerPhase;
    }
    public double getAllRedTime() {
        return allRedTime;
    }
    public double getMaximumSaturatedRatio() {
        return maximumSaturatedRatio;
    }
    public double getMinimumFlowRate() {
        return minimumFlowRate;
    }

    public double getMinimumCycleTime(int numPhases) {
        return minimumGreenTime * numPhases + L(numPhases);
    }

    public double delay(double c, double g, double x, double q){
        // Webster's delay formula
        // c: cycle time
        // g: effective green time
        // x: ratio of green time to cycle time
        // q: flow rate
        // Validate inputs to avoid division by zero or invalid operations
        if (c == 0.0 || q == 0.0 || x == 1.0 || g*x/c == 1.0) {
            System.err.println("Invalid inputs: c=" + c + ", g=" + g + ", x=" + x + ", q=" + q);
            throw new IllegalArgumentException("Invalid input: C, q, or x cannot be zero or x cannot be 1.");
        }
        // Calculate each term step-by-step
        // Term 1: C * (1 - g/C)^2 / (2 * (1 - g/C * x))
        double term1 = c * Math.pow(1.0 - (g / c), 2.0) / (2.0 * (1.0 - (g / c) * x));
        // Term 2: x^2 / (2 * q * (1 - x))
        double term2 = Math.pow(x, 2.0) / (2.0 * q * (1.0 - x));
        // Term 3: 0.65 * (C / q^2)^(1/3) * x^(2 + 5*g/C)
        double term3 = 0.65 * Math.pow(c / Math.pow(q, 2.0), 1.0 / 3.0) * Math.pow(x, 2.0 + 5.0 * (g / c));
        // Combine all terms
        return Math.min(term1 + term2 - term3, 40.0); // Return the total delay, capped at 40 seconds

    }

    public double L(int n){
        if (totalLostTime > 0.0) {
            return totalLostTime; // Use the total lost time if it is set
        }
        return n*lostTimePerPhase + allRedTime; // Total lost time in seconds
    }

    public double cOpt(double y, int n) {
        // Optimal cycle time using Webster's formula
        // l: the total lost or unusable time during a signal cycle, in seconds.
        // y: the sum of saturated ratios
        // n: the number of phases

        double optimalCycleTime = (1.5 * L(n) + 5.0) / (1.0 - Math.min(y, maximumSaturatedRatio)); // Ensure y does not exceed the maximum saturated ratio
        double minimumCycleTime = getMinimumCycleTime(n);
        double maximumCycleTime = getMaximumCycleLength();
        return Math.min(Math.max(optimalCycleTime, minimumCycleTime),
                        maximumCycleTime); // Ensure the cycle time is within the range of minimum and maximum cycle times
    }

    public double gOpt(double C, int n) {
        return C-L(n); // Optimal green time
    }


}
