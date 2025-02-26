package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

public class MobilityCoinsParameters {
    // assumed CO2eq for one km of driving the car
    public final double emissions_gco2_per_km_car = 149.0;

    // assumed CO2eq for one km of riding in transit
    public final double emissions_gco2_per_km_transit = 30.0;

    // Equivalent of one coin in CO2eq
    public final double cost_coins_per_gco2 = 1e-3;

    // incentive in coins for riding one km the bicycle
    public final double incentive_coins_per_km_bicycle = 1.0;

    // incentive in coins for walking one km
    public final double incentive_coins_per_km_walking = 1.0;

    // marginal utility for coin losses
    public final double beta_loss_u_per_coin = 0.310998; // from beta_cost

    // marginal utility for coin gains
    public final double beta_gain_u_per_coin = 0.310998; // from beta_cost

    // blending factor when updating market price
    public final double marketPriceSmoothing = 0.1;

    // initial market price
    public final double initialMarketPrice_EUR_per_coin = 1.0;

    // target coins volume
    public final double targetCoins = 150000.0;

    // update coins delta
    public final double marketPriceUpdate = 0.1;

    // initial coins per person
    public final double initialCoins_per_person = 10.0;
}
