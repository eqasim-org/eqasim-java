package org.eqasim.ile_de_france.mode_choice;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.AbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFPtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFBikeUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.estimators.IDFCarUtilityEstimator;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
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
	public static final String BIKE_ESTIMATOR_NAME = "IDFBikeUtilityEstimator";

	public IDFModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		if(getConfig().getModules().containsKey(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)) {
			bindModeAvailability(MODE_AVAILABILITY_NAME).toInstance(new ModeAvailability() {
				private IDFModeAvailability delegate = new IDFModeAvailability();
				private String mode = ((AbstractAccessModuleConfigGroup) getConfig().getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)).getModeName();
				@Override
				public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> discreteModeChoiceTrips) {
					Collection<String> modes = delegate.getAvailableModes(person, discreteModeChoiceTrips);
					modes.add(mode);
					return modes;
				}
			});
		} else {
			bindModeAvailability(MODE_AVAILABILITY_NAME).to(IDFModeAvailability.class);
		}

		bind(IDFPersonPredictor.class);

		bindCostModel(CAR_COST_MODEL_NAME).to(IDFCarCostModel.class);
		bindCostModel(PT_COST_MODEL_NAME).to(IDFPtCostModel.class);

		bindUtilityEstimator(CAR_ESTIMATOR_NAME).to(IDFCarUtilityEstimator.class);
		bindUtilityEstimator(BIKE_ESTIMATOR_NAME).to(IDFBikeUtilityEstimator.class);
		bind(IDFSpatialPredictor.class);

		bind(ModeParameters.class).to(IDFModeParameters.class);
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
}
