package org.eqasim.ile_de_france.policies.limited_traffic_zone;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class LimitedTrafficZonePolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	LimitedTrafficZonePolicyFactory provideLimitedTrafficZonePolicyFactory(Network network) {
		return new LimitedTrafficZonePolicyFactory(getConfig(), network);
	}
}
