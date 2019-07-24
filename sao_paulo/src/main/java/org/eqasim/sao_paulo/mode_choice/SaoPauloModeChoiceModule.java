package org.eqasim.sao_paulo.mode_choice;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;

import ch.ethz.matsim.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

public class SaoPauloModeChoiceModule extends AbstractDiscreteModeChoiceExtension {
	@Override
	protected void installExtension() {
		bindModeAvailability(EqasimModeChoiceModule.MODE_AVAILABILITY_NAME).to(SaoPauloModeAvailability.class);
	}
}
