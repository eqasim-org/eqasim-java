package org.eqasim.examples.zurich_av;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

public class ZurichAvModule extends AbstractEqasimExtension {
	static public final String ZURICH_AV_MODE_AVAILABILITY_NAME = "ZurichAvModeAvailability";

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(ZURICH_AV_MODE_AVAILABILITY_NAME).to(ZurichAvModeAvailability.class);
	}
}
