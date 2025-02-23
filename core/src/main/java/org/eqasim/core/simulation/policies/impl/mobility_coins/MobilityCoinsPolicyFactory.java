package org.eqasim.core.simulation.policies.impl.mobility_coins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.DefaultPolicy;
import org.eqasim.core.simulation.policies.Policy;
import org.eqasim.core.simulation.policies.PolicyFactory;
import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsCalculator;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsMarket;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsParameters;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsRoutingPenalty;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsUtilityPenalty;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class MobilityCoinsPolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(MobilityCoinsPolicyFactory.class);

	static public final String POLICY_NAME = "mobilityCoins";

	private final Config config;
	private final ModeParameters modeParameters;

	private final MobilityCoinsParameters parameters;
	private final MobilityCoinsCalculator calculator;
	private final MobilityCoinsMarket market;

	public MobilityCoinsPolicyFactory(Config config,
			ModeParameters modeParameters, MobilityCoinsParameters parameters, MobilityCoinsCalculator calculator,
			MobilityCoinsMarket market) {
		this.config = config;
		this.modeParameters = modeParameters;
		this.parameters = parameters;
		this.calculator = calculator;
		this.market = market;
	}

	@Override
	public Policy createPolicy(String name, PolicyPersonFilter personFilter) {
		for (ConfigGroup item : PoliciesConfigGroup.get(config)
				.getParameterSets(MobilityCoinsPolicyFactory.POLICY_NAME)) {
			MobilityCoinsConfigGroup policyItem = (MobilityCoinsConfigGroup) item;

			if (policyItem.policyName.equals(name)) {
				return createPolicy(policyItem, personFilter);
			}
		}

		throw new IllegalStateException(
				"Configuration not found for policy " + name + " of type " + MobilityCoinsPolicyFactory.POLICY_NAME);
	}

	private Policy createPolicy(MobilityCoinsConfigGroup enterConfig, PolicyPersonFilter personFilter) {
		logger.info("Creating policy " + enterConfig.policyName + " of type " + MobilityCoinsPolicyFactory.POLICY_NAME);

		return new DefaultPolicy(
				new MobilityCoinsRoutingPenalty(modeParameters, market, calculator),
				new MobilityCoinsUtilityPenalty(modeParameters, market, calculator, parameters));
	}
}
