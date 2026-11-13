package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

/**
 * The rounding rules of the Swiss PT fares, shared by the calculators (as _round_up and _fare_rounding in
 * eqasim-switzerland, utils/gtfs_router/pricing.py).
 */
public final class PtFareRounding {
    private PtFareRounding() {
    }

    /** Up to the next multiple of step, without going a step too far on a float such as 34.00000000000001. */
    public static double roundUp(double price, double step) {
        double steps = Math.round(price / step * 1e9) / 1e9;
        return Math.ceil(steps) * step;
    }

    /** Up to 20 cents below 69 km, else up to the next franc. */
    public static double fareRounding(double price, double distance_km) {
        return distance_km < 69 ? roundUp(price, 0.2) : roundUp(price, 1.0);
    }

    /** To the cent, halves up. */
    public static double roundToCents(double price) {
        return Math.round(price * 100.0) / 100.0;
    }
}
