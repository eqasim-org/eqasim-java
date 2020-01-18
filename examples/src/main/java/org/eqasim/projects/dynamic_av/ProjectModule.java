package org.eqasim.projects.dynamic_av;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectAvModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.ProjectModeParameters;
import org.eqasim.projects.dynamic_av.mode_choice.constraints.InfiniteHeadwayConstraint;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.ProjectAvUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.ProjectBikeUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.ProjectCarUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.ProjectPtUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.estimators.ProjectWalkUtilityEstimator;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectBikePredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPersonPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectPtPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectTripPredictor;
import org.eqasim.projects.dynamic_av.mode_choice.utilities.predictors.ProjectWalkPredictor;
import org.eqasim.projects.dynamic_av.pricing.PricingModule;
import org.eqasim.projects.dynamic_av.service_area.OperatingArea;
import org.eqasim.projects.dynamic_av.service_area.ProjectNetworkFilter;
import org.eqasim.projects.dynamic_av.waiting_time.ProjectWaitingTimeFactory;
import org.eqasim.projects.dynamic_av.waiting_time.WaitingTimeAnalysisListener;
import org.eqasim.projects.dynamic_av.waiting_time.WaitingTimeComparisonListener;
import org.eqasim.projects.dynamic_av.waiting_time.WaitingTimeWriter;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.network.AVNetworkFilter;
import ch.ethz.matsim.av.waiting_time.WaitingTime;
import ch.ethz.matsim.av.waiting_time.WaitingTimeFactory;

public class ProjectModule extends AbstractEqasimExtension {
	static public final String PROJECT_MODE_AVAILABILITY_NAME = "ProjectModeAvailability";

	static public final String PROJECT_CAR_ESTIMATOR = "ProjectCarEstimator";
	static public final String PROJECT_PT_ESTIMATOR = "ProjectPtEstimator";
	static public final String PROJECT_BIKE_ESTIMATOR = "ProjectBikeEstimator";
	static public final String PROJECT_WALK_ESTIMATOR = "ProjectWalkEstimator";
	static public final String PROJECT_AV_ESTIMATOR = "ProjectAvEstimator";

	private final CommandLine commandLine;

	public ProjectModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(PROJECT_MODE_AVAILABILITY_NAME).to(ProjectModeAvailability.class);

		bindUtilityEstimator(PROJECT_CAR_ESTIMATOR).to(ProjectCarUtilityEstimator.class);
		bindUtilityEstimator(PROJECT_PT_ESTIMATOR).to(ProjectPtUtilityEstimator.class);
		bindUtilityEstimator(PROJECT_BIKE_ESTIMATOR).to(ProjectBikeUtilityEstimator.class);
		bindUtilityEstimator(PROJECT_WALK_ESTIMATOR).to(ProjectWalkUtilityEstimator.class);
		bindUtilityEstimator(PROJECT_AV_ESTIMATOR).to(ProjectAvUtilityEstimator.class);

		bind(ProjectBikePredictor.class);
		bind(ProjectWalkPredictor.class);
		bind(ProjectPtPredictor.class);
		bind(ProjectPersonPredictor.class);
		bind(ProjectTripPredictor.class);

		bind(WaitingTimeFactory.class).to(ProjectWaitingTimeFactory.class);
		bind(AVNetworkFilter.class).to(ProjectNetworkFilter.class);

		addControlerListenerBinding().to(WaitingTimeAnalysisListener.class);

		install(new PricingModule(commandLine));

		bindTripConstraintFactory(InfiniteHeadwayConstraint.NAME).to(InfiniteHeadwayConstraint.Factory.class);

		addEventHandlerBinding().to(WaitingTimeComparisonListener.class);
		addControlerListenerBinding().to(WaitingTimeComparisonListener.class);
	}

	@Provides
	@Singleton
	public ProjectModeParameters provideProjectModeParameters(EqasimConfigGroup config)
			throws IOException, ConfigurationException {
		ProjectModeParameters parameters = ProjectModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public ProjectAvModeParameters provideProjectAvModeParameters(EqasimAvConfigGroup config)
			throws IOException, ConfigurationException {
		ProjectAvModeParameters parameters = ProjectAvModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("av-mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public OperatingArea provideOperatingArea(Config config, ProjectConfigGroup projectConfig, Network network) {
		return OperatingArea.load(projectConfig.getWaitingTimeGroupIndexAttribute(), network,
				ConfigGroup.getInputFileURL(config.getContext(), projectConfig.getOperatingAreaPath()));
	}

	@Provides
	@Singleton
	public ProjectNetworkFilter provideProjectNetworkFilter(OperatingArea operatingArea) {
		return new ProjectNetworkFilter(operatingArea);
	}

	@Provides
	@Singleton
	public WaitingTimeWriter provideWaitingTimeWriter(Map<Id<AVOperator>, WaitingTime> waitingTimes,
			OperatingArea operatingArea, Map<Id<AVOperator>, Network> networks, AVConfigGroup config) {
		WaitingTime waitingTime = waitingTimes.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		Network network = networks.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		WaitingTimeConfig waitingTimeConfig = config.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID)
				.getWaitingTimeConfig();

		return new WaitingTimeWriter(waitingTime, operatingArea, network, waitingTimeConfig);
	}

	@Provides
	@Singleton
	public OVGKCalculator provideOVGKCalculator(TransitSchedule transitSchedule) {
		return new OVGKCalculator(transitSchedule);
	}

	@Provides
	@Singleton
	public WaitingTimeComparisonListener provideWaitingTimeComparisonListener(
			Map<Id<AVOperator>, WaitingTime> waitingTimes, OutputDirectoryHierarchy outputHierarchy) {
		WaitingTime waitingTime = waitingTimes.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		return new WaitingTimeComparisonListener(outputHierarchy, waitingTime);
	}
}
