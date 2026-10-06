package org.eqasim.switzerland.ch_cmdp.tolls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.tools.random.Normal;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a deterministic, per-vehicle factor to convert a monetary toll into the
 * router's disutility units (e.g. seconds), representing unobserved heterogeneity in
 * agents' sensitivity to cost.
 * <p>
 * Each vehicle is assigned a fixed, reproducible draw from a log-normal distribution,
 * derived purely from its id via a SplitMix64 hash (no {@link java.util.Random} state
 * involved), normalized so the draw has a population mean of 1. This mirrors MATSim's
 * own {@code RandomizingTimeDistanceTravelDisutilityFactory}, which multiplies monetary
 * disutility by a per-person log-normal random draw with mean 1 so that individual route
 * choice varies while the population's average valuation of money stays unchanged.
 * <p>
 * The draw is then converted using the value of time in currency/hour, so the
 * result can be used directly as:
 * {@code disutility += toll * marginalCostOfTolls.getMarginalCostOfTolls(vehicle)}.
 */
public class MarginalCostOfTolls {
    private static final double SECONDS_PER_HOUR = 3600.0;
    private final Logger logger = LogManager.getLogger(MarginalCostOfTolls.class);

    // standard deviation of the underlying normal distribution; controls the spread of heterogeneity
    private final double sigma;
    // mu chosen so the log-normal distribution has mean 1: E[exp(N(mu, sigma^2))] = exp(mu + sigma^2/2) = 1
    private final double mu;
    // population-average conversion from one currency unit to seconds of disutility
    private final double secondsPerCurrencyUnit;
    // base seed so the whole population's draws can be reproducibly shifted (e.g. across scenario variants)
    private final long baseSeed;

    // each vehicle keeps the same complete seconds-per-currency multiplier for the whole run
    private final Map<Id<Vehicle>, Double> vehicleMultipliers = new ConcurrentHashMap<>();

    public MarginalCostOfTolls(double sigma, double valueOfTime) {
        this(sigma, valueOfTime, 4711L);
    }

    public MarginalCostOfTolls(double sigma, double valueOfTime, long baseSeed) {
        if (!Double.isFinite(valueOfTime) || valueOfTime <= 0.0) {
            throw new IllegalArgumentException("valueOfTime must be positive and finite");
        }
        if (!Double.isFinite(sigma) || sigma < 0.0) {
            throw new IllegalArgumentException("sigma must be non-negative and finite");
        }
        this.sigma = sigma;
        this.mu = -0.5 * sigma * sigma;
        this.secondsPerCurrencyUnit = SECONDS_PER_HOUR / valueOfTime;
        this.baseSeed = baseSeed;

        logger.info("Initialized MarginalCostOfTolls with: \n\t - sigma={} \n\t - valueOfTime={} \n\t - baseSeed={}", sigma, valueOfTime, baseSeed);
    }

    /**
     * Returns the factor to multiply a toll (in currency) by to get its contribution to
     * the router's disutility. Deterministic per vehicle id: the same vehicle always gets
     * the same factor, across calls and across runs.
     */
    public double getMarginalCostOfTolls(Vehicle vehicle) {
        if (sigma < 1e-6) {
            return secondsPerCurrencyUnit;
        }
        return vehicleMultipliers.computeIfAbsent(vehicle.getId(), this::drawMultiplier);
    }

    // ------------------- HELPER METHODS -------------------

    float drawCostSensitivity(Id<Vehicle> vehicleId) {
        if (sigma<1e-6) {
            return 1.0f;
        }
        return (float) Math.max(Math.exp(mu + sigma * standardNormalFor(vehicleId)), 0.0);
    }
    float drawCostSensitivity(String str) {
        if (sigma<1e-6) {
            return 1.0f;
        }
        return (float) Math.max(Math.exp(mu + sigma * standardNormalFor(str)), 0.0);
    }

    private double drawMultiplier(Id<Vehicle> vehicleId) {
        return drawCostSensitivity(vehicleId) * secondsPerCurrencyUnit;
    }

    /**
     * Deterministic standard-normal draw for a vehicle, via Box-Muller on two
     * decorrelated SplitMix64 outputs (chained: the second key is derived from the
     * first, so both come from a single seed without needing independent inputs).
     */
    public double standardNormalFor(Id<Vehicle> vehicleId) {
        return standardNormalFor(vehicleId.toString());
    }

    public double standardNormalFor(String str) {
        return Normal.get(str, baseSeed);
    }
}
