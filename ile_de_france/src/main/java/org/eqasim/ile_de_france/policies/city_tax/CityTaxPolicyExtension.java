package org.eqasim.ile_de_france.policies.city_tax;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.matsim.api.core.v01.network.Network;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CityTaxPolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	CityTaxPolicyFactory provideCityTaxPolicyFactory(Network network, IDFModeParameters modeParameters) {
		return new CityTaxPolicyFactory(getConfig(), network, modeParameters);
	}
}
