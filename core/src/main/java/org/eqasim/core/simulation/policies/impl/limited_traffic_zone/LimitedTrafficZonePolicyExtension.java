package org.eqasim.core.simulation.policies.impl.limited_traffic_zone;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class LimitedTrafficZonePolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	LimitedTrafficZonePolicyFactory provideLimitedTrafficZonePolicyFactory(Network network,
			Population population, OutputDirectoryHierarchy outputHierarchy) {
		return new LimitedTrafficZonePolicyFactory(getConfig(), network, population, outputHierarchy);
	}
}
