package org.eqasim.core.simulation.policies.impl.discount;

import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DiscountPolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	DiscountPolicyFactory provideTransitDiscountPolicyFactory(Network network, ModeParameters modeParameters,
			Map<String, CostModel> boundCostModels, EqasimConfigGroup eqasimConfig) {
		Map<String, CostModel> relevantCostModels = new HashMap<String, CostModel>();

		PoliciesConfigGroup policies = PoliciesConfigGroup.get(getConfig());
		if (policies != null) {
			for (var policy : policies.getParameterSets(DiscountPolicyFactory.POLICY_NAME)) {
				String mode = ((DiscountConfigGroup) policy).mode;

				if (boundCostModels.containsKey(mode)) {
					relevantCostModels.put(mode, boundCostModels.get(mode));
				}
			}
		}

		return new DiscountPolicyFactory(getConfig(), relevantCostModels, modeParameters);
	}
}
