package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsDistances;
import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsWriter;
import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsWriter.Entry;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

public class MobilityCoinsMarket implements IterationEndsListener {
    static public final String WALLET_ATTRIBUTE = "wallet";

    private final MobilityCoinsParameters parameters;
    private final MobilityCoinsCalculator calculator;
    private final MobilityCoinsWriter writer;

    private final Population population;

    // updated after every iteration
    private double marketPrice_EUR_per_coin;

    public double getMarketPrice_EUR_per_coin() {
        // used wherever the market price is needed
        return marketPrice_EUR_per_coin;
    }

    public MobilityCoinsMarket(MobilityCoinsParameters parameters, MobilityCoinsCalculator calculator,
            Population population, MobilityCoinsWriter writer) {
        this.parameters = parameters;
        this.population = population;
        this.calculator = calculator;
        this.writer = writer;
        this.marketPrice_EUR_per_coin = parameters.initialMarketPrice_EUR_per_coin;

        for (Person person : population.getPersons().values()) {
            person.getAttributes().putAttribute(WALLET_ATTRIBUTE, parameters.initialCoins_per_person);
        }
    }

    private double calculateBalance() {
        // calculate a balance of coins in the system
        double globalBalance = 0.0;

        // the idea is that we go through the current configuration of the population
        // and reconstruct the coins lost / gained
        for (Person person : population.getPersons().values()) {
            double personBalance = getInitialCoins(person);

            for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
                MobilityCoinsDistances distances = MobilityCoinsDistances.calculate(trip.getTripElements());
                double coinsDelta = calculator.calculateCoinDelta(distances);
                personBalance += coinsDelta;
            }

            globalBalance += Math.max(0.0, personBalance);

            if (personBalance < 0.0) {
                // agent needed to buy extra coins
                globalBalance -= personBalance;
            }

        }

        return globalBalance;
    }

    private double calculateMarketPrice(double globalBalance) {
        // update rule
        if (globalBalance > parameters.targetCoins) {
            // too many coins used, make more expensive
            return marketPrice_EUR_per_coin + parameters.marketPriceUpdate;
        } else {
            // not enough coins used, make less expensive
            return marketPrice_EUR_per_coin - parameters.marketPriceUpdate;
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        // calculate a balance of coins in the system
        double globalBalance = calculateBalance();

        // at the end of the iteration, we update the market price
        double updatedMarketPrice = calculateMarketPrice(globalBalance);

        // perform the update
        marketPrice_EUR_per_coin = //
                marketPrice_EUR_per_coin * (1.0 - parameters.marketPriceSmoothing) + //
                        updatedMarketPrice * parameters.marketPriceSmoothing;

        // track prices
        writer.writeMarketPrice(
                new Entry(event.getIteration(), globalBalance, updatedMarketPrice, marketPrice_EUR_per_coin));
    }

    static public double getInitialCoins(Person person) {
        return (Double) person.getAttributes().getAttribute(WALLET_ATTRIBUTE);
    }
}
