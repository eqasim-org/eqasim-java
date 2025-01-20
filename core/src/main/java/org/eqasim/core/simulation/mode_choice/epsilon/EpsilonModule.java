package org.eqasim.core.simulation.mode_choice.epsilon;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Provides;

public class EpsilonModule extends AbstractEqasimExtension {

	public static final Logger logger = LogManager.getLogger(EpsilonModule.class);

	public static final String EPSILON_UTILITY_PREFIX = "epsilon_";

	@Provides
	public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
		return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
	}

	@Override
	protected void installEqasimExtension() {
		EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(getConfig());

		if (eqasimConfigGroup.getUsePseudoRandomErrors()) {
			bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);
		} else {
			bind(EpsilonProvider.class).toInstance(NoopEpsilonProvider.INSTANCE);
		}

		for (Map.Entry<String, String> entry : eqasimConfigGroup.getEstimators().entrySet()) {
			String mode = entry.getKey();
			String utilityEstimator = entry.getValue();

			if (utilityEstimator.startsWith(EPSILON_UTILITY_PREFIX)) {
				throw new IllegalStateException(String.format(
						"Estimator for %s is prefixed with %s. Use eqasim.usePseudoRandomErrors = true instead now.",
						mode, EPSILON_UTILITY_PREFIX));
			}
		}
	}
}
