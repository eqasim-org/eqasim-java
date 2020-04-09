package org.eqasim.examples.zurich_adpt.mode_choice;

import java.io.File;

import org.eqasim.automated_vehicles.mode_choice.constraints.AvWalkConstraint;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.examples.zurich_adpt.mode_choice.constraints.AdPTConstraint;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.AdPTCostModel;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.mode_parameters.AdPTModeParameters;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.estimators.AdPTUtilityEstimator;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.predictors.AdPTPredictor;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AdPTModeChoiceModule extends AbstractEqasimExtension {
	static public final String ADPT_ESTIMATOR_NAME = "AdPTEstimator";
	static public final String ADPT_COST_MODEL_NAME = "AdPTCostModel";
	static public final String ADPT_CONSTRAINT_NAME = "AdptConstraint";

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(ADPT_ESTIMATOR_NAME).to(AdPTUtilityEstimator.class);
		bindCostModel(ADPT_COST_MODEL_NAME).to(AdPTCostModel.class);
		bind(AdPTPredictor.class);
		bindTripConstraintFactory(ADPT_CONSTRAINT_NAME).to(AdPTConstraint.Factory.class);

	}

	@Provides
	@Singleton
	public AdPTCostModel provideAdPTCostModel(ZonalVariables zonalVariables) {
		return new AdPTCostModel(zonalVariables);
	}

	@Provides
	@Singleton
	public AdPTModeParameters provideAdPTModeParameters() {
		AdPTModeParameters parameters = AdPTModeParameters.buildDefault();		

		return parameters;
	}

}
