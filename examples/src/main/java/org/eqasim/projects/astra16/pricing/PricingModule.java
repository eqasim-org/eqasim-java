package org.eqasim.projects.astra16.pricing;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

public class PricingModule extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
		bindCostModel(AstraAvCostModel.NAME).to(AstraAvCostModel.class);
	}
}
