package org.eqasim.examples.corsica_carpooling;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.corsica_carpooling.conflicts.CarpoolingConflictLogic;
import org.eqasim.examples.corsica_carpooling.mode_choice.CarpoolingModeAvailability;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.ConflictModule;
import org.eqasim.ile_de_france.discrete_mode_choice.conflicts.logic.ConflictLogic;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.io.Resources;

public class RunCorsicaCarpoolingSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();
		URL configUrl = Resources.getResource("corsica/corsica_config.xml");

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());

		config.controler().setLastIteration(20);
		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);
		cmd.applyConfiguration(config);

		{ // Add the carpooling mode to the choice model
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

			// Add DRT to the available modes
			dmcConfig.setModeAvailability(CarpoolingModeAvailability.NAME);

			// Add DRT to cached modes
			Set<String> cachedModes = new HashSet<>();
			cachedModes.addAll(dmcConfig.getCachedModes());
			cachedModes.add("carpooling");
			dmcConfig.setCachedModes(cachedModes);

			// Set up choice model
			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			eqasimConfig.setCostModel("carpooling", "carpooling");
			eqasimConfig.setEstimator("carpooling", "carpooling");

			// Set analysis interval
			eqasimConfig.setAnalysisInterval(1);

			// Add as network mode
			Set<String> networkModes = new HashSet<>(config.plansCalcRoute().getNetworkModes());
			networkModes.add("carpooling");
			config.plansCalcRoute().setNetworkModes(networkModes);
		}

		{ // Set up some defaults for MATSim scoring
			ModeParams modeParams = new ModeParams("carpooling");
			config.planCalcScore().addModeParams(modeParams);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		new CarpoolingConfigurator().configureNetwork(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		{ // Add overrides for Corsica + carpooking
			controller.addOverridingModule(new CarpoolingModule(cmd));
		}

		// Add conflict logic
		controller.addOverridingModule(new ConflictModule());
		ConflictModule.configure(DiscreteModeChoiceConfigGroup.getOrCreate(config));

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ConflictLogic.class).toInstance(new CarpoolingConflictLogic());
			}
		});

		controller.run();
	}
}
