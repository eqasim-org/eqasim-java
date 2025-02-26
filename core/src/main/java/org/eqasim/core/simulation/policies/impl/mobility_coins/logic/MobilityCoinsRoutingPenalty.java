package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsDistances;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import com.google.common.base.Preconditions;

public class MobilityCoinsRoutingPenalty implements RoutingPenalty {
    private final ModeParameters modeParameters;
    private final MobilityCoinsCalculator calculator;
    private final MobilityCoinsMarket market;

    public MobilityCoinsRoutingPenalty(ModeParameters modeParameters, MobilityCoinsMarket market,
            MobilityCoinsCalculator calculator) {
        this.modeParameters = modeParameters;
        this.market = market;
        this.calculator = calculator;
    }

    @Override
    public double getLinkPenalty(Link link, Person person, double time, double baseDisutility) {
        // length of the link
        double length_km = link.getLength() * 1e-3;

        // prepare distances to use same calculation infrastructure
        MobilityCoinsDistances distances = new MobilityCoinsDistances(length_km, 0.0, 0.0, 0.0, 0.0);

        // calculate coins for going through the link
        double coins = calculator.calculateCoinDelta(distances);

        // calculate penalty in EUR
        double penalty_EUR = -market.getMarketPrice_EUR_per_coin() * coins;

        // we need to give the penalty in seconds, so convert to utilities using
        // marginal utility of cost
        double penalty_u = penalty_EUR * modeParameters.betaCost_u_MU;

        // convert to minutes using marginal utility of travel time in car
        double penalty_min = penalty_u / modeParameters.car.betaTravelTime_u_min;

        Preconditions.checkState(penalty_min >= 0.0);

        // return penalty in seconds
        return penalty_min * 60.0;
    }
}
