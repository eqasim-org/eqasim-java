package org.eqasim.ile_de_france.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.tour_finder.ActivityTourFinderWithExcludedActivities;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.BikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFMotorbikeCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFMotorbikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPassengerUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFMotorbikePredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPassengerPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class IDFModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String MOTORBIKE_COST_MODEL_NAME = "IDFMotorbikeCostModel";

	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";

	public static final String PASSENGER_ESTIMATOR_NAME = "IDFPassengerUtilityEstimator";
	public static final String BICYCLE_ESTIMATOR_NAME = "IDFBicycleUtilityEstimator";
	public static final String MOTORBIKE_ESTIMATOR_NAME = "IDFMotorbikeUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "IDFPtUtilityEstimator";

	static public final String PASSENGER = "passenger";
	static public final String MOTORBIKE = "motorbike";
	static public final String BICYCLE = "bicycle";

	public static final String ISOLATED_OUTSIDE_TOUR_FINDER_NAME = "IsolatedOutsideTrips";

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(IDFModeAvailability.class);

		bind(IDFPersonPredictor.class);
		bind(IDFMotorbikePredictor.class);
		bind(IDFPassengerPredictor.class);
		bind(IDFPtPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(IDFCarCostModel.class);
		bindCostModel(MOTORBIKE_COST_MODEL_NAME).to(IDFMotorbikeCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(IDFPtCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(IDFCarUtilityEstimator.class);
		bindUtilityEstimator(BICYCLE_ESTIMATOR_NAME).to(BikeUtilityEstimator.class);
		bindUtilityEstimator(PASSENGER_ESTIMATOR_NAME).to(IDFPassengerUtilityEstimator.class);
		bindUtilityEstimator(MOTORBIKE_ESTIMATOR_NAME).to(IDFMotorbikeUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(IDFPtUtilityEstimator.class);

		bind(ModeParameters.class).to(IDFModeParameters.class);

		bindTourFinder(ISOLATED_OUTSIDE_TOUR_FINDER_NAME).to(ActivityTourFinderWithExcludedActivities.class);
	}

	@Provides
	@Singleton
	public IDFModeParameters provideModeChoiceParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		IDFModeParameters parameters = IDFModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public IDFCostParameters provideCostParameters(EqasimConfigGroup config) {
		IDFCostParameters parameters = IDFCostParameters.buildDefault();

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
	
	@Provides
	@Named("motorbike")
	public CostModel providePtCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "motorbike");
	}
}
