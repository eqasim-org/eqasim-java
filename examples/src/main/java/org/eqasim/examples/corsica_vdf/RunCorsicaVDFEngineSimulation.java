package org.eqasim.examples.corsica_vdf;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.eqasim.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.vdf.engine.VDFEngineModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.io.Resources;

/**
 * This is an example run script that runs the Corsica test scenario with a
 * volume-delay function to simulate travel times.
 */
public class RunCorsicaVDFEngineSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);

		URL configUrl = Resources.getResource("corsica/corsica_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, configurator.getConfigGroups());

		config.controler().setLastIteration(2);

		// VDF: Add config group
		config.addModule(new VDFConfigGroup());

		// VDF: Disable queue logic
		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		// VDF: Set capacity factor instead (~0.1 for a 10% simulation in theory... any better advice?)
		VDFConfigGroup.getOrCreate(config).setCapacityFactor(0.1);

		// VDF: Optional
		VDFConfigGroup.getOrCreate(config).setWriteInterval(1);
		VDFConfigGroup.getOrCreate(config).setWriteFlowInterval(1);

		// VDF Engine: Add config group
		config.addModule(new VDFEngineConfigGroup());

		// VDF Engine: Decide whether to genertae link events or not
		VDFEngineConfigGroup.getOrCreate(config).setGenerateNetworkEvents(false);

		// VDF Engine: Remove car from main modes
		Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
		mainModes.remove("car");
		config.qsim().setMainModes(mainModes);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		// VDF: Add modules
		controller.addOverridingModule(new VDFModule());
		controller.addOverridingQSimModule(new VDFQSimModule());

		// VDF Engine: Add modules
		controller.addOverridingModule(new VDFEngineModule());

		// VDF Engine: Active engine
		controller.configureQSimComponents(cfg -> {
			EqasimTransitQSimModule.configure(cfg, controller.getConfig());
			cfg.addNamedComponent(VDFEngineModule.COMPONENT_NAME); // here
		});

		controller.run();
	}
}
