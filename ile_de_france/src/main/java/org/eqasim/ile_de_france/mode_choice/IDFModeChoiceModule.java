package org.eqasim.ile_de_france.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CarPredictor;
import org.eqasim.ile_de_france.mode_choice.constraints.InitialWaitingTimeConstraint;
import org.eqasim.ile_de_france.mode_choice.constraints.SameLocationWalkConstraint;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.NantesPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFBikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarPTUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPassengerUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFPtUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

import com.google.inject.Provider;
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
	public static final String CAR_PT_ESTIMATOR_NAME = "IDFCarPTUtilityEstimator";
	public static final String PASSENGER_ESTIMATOR_NAME = "IDFPassengerUtilityEstimator";

	public static final String INITIAL_WAITING_TIME_CONSTRAINT = "InitialWaitingTimeConstraint";
	public static final String SAME_LOCATION_WALK_CONSTRAINT = "SameLocationWalkConstraint";

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
		bindUtilityEstimator(CAR_PT_ESTIMATOR_NAME).to(IDFCarPTUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(IDFBikeUtilityEstimator.class);
		bind(IDFSpatialPredictor.class);

		bindTripConstraintFactory(INITIAL_WAITING_TIME_CONSTRAINT).to(InitialWaitingTimeConstraint.Factory.class);
		bindTripConstraintFactory(SAME_LOCATION_WALK_CONSTRAINT).to(SameLocationWalkConstraint.Factory.class);

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
	}

	@Provides
	@Singleton
	public IDFCarPTUtilityEstimator provideIDFCarPTUtilityEstimator(ModeParameters parameters, CarPredictor predictor, EqasimConfigGroup eqasimConfigGroup, Map<String, Provider<UtilityEstimator>> utilityEstimatorProviders) {
		UtilityEstimator carEstimator = utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get("car")).get();
		UtilityEstimator ptEstimator = utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get("pt")).get();

		return new IDFCarPTUtilityEstimator(parameters, predictor, carEstimator, ptEstimator);
	}

	// @Provides
    // public DefaultFeederDrtUtilityEstimator provideDefaultFeederDrtUtilityEstimator(EqasimConfigGroup eqasimConfigGroup, MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup, Map<String, Provider<UtilityEstimator>> utilityEstimatorProviders) {
    //     Map<String, UtilityEstimator> ptEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.ptModeName)).get()));
    //     Map<String, UtilityEstimator> drtEstimators = multiModeFeederDrtConfigGroup.getModalElements().stream().collect(Collectors.toMap(cfg -> cfg.mode, cfg -> utilityEstimatorProviders.get(eqasimConfigGroup.getEstimators().get(cfg.accessEgressModeName)).get()));
    //     return new DefaultFeederDrtUtilityEstimator(ptEstimators, drtEstimators);
    // }

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
}
