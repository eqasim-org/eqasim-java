package org.eqasim.examples.zurich_adpt.mode_choice;

import java.io.File;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
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

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(ADPT_ESTIMATOR_NAME).to(AdPTUtilityEstimator.class);
		bindCostModel(ADPT_COST_MODEL_NAME).to(AdPTCostModel.class);
		bind(AdPTPredictor.class);
	}

	@Provides
	@Singleton
	public AdPTCostModel provideAdPTCostModel(ZonalVariables zonalVariables) {
		return new AdPTCostModel(zonalVariables);
	}

	@Provides
	@Singleton
	public AdPTModeParameters provideAdPTModeParameters(EqasimAvConfigGroup config) {
		AdPTModeParameters parameters = AdPTModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		return parameters;
	}

}
