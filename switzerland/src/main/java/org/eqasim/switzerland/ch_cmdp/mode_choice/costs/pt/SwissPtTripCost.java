package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.List;
import java.util.function.DoubleSupplier;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
import org.matsim.api.core.v01.population.Person;

/**
 * The PT price of a trip, the same for the ch and ch_cmdp simulations (their SwissPtCostModel): free travel, then the
 * tickets of the part the regional subscription does not cover (or a Léman Pass single ticket), capped.
 */
public final class SwissPtTripCost {
    /** The highest price of a trip, without and with the half fare (half-fare card, or under 16). */
    public static final double MAXIMUM_PRICE = 60.0;
    public static final double MAXIMUM_PRICE_HALF_FARE = 35.0;

    private SwissPtTripCost() {
    }

    /**
     * Whether a traveller travels free: with a GA, under 6, under 16 with a Junior card, or under 25 with a Gleis 7
     * card from 19:00 to 05:00 (the departure time is only asked for then).
     */
    public static boolean hasFreeTravel(int age, boolean generalSubscription, boolean juniorSubscription,
            boolean gleis7Subscription, DoubleSupplier departureTime) {
        return generalSubscription
                || age < 6
                || (age < 16 && juniorSubscription)
                || (age < 25 && gleis7Subscription && isNight(departureTime.getAsDouble()));
    }

    private static boolean isNight(double time) {
        return time < 5 * 3600 || time >= 19 * 3600;
    }

    /**
     * The price of a trip without free travel (hasFreeTravel). With a regional subscription (Verbundabo), only what the
     * trip travels outside the zones of the person's RegionalSubscriptionProducts is paid; without the subscription,
     * the whole trip. The usual tickets, or a Léman Pass single ticket and the usual tickets of the other legs
     * (SwissPtStageCostCalculator.price), at most MAXIMUM_PRICE (MAXIMUM_PRICE_HALF_FARE with the half fare).
     */
    public static double price(Person person, PersonVariables personVariables, boolean halbtaxSubscription,
            boolean regionalSubscription, SwissPtVariables trip, SwissPtRoutePredictor predictor,
            SwissPtStageCostCalculator calculators) {
        List<SwissPtLegVariables> paid = regionalSubscription
                ? predictor.uncoveredLegs(trip, RegionalSubscription.of(person))
                : trip.legVariables;

        double price = calculators.price(trip.legVariables, paid, halbtaxSubscription, personVariables);

        boolean halfFare = halbtaxSubscription || SwissPtStageCostCalculator.hasChildFare(personVariables);
        return Math.min(price, halfFare ? MAXIMUM_PRICE_HALF_FARE : MAXIMUM_PRICE);
    }
}
