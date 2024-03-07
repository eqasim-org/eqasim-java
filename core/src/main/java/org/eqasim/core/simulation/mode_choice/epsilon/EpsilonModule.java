package org.eqasim.core.simulation.mode_choice.epsilon;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.config.ModeParameterSet;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;

public class EpsilonModule extends AbstractEqasimExtension {

	public static final Logger logger = LogManager.getLogger(EpsilonModule.class);

	public static final String EPSILON_UTILITY_PREFIX = "epsilon:";

	@Provides
	public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
		return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
	}

	@Override
	protected void installEqasimExtension() {
		bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);

		EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) getConfig().getModules()
				.get(EqasimConfigGroup.GROUP_NAME);
		DiscreteModeChoiceConfigGroup dmcConfigGroup = (DiscreteModeChoiceConfigGroup) getConfig().getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		boolean usesMaximumUtilitySelector = dmcConfigGroup.getSelector().equals(SelectorModule.MAXIMUM);
		Set<String> processed = new HashSet<>();

		for (ModeParameterSet mode : eqasimConfigGroup.getModes()) {
			if (mode.isUsePseudoRandomError()) {
				if (!usesMaximumUtilitySelector) {
					logger.warn("Using epsulon utility estimator without maximum utility selection. Are you sure?");
				}

				if (processed.contains(mode.getEstimator())) {
					logger.warn(String.format(
							"The epsilon utility estimator '%s' is used for more than one mode. The seed of the epsilon generator will rely on the first mode.",
							mode.getEstimator()));
					continue;
				}

				processed.add(mode.getEstimator());

				bindUtilityEstimator(EPSILON_UTILITY_PREFIX + mode.getEstimator()).toProvider(new Provider<>() {
					@Inject
					private Map<String, Provider<UtilityEstimator>> factory;

					@Inject
					private EpsilonProvider epsilonProvider;

					@Override
					public UtilityEstimator get() {
						UtilityEstimator delegate = factory.get(mode.getEstimator()).get();
						return new EpsilonAdapter(mode.getMode(), delegate, epsilonProvider);
					}
				});
			}
		}
	}
	
	static public String getEstimator(String baseEstimator) {
		return EPSILON_UTILITY_PREFIX + baseEstimator;
	}
}
