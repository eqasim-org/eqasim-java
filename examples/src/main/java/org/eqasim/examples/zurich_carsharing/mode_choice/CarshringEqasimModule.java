package org.eqasim.examples.zurich_carsharing.mode_choice;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.examples.zurich_carsharing.mode_choice.costs.CarsharingCostModel;
import org.eqasim.examples.zurich_carsharing.mode_choice.parameters.CarsharingCostParameters;
import org.eqasim.examples.zurich_carsharing.mode_choice.parameters.CarsharingModeParameters;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CarshringEqasimModule extends AbstractEqasimExtension {
	static public final String ZURICH_AV_MODE_AVAILABILITY_NAME = "CarsharingModeAvailability";
	static public final String CARSHARING_COST_MODEL_NAME = "freefloating";

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(ZURICH_AV_MODE_AVAILABILITY_NAME).to(CarsharingModeAvailability.class);
		bindCostModel(CARSHARING_COST_MODEL_NAME).to(CarsharingCostModel.class);

	}

	@Provides
	@Singleton
	public CarsharingCostParameters provideCarsharingCostParameters() {
		CarsharingCostParameters parameters = CarsharingCostParameters.buildDefault();

		return parameters;
	}

	@Provides
	@Singleton
	public CarsharingModeParameters provideCarsharingModeParameters() {
		CarsharingModeParameters parameters = CarsharingModeParameters.buildDefault();

		return parameters;
	}

	@Provides
	@Singleton
	public CarsharingCostModel provideCarsharingCostModel(CarsharingCostParameters costParameters) {
		return new CarsharingCostModel(costParameters);
	}
}
