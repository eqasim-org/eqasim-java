package org.eqasim.core.simulation.policies.impl.discount;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.DefaultPolicy;
import org.eqasim.core.simulation.policies.Policy;
import org.eqasim.core.simulation.policies.PolicyFactory;
import org.eqasim.core.simulation.policies.PolicyPersonFilter;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

public class DiscountPolicyFactory implements PolicyFactory {
	private static final Logger logger = LogManager.getLogger(DiscountPolicyFactory.class);

	static public final String POLICY_NAME = "discount";

	private final Config config;
	private final Map<String, CostModel> costModels;
	private final ModeParameters modeParameters;

	public DiscountPolicyFactory(Config config, Map<String, CostModel> costModels,
			ModeParameters modeParameters) {
		this.config = config;
		this.costModels = costModels;
		this.modeParameters = modeParameters;
	}

	@Override
	public Policy createPolicy(String name, PolicyPersonFilter personFilter) {
		for (ConfigGroup item : PoliciesConfigGroup.get(config)
				.getParameterSets(DiscountPolicyFactory.POLICY_NAME)) {
			DiscountConfigGroup policyItem = (DiscountConfigGroup) item;

			if (policyItem.policyName.equals(name)) {
				return createPolicy(policyItem, personFilter);
			}
		}

		throw new IllegalStateException(
				"Configuration not found for policy " + name + " of type " + DiscountPolicyFactory.POLICY_NAME);
	}

	private Policy createPolicy(DiscountConfigGroup discountConfig, PolicyPersonFilter personFilter) {
		logger.info("Creating policy " + discountConfig.policyName + " of type "
				+ DiscountPolicyFactory.POLICY_NAME + " for mode " + discountConfig.mode);
		logger.info("  Price factor: " + discountConfig.priceFactor);

		CostModel costModel = costModels.get(discountConfig.mode);

		if (costModel != null) {
			return new DefaultPolicy(null,
					new DiscountUtilityPenalty(discountConfig.mode, costModel, modeParameters,
							discountConfig.priceFactor, personFilter));
		} else {
			// no cost model defined so no cost to discount
			return new DefaultPolicy(null, null);
		}

	}
}
