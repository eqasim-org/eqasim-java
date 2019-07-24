package org.eqasim.sao_paulo.mode_choice;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.utilities.IDFUtilityEstimator;
import org.matsim.core.config.CommandLine;

public class SaoPauloModeChoiceModule extends IDFModeChoiceModule {
	public SaoPauloModeChoiceModule(CommandLine commandLine) {
		super(commandLine);
	}

	@Override
	protected void installExtension() {
		bindTripEstimator(EqasimModeChoiceModule.UTILITY_ESTIMATOR_NAME).to(IDFUtilityEstimator.class);
		bindModeAvailability(EqasimModeChoiceModule.MODE_AVAILABILITY_NAME).to(SaoPauloModeAvailability.class);
	}
}
