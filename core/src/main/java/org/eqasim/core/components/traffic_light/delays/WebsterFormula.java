package org.eqasim.core.components.traffic_light.delays;

public class WebsterFormula {
    public static double TOTAL_LOST_TIME = 5.0; // NOT USED
    // Total lost time in seconds, this default value is obtained from:
    // Automated generation of traffic signals and lanes for MATSim based on OpenStreetMap
    // Theresa Ziemke, Sohnke Braun, 2021, section 3.4
    public static double MINIMUM_GREEN_TIME = 10.0; // same source as above

    public static double getMinimumGreenTime() {
        return MINIMUM_GREEN_TIME;
    }

    public static double delay(double c, double g, double x, double q){
        // Webster's delay formula
        // c: cycle time
        // g: effective green time
        // x: ratio of green time to cycle time
        // q: flow rate
        // Validate inputs to avoid division by zero or invalid operations
        if (c == 0 || q == 0 || x == 1 || g*x/c == 0) {
            throw new IllegalArgumentException("Invalid input: C, q, or x cannot be zero or x cannot be 1.");
        }
        // Calculate each term step-by-step
        // Term 1: C * (1 - g/C)^2 / (2 * (1 - g/C * x))
        double term1 = c * Math.pow(1 - (g / c), 2) / (2 * (1 - (g / c) * x));
        // Term 2: x^2 / (2 * q * (1 - x))
        double term2 = Math.pow(x, 2) / (2 * q * (1 - x));
        // Term 3: 0.65 * (C / q^2)^(1/3) * x^(2 + 5*g/C)
        double term3 = 0.65 * Math.pow(c / Math.pow(q, 2), 1.0 / 3.0) * Math.pow(x, 2 + 5.0 * (g / c));
        // Combine all terms
        return term1 + term2 - term3;

    }

    public static double L(int n){
        // source: //https://www.apsed.in/post/traffic-signal-design-webster-s-formula-for-optimum-cycle-length#:~:text=All%20red%20time%20is%20usually,ratio%20at%20all%20the%20phases
        // same formula used in: Investigating Parameter Interactions with the Factorial Design Method: Webster’s Optimal Cycle Length Model, Ali Payıdar AKGÜNGÖR, Ersin KORKMAZ, 2018
        double lostTimeAtPhase = 3.0;
        double allRedTime = 0.0;
        return n*lostTimeAtPhase + allRedTime; // Total lost time in seconds
    }

    public static double cOpt(double y, int n) {
        // Optimal cycle time using Webster's formula
        // l: the total lost or unusable time during a signal cycle, in seconds.
        // y: the sum of saturated ratios
        if (y==1) {
            throw new IllegalArgumentException("Invalid input: y (cannot be 1).");
        }
        return (1.5 * L(n) + 5.0) / (1 - y);
    }

    public static double gOpt(double C, int n) {
        return C-L(n); // Optimal green time
    }


}
