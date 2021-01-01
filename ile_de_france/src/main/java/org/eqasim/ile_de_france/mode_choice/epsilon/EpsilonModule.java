package org.eqasim.ile_de_france.mode_choice.epsilon;

import org.matsim.core.config.groups.GlobalConfigGroup;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class EpsilonModule extends AbstractDiscreteModeChoiceExtension {
	@Provides
	public GumbelEpsilonProvider provideGumbelEpsilonProvider(GlobalConfigGroup config) {
		return new GumbelEpsilonProvider(config.getRandomSeed(), 1.0);
	}

	@Provides
	@Singleton
	public EpsilonSelector.Factory provideEpsilonSelectorFactory(EpsilonProvider epsilonProvider) {
		return new EpsilonSelector.Factory(epsilonProvider);
	}

	@Override
	protected void installExtension() {
		bind(EpsilonProvider.class).to(GumbelEpsilonProvider.class);
		bindTourSelectorFactory(EpsilonSelector.NAME).to(EpsilonSelector.Factory.class);
	}
}
