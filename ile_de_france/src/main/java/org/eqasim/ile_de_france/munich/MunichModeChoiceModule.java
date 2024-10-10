package org.eqasim.ile_de_france.munich;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;

public class MunichModeChoiceModule extends AbstractEqasimExtension {
	public static final String PT_COST_MODEL_NAME = "MunichPtCostModel";

	@Override
	protected void installEqasimExtension() {
		bindCostModel(PT_COST_MODEL_NAME).to(MunichPtCostModel.class);
	}
}
