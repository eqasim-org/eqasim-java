package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsDistances;

/**
 * This class calculates gains and losses based on the per-mode distances.
 */
public class MobilityCoinsCalculator {
    private final MobilityCoinsParameters parameters;

    public MobilityCoinsCalculator(MobilityCoinsParameters parameters) {
        this.parameters = parameters;
    }

    public double calculateCoinDelta(MobilityCoinsDistances distances) {
        double coins = 0.0;

        coins -= parameters.cost_coins_per_gco2 * parameters.emissions_gco2_per_km_car * distances.car_km();
        coins -= parameters.cost_coins_per_gco2 * parameters.emissions_gco2_per_km_car * distances.carPassenger_km();

        coins -= parameters.cost_coins_per_gco2 * parameters.emissions_gco2_per_km_transit * distances.transit_km();

        coins += parameters.incentive_coins_per_km_bicycle * distances.bicycle_km();
        coins += parameters.incentive_coins_per_km_walking * distances.walk_km();

        return coins;
    }
}
