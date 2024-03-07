package org.eqasim.core.simulation.mode_choice.epsilon;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.core.config.groups.GlobalConfigGroup;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EpsilonModule extends AbstractEqasimExtension {

	public static final Logger logger = LogManager.getLogger(EpsilonModule.class);

	public static final String EPSILON_UTILITY_PREFIX = "epsilon_";
	@Provides
	public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
		return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
	}

	@Override
	protected void installEqasimExtension() {
		bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);


		EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) getConfig().getModules().get(EqasimConfigGroup.GROUP_NAME);
		Set<String> processed = new HashSet<>();
		for(Map.Entry<String, String > entry: eqasimConfigGroup.getEstimators().entrySet()) {
			String mode = entry.getKey();
			String utilityEstimator = entry.getValue();
			if(utilityEstimator.startsWith(EPSILON_UTILITY_PREFIX)) {
				if(processed.contains(utilityEstimator)) {
					logger.warn(String.format("The epsilon utility estimator '%s' is used for more than one mode. The seed of the epsilon generator will rely on the first mode", utilityEstimator));
					continue;
				}
				processed.add(utilityEstimator);
				String baseEstimator = utilityEstimator.substring(EPSILON_UTILITY_PREFIX.length());
				bindUtilityEstimator(utilityEstimator).toProvider(new Provider<>() {
                    @Inject
                    private Map<String, Provider<UtilityEstimator>> factory;

                    @Inject
                    private EpsilonProvider epsilonProvider;

                    @Override
                    public UtilityEstimator get() {
                        UtilityEstimator delegate = factory.get(baseEstimator).get();
                        return new EpsilonAdapter(mode, delegate, epsilonProvider);
                    }
                });
			}
		}
	}
}
