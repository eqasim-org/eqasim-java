package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class MobilityCoinsParameters implements ParameterDefinition {
    // assumed CO2eq for one km of driving the car
    public double emissions_gco2_per_km_car = 149.0;

    // assumed CO2eq for one km of riding in transit
    public double emissions_gco2_per_km_transit = 30.0;

    // Equivalent of one coin in CO2eq
    public double cost_coins_per_gco2 = 0.001;

    // incentive in coins for riding one km the bicycle
    public double incentive_coins_per_km_bicycle = 10.0;

    // incentive in coins for walking one km
    public double incentive_coins_per_km_walking = 10.0;

    // marginal utility for coin losses
    public double beta_loss_u_per_coin = 1.0;

    // marginal utility for coin gains
    public double beta_gain_u_per_coin = 1.0;

    // blending factor when updating market price
    public double marketPriceSmoothing = 0.1;

    // initial market price
    public double initialMarketPrice_EUR_per_coin = 1.0;

    // target coins volume
    public double targetCoins = 100000000.0 * 0.001;

    // update coins delta
    public double marketPriceUpdate = 0.1;

    // initial coins per person
    public double initialCoins_per_person = 20.0;
}
