package org.eqasim.sao_paulo.scenario;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.sao_paulo.mode_choice.SaoPauloModeChoiceModule;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloModeParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		ConfigAdapter.run(args, new EqasimConfigurator(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config, String prefix) {
		// Ignore some input files
		// config.transit().setVehiclesFile(null);
		// config.households().setInputFile(null);

		// Set up mode choice
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, SaoPauloModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SaoPauloModeChoiceModule.PT_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.taxi, SaoPauloModeChoiceModule.TAXI_COST_MODEL_NAME);

		eqasimConfig.setCrossingPenalty(8.0);
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SaoPauloModeChoiceModule.MODE_AVAILABILITY_NAME);
		Collection<String> tripConstraints = dmcConfig.getTripConstraints();
		tripConstraints.add("WalkDurationConstraint");
		dmcConfig.setTripConstraints(tripConstraints);

		List<String> networkModes = new LinkedList<>(config.routing().getNetworkModes());
		networkModes.add("taxi");
		config.routing().setNetworkModes(networkModes);

		ModeParams taxiParams = new ModeParams("taxi");
		config.scoring().addModeParams(taxiParams);

		// update the scoring parameters used in pt routing
		config.scoring().setMarginalUtlOfWaitingPt_utils_hr(
				SaoPauloModeParameters.buildDefault().pt.betaWaitingTime_u_min * 60.0);
		config.scoring().setPerforming_utils_hr(0.0);
		config.scoring().setUtilityOfLineSwitch(0.0);

		ModeParams ptParams = new ModeParams("pt");
		ptParams.setMarginalUtilityOfTraveling(SaoPauloModeParameters.buildDefault().pt.betaInVehicleTime_u_min * 60.0);

		config.scoring().addModeParams(ptParams);

		ModeParams walkParams = new ModeParams("walk");
		walkParams.setMarginalUtilityOfTraveling(
				SaoPauloModeParameters.buildDefault().pt.betaAccessEgressTime_u_min * 60.0);

		config.scoring().addModeParams(walkParams);

		// update transit parameters
		config.transitRouter().setDirectWalkFactor(100.0);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(400.0);
		config.transitRouter().setSearchRadius(1300.0);

	}
}
