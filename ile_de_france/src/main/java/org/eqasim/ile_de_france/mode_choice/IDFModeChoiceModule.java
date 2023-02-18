package org.eqasim.ile_de_france.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.epsilon.UniformEpsilonProvider;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.mode_choice.constraints.InitialWaitingTimeConstraint;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.NantesPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFBikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPassengerUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.vdf.VDFTravelTime;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contribs.discrete_mode_choice.components.estimators.AbstractTripRouterEstimator.PreroutingLogic;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class IDFModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	public static final String MODE_AVAILABILITY_NAME = "IDFModeAvailability";

	public static final String CAR_COST_MODEL_NAME = "IDFCarCostModel";
	public static final String PT_COST_MODEL_NAME = "IDFPtCostModel";

	public static final String CAR_ESTIMATOR_NAME = "IDFCarUtilityEstimator";
	public static final String PT_ESTIMATOR_NAME = "IDFPtUtilityEstimator";
	public static final String BIKE_ESTIMATOR_NAME = "IDFBikeUtilityEstimator";
	public static final String PASSENGER_ESTIMATOR_NAME = "IDFPassengerUtilityEstimator";

	public static final String INITIAL_WAITING_TIME_CONSTRAINT = "InitialWaitingTimeConstraint";

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(MODE_AVAILABILITY_NAME).to(IDFModeAvailability.class);

		bind(IDFPersonPredictor.class);
		bind(IDFPtPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(IDFCarCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(IDFCarUtilityEstimator.class);
		bindUtilityEstimator(PT_ESTIMATOR_NAME).to(IDFPtUtilityEstimator.class);
		bindUtilityEstimator(PASSENGER_ESTIMATOR_NAME).to(IDFPassengerUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(IDFBikeUtilityEstimator.class);
		bind(IDFSpatialPredictor.class);

		bindTripConstraintFactory(INITIAL_WAITING_TIME_CONSTRAINT).to(InitialWaitingTimeConstraint.Factory.class);

		bind(ModeParameters.class).to(IDFModeParameters.class);

		String costModel = commandLine.getOption("cost-model").orElse("idf");

		switch (costModel) {
		case "idf":
			bindCostModel(PT_COST_MODEL_NAME).to(IDFPtCostModel.class);
			break;
		case "nantes":
			bindCostModel(PT_COST_MODEL_NAME).to(NantesPtCostModel.class);
			break;
		default:
			throw new IllegalStateException();
		}

		bind(PreroutingLogic.class).to(IDFPreroutingLogic.class);
		addControlerListenerBinding().to(IDFPreroutingLogic.class);
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
	public InitialWaitingTimeConstraint.Factory provideInitialWaitingTimeConstraintFactory() {
		double maximumInitialWaitingTime_min = 15.0;
		return new InitialWaitingTimeConstraint.Factory(maximumInitialWaitingTime_min);
	}

	@Provides
	@Singleton
	public IDFPreroutingLogic provideIDFPreroutingVoter(Network network, VDFTravelTime travelTime) {
		UniformEpsilonProvider epsilon = new UniformEpsilonProvider(getConfig().global().getRandomSeed());
		Set<String> routingModes = Set.of("car", "car_passenger");
		return new IDFPreroutingLogic(epsilon, routingModes, travelTime, network);
	}
}
