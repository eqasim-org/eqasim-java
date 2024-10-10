package org.eqasim.ile_de_france.policies.transit_discount;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.policies.DefaultPolicy;
import org.eqasim.ile_de_france.policies.PoliciesConfigGroup;
import org.eqasim.ile_de_france.policies.Policy;
import org.eqasim.ile_de_france.policies.PolicyFactory;
import org.eqasim.ile_de_france.policies.PolicyPersonFilter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class TransitDiscountPolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(TransitDiscountPolicyFactory.class);

	static public final String POLICY_NAME = "transitDiscount";

	private final Config config;
	private final CostModel costModel;
	private final IDFModeParameters modeParameters;

	public TransitDiscountPolicyFactory(Config config, CostModel costModel, IDFModeParameters modeParameters) {
		this.config = config;
		this.costModel = costModel;
		this.modeParameters = modeParameters;
	}

	@Override
	public Policy createPolicy(String name, PolicyPersonFilter personFilter) {
		for (ConfigGroup item : PoliciesConfigGroup.get(config)
				.getParameterSets(TransitDiscountPolicyFactory.POLICY_NAME)) {
			TransitDiscountConfigGroup policyItem = (TransitDiscountConfigGroup) item;

			if (policyItem.policyName.equals(name)) {
				return createPolicy(policyItem, personFilter);
			}
		}

		throw new IllegalStateException(
				"Configuration not found for policy " + name + " of type " + TransitDiscountPolicyFactory.POLICY_NAME);
	}

	private Policy createPolicy(TransitDiscountConfigGroup discountConfig, PolicyPersonFilter personFilter) {
		logger.info("Creating policy " + discountConfig.policyName + " of type "
				+ TransitDiscountPolicyFactory.POLICY_NAME);
		logger.info("  Price factor: " + discountConfig.priceFactor);

		return new DefaultPolicy(null,
				new TransitDiscountUtilityPenalty(costModel, modeParameters, discountConfig.priceFactor, personFilter));
	}
}
