package org.eqasim.core.components.traffic;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficModule extends AbstractModule {
	@Override
	public void install() {
		bind(CrossingPenalty.class).to(DefaultCrossingPenalty.class);
	}

	@Provides
	@Singleton
	public DefaultCrossingPenalty provideDefaultCrossingPenalty(Network network, EqasimConfigGroup eqasimConfig) {
		return DefaultCrossingPenalty.build(network, eqasimConfig.getCrossingPenalty());
	}
}
