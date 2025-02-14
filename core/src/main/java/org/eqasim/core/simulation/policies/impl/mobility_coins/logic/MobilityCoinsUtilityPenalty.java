package org.eqasim.core.simulation.policies.impl.mobility_coins.logic;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.impl.mobility_coins.MobilityCoinsDistances;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

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
			List<? extends PlanElement> elements) {
		// calculate modal distances
		MobilityCoinsDistances distances = MobilityCoinsDistances.calculate(elements);

		// calculate gains and losses
		double deltaCoins = calculator.calculateCoinDelta(distances);

		// prepare utility that is added
		double utility = 0.0;

		if (deltaCoins < 0.0) { // losses
			// calculate monetary price of coins that need to be purchased
			double purchaseCost_EUR = -deltaCoins * market.getMarketPrice_EUR_per_coin();

			// use marginal utility of cost to integrate into penalty
			utility += modeParameters.betaCost_u_MU * purchaseCost_EUR;

			// bare marginal utility from coin loss
			utility += parameters.beta_loss_u_per_coin * deltaCoins;
		} else { // gains
			// TODO: gain back money here using marginal utility of money???

			utility += parameters.beta_gain_u_per_coin * deltaCoins;
		}

		// we need to return a penalty (= inverse of added utility)
		return -utility;
	}
}
