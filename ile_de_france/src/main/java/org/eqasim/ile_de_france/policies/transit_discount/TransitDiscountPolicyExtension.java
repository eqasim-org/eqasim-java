package org.eqasim.ile_de_france.policies.transit_discount;

import java.util.Map;
import java.util.Objects;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TransitDiscountPolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	TransitDiscountPolicyFactory provideTransitDiscountPolicyFactory(Network network, IDFModeParameters modeParameters,
			Map<String, CostModel> costModels, EqasimConfigGroup eqasimConfig) {
		CostModel costModel = Objects
				.requireNonNull(costModels.get(eqasimConfig.getCostModels().get(TransportMode.pt)));
		return new TransitDiscountPolicyFactory(getConfig(), costModel, modeParameters);
	}
}
