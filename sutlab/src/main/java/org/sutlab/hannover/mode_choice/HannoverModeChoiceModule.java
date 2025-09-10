package org.sutlab.hannover.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.tour_finder.ActivityTourFinderWithExcludedActivities;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;

import org.sutlab.hannover.mode_choice.costs.HannoverCarCostModel;
import org.sutlab.hannover.mode_choice.costs.HannoverPtCostModel;
import org.sutlab.hannover.mode_choice.parameters.HannoverCostParameters;
import org.sutlab.hannover.mode_choice.parameters.HannoverModeParameters;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverBicycleUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverCarPassengerUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverCarUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.estimators.HannoverPtUtilityEstimator;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverCarPassengerPredictor;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPersonPredictor;
import org.sutlab.hannover.mode_choice.utilities.predictors.HannoverPtPredictor;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class HannoverModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "HannoverModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "HannoverCarCostModel";
	public static final String PT_COST_MODEL_NAME = "HannoverPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "HannoverCarUtilityEstimator";
	public static final String CAR_PASSENGER_ESTIMATOR_NAME = "HannoverCarPassengerUtilityEstimator";
	public static final String BICYCLE_ESTIMATOR_NAME = "HannoverBicycleUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "HannoverPtUtilityEstimator";

	static public final String CAR_PASSENGER = "car_passenger";
	static public final String BICYCLE = "bike";

	public static final String ISOLATED_OUTSIDE_TOUR_FINDER_NAME = "IsolatedOutsideTrips";

	public HannoverModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(HannoverModeAvailability.class);

		bind(HannoverPersonPredictor.class);
		bind(HannoverCarPassengerPredictor.class);
		bind(HannoverPtPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(HannoverCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(HannoverPtCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(HannoverCarUtilityEstimator.class);
		bindUtilityEstimator(BICYCLE_ESTIMATOR_NAME).to(HannoverBicycleUtilityEstimator.class);
		bindUtilityEstimator(CAR_PASSENGER_ESTIMATOR_NAME).to(HannoverCarPassengerUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(HannoverPtUtilityEstimator.class);

		bind(ModeParameters.class).to(HannoverModeParameters.class);

		bindTourFinder(ISOLATED_OUTSIDE_TOUR_FINDER_NAME).to(ActivityTourFinderWithExcludedActivities.class);
	}

	@Provides
	@Singleton
	public HannoverModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		HannoverModeParameters parameters = HannoverModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public HannoverCostParameters provideCostParameters(EqasimConfigGroup config) {
		HannoverCostParameters parameters = HannoverCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public ActivityTourFinderWithExcludedActivities provideActivityTourFinderWithExcludedActivities(
			DiscreteModeChoiceConfigGroup dmcConfig) {
		ActivityTourFinderConfigGroup config = dmcConfig.getActivityTourFinderConfigGroup();
		return new ActivityTourFinderWithExcludedActivities(List.of("outside"),
				new ActivityTourFinder(config.getActivityTypes()));
	}
}