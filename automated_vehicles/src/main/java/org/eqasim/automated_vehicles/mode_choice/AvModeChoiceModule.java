package org.eqasim.automated_vehicles.mode_choice;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.automated_vehicles.mode_choice.constraints.AvWalkConstraint;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostListener;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostModel;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostParameters;
import org.eqasim.automated_vehicles.mode_choice.cost.AvCostWriter;
import org.eqasim.automated_vehicles.mode_choice.mode_parameters.AvModeParameters;
import org.eqasim.automated_vehicles.mode_choice.utilities.estimators.AvUtilityEstimator;
import org.eqasim.automated_vehicles.mode_choice.utilities.predictors.AvPredictor;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.analysis.LinkFinder;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;

public class AvModeChoiceModule extends AbstractEqasimExtension {
	static public final String AV_ESTIMATOR_NAME = "AvEstimator";
	static public final String AV_COST_MODEL_NAME = "AvCostModel";
	static public final String AV_WALK_CONSTRAINT_NAME = "AvWalkConstraint";

	private final CommandLine commandLine;

	public AvModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(AV_ESTIMATOR_NAME).to(AvUtilityEstimator.class);
		bindCostModel(AV_COST_MODEL_NAME).to(AvCostModel.class);
		bind(AvPredictor.class);

		bindTripConstraintFactory(AV_WALK_CONSTRAINT_NAME).to(AvWalkConstraint.Factory.class);

		addEventHandlerBinding().to(FleetDistanceListener.class);
		addControlerListenerBinding().to(AvCostListener.class);
		addControlerListenerBinding().to(AvCostWriter.class);
	}

	@Provides
	@Singleton
	public AvCostModel provideAvCostModel(AvCostListener listener) {
		return new AvCostModel(listener);
	}

	@Provides
	@Singleton
	public AvCostWriter provideAvCostWriter(AvCostListener listener, OutputDirectoryHierarchy outputHierarchy) {
		File outputPath = new File(outputHierarchy.getOutputFilename("av_price.csv"));
		return new AvCostWriter(outputPath, listener);
	}

	@Provides
	@Singleton
	public AvCostListener provideAvCostListener(AvCostParameters parameters, FleetDistanceListener distanceListener,
			AVConfigGroup config, EqasimConfigGroup eqasimConfig) {
		int numberOfVehicles = config.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID).getGeneratorConfig()
				.getNumberOfVehicles();

		return new AvCostListener(parameters, distanceListener, numberOfVehicles);
	}

	@Provides
	@Singleton
	public AvCostParameters provideAvCostParameters(EqasimAvConfigGroup config) {
		AvCostParameters parameters = AvCostParameters.buildDefault();

		if (config.getCostParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getCostParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("av-cost-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public AvModeParameters provideAvModeParameters(EqasimAvConfigGroup config) {
		AvModeParameters parameters = AvModeParameters.buildDefault();

		if (config.getModeParametersPath() != null) {
			ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
		}

		ParameterDefinition.applyCommandLine("av-mode-parameter", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Named("av")
	public CostModel provideCarCostModel(Map<String, Provider<CostModel>> factory, EqasimConfigGroup config) {
		return getCostModel(factory, config, "av");
	}

	@Provides
	@Singleton
	public FleetDistanceListener provideFleetDistanceListener(Network network) {
		LinkFinder linkFinder = new LinkFinder(network);
		return new FleetDistanceListener(Collections.singleton(OperatorConfig.DEFAULT_OPERATOR_ID), linkFinder);
	}
}
