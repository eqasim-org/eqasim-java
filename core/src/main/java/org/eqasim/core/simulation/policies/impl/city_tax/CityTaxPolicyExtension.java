package org.eqasim.core.simulation.policies.impl.city_tax;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CityTaxPolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
	}

	@Provides
	@Singleton
	CityTaxPolicyFactory provideCityTaxPolicyFactory(Network network, OutputDirectoryHierarchy outputHierarchy,
			ModeParameters modeParameters) {
		return new CityTaxPolicyFactory(getConfig(), network, outputHierarchy, modeParameters);
	}
}
