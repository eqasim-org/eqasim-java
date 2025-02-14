package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsDistances;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

public class MobilityCoinsUtilityPenalty implements UtilityPenalty {
	private final ModeParameters modeParameters;

	private final MobilityCoinsParameters parameters;
	private final MobilityCoinsMarket market;
	private final MobilityCoinsCalculator calculator;

	public MobilityCoinsUtilityPenalty(ModeParameters modeParameters,
			MobilityCoinsMarket market, MobilityCoinsCalculator calculator, MobilityCoinsParameters parameters) {
		this.market = market;
		this.calculator = calculator;
		this.modeParameters = modeParameters;
		this.parameters = parameters;
	}

	@Override
	public double calculatePenalty(String mode, Person person, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> elements) {
		// calculate modal distances
		MobilityCoinsDistances distances = MobilityCoinsDistances.calculate(elements);

		// calculate gains and losses
		double deltaCoins = calculator.calculateCoinDelta(distances);

		// prepare utility that is added
		double utility = 0.0;

		if (deltaCoins < 0.0) { // losses
			utility += parameters.beta_loss_u_per_coin * deltaCoins;
		} else { // gains
			utility += parameters.beta_gain_u_per_coin * deltaCoins;
		}

		// calculate personal coin balance before and after performing the trip
		double balanceBeforeTrip = MobilityCoinsMarket.getInitialCoins(person);

		for (TripCandidate previousTrip : previousTrips) {
			MobilityCoinsDistances previousDistances = MobilityCoinsDistances
					.calculate(((RoutedTripCandidate) previousTrip).getRoutedPlanElements());
			balanceBeforeTrip += calculator.calculateCoinDelta(previousDistances);
		}

		double balanceAfterTrip = balanceBeforeTrip + deltaCoins;

		if (balanceAfterTrip < 0.0 && balanceAfterTrip < balanceBeforeTrip) {
			// we need to purchase coins, calculate here
			double purchaseCoins = balanceBeforeTrip - balanceAfterTrip;

			// calculate monetary price of coins that need to be purchased
			double purchaseCost_EUR = purchaseCoins * market.getMarketPrice_EUR_per_coin();

			// use marginal utility of cost to integrate into penalty
			utility += modeParameters.betaCost_u_MU * purchaseCost_EUR;
		}

		// we need to return a penalty (= inverse of added utility)
		return -utility;
	}
}
