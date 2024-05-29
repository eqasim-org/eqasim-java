package org.eqasim.ile_de_france.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.tour_finder.ActivityTourFinderWithExcludedActivities;
import org.eqasim.core.simulation.modes.drt.mode_choice.DrtModeAvailabilityWrapper;
import org.eqasim.core.simulation.modes.drt.mode_choice.constraints.DrtServiceTimeConstraint;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.FeederDrtModeAvailabilityWrapper;
import org.eqasim.ile_de_france.mode_choice.costs.IDFDrtCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFFeederDrtCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFBikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.modules.config.ActivityTourFinderConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class IDFModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";
	public static final String MODE_AVAILABILITY_UMIM_NAME = "IDFModeAvailabilityUMIM";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";
	public static final String DRT_COST_MODEL_NAME = "IDFDrtCostModel";
	public static final String FEEDER_DRT_COST_MODEL_NAME = "IDFFeederDrtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "IDFBikeUtilityEstimator";

	public static final String ISOLATED_OUTSIDE_TOUR_FINDER_NAME = "IsolatedOutsideTrips";

	public static final String DRT_SERVICE_TIME_CONSTRAINT_NAME = "DrtServiceTimeConstraint";

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(FeederDrtModeAvailabilityWrapper.class);
		bindModeAvailability(MODE_AVAILABILITY_UMIM_NAME).to(DrtModeAvailabilityWrapper.class);

		bind(IDFPersonPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(IDFCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(IDFPtCostModel.class);
		bindCostModel(DRT_COST_MODEL_NAME).to(IDFDrtCostModel.class);
		bindCostModel(FEEDER_DRT_COST_MODEL_NAME).to(IDFFeederDrtCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(IDFCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(IDFBikeUtilityEstimator.class);
		bind(IDFSpatialPredictor.class);

		bind(ModeParameters.class).to(IDFModeParameters.class);

		bindTourFinder(ISOLATED_OUTSIDE_TOUR_FINDER_NAME).to(ActivityTourFinderWithExcludedActivities.class);
		bindTripConstraintFactory(DRT_SERVICE_TIME_CONSTRAINT_NAME).to(DrtServiceTimeConstraint.Factory.class);
	}

	@Provides
	@Singleton
	public IDFModeParameters provideModeChoiceParameters(EqasimConfigGroup config, OutputDirectoryHierarchy outputDirectoryHierarchy)
			throws IOException, ConfigurationException {
		IDFModeParameters parameters = IDFModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-choice-parameter", commandLine, parameters);
		ParameterDefinition.writeToFile(parameters, outputDirectoryHierarchy.getOutputFilename("mode_params.yml"));
		return parameters;
	}

	@Provides
	@Singleton
	public IDFCostParameters provideCostParameters(EqasimConfigGroup config, OutputDirectoryHierarchy outputDirectoryHierarchy) throws IOException {
		IDFCostParameters parameters = IDFCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("cost-parameter", commandLine, parameters);
		ParameterDefinition.writeToFile(parameters, outputDirectoryHierarchy.getOutputFilename("cost_params.yml"));
		return parameters;
	}

	@Provides
	@Singleton
	public ActivityTourFinderWithExcludedActivities provideActivityTourFinderWithExcludedActivities(DiscreteModeChoiceConfigGroup dmcConfig) {
		ActivityTourFinderConfigGroup config = dmcConfig.getActivityTourFinderConfigGroup();
		return new ActivityTourFinderWithExcludedActivities(List.of("outside"), new ActivityTourFinder(config.getActivityTypes()));
	}

	@Provides
	public FeederDrtModeAvailabilityWrapper provideFeederDrtModeAvailabilityWrapper(Config config) {
		return new FeederDrtModeAvailabilityWrapper(config, new IDFModeAvailability());
	}

	@Provides
	public DrtModeAvailabilityWrapper provideDrtModeAvailabilityWrapper(Config config) {
		return new DrtModeAvailabilityWrapper(config, new FeederDrtModeAvailabilityWrapper(config, new IDFModeAvailability()));
	}

	@Provides
	@Singleton
	public DrtServiceTimeConstraint.Factory provideDrtServiceTimeConstraintFactory() throws ConfigurationException {
		String prefix = "drt-service-time:";
		Map<String, Map<String, List<List<Integer>>>> timeSlotsPerDrtModePerMainMode = new HashMap<>();
		for(String option: this.commandLine.getAvailableOptions()) {
			if(!option.startsWith(prefix)) {
				continue;
			}
			String[] parts = option.split(":");
			if(parts.length != 3) {
				throw new IllegalStateException(String.format("%s options should be of the form %smainMode:drtMode", prefix, prefix));
			}
			List<List<Integer>> slots = Arrays.stream(this.commandLine.getOptionStrict(option).split(";"))
					.map(slotString -> Arrays.stream(slotString.split(":")).map(Integer::parseInt).toList())
					.toList();

			timeSlotsPerDrtModePerMainMode.computeIfAbsent(parts[1], mainMode -> new HashMap<>()).computeIfAbsent(parts[2], drtMode -> slots);
		}
		return new DrtServiceTimeConstraint.Factory(timeSlotsPerDrtModePerMainMode);
	}
}
