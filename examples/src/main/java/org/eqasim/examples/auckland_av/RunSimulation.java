package org.eqasim.examples.auckland_av;

import java.io.File;
import java.io.IOException;

import org.eqasim.auckland.AucklandModule;
import org.eqasim.automated_vehicles.components.AvConfigurator;
import org.eqasim.automated_vehicles.mode_choice.AvModeChoiceModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDatabaseModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusDispatcherModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVehicleToVSGeneratorModule;
import ch.ethz.idsc.amodeus.matsim.mod.AmodeusVirtualNetworkModule;
import ch.ethz.idsc.amodeus.net.DatabaseModule;
import ch.ethz.idsc.amodeus.net.MatsimAmodeusDatabase;
import ch.ethz.idsc.amodeus.options.ScenarioOptions;
import ch.ethz.idsc.amodeus.options.ScenarioOptionsBase;
import ch.ethz.idsc.amodeus.util.io.MultiFileTools;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.framework.AVQSimModule;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "fleet-size") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		AvConfigurator.configure(config);
		cmd.applyConfiguration(config);

		// Here we customize our configuration by setting the fleet size from the
		// command line
		OperatorConfig operatorConfig = AVConfigGroup.getOrCreate(config)
				.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID);
		operatorConfig.getGeneratorConfig().setNumberOfVehicles(Integer.parseInt(cmd.getOptionStrict("fleet-size")));
		operatorConfig.getDispatcherConfig().setType("GlobalBipartiteMatchingDispatcher");

		// This *can* be used for advanced dispatchers, but GLPK must be set up
		// properly.
		// operatorConfig.getParams().put("virtualNetworkPath",
		// "aucklandVirtualNetwork");
		// operatorConfig.getParams().put("travelDataPath", "aucklandTravelData");

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		EqasimConfigurator.adjustScenario(scenario);

		// The AvConfigurator provides some convenience functions to adjust the
		// scenario. Here, we add the mode 'av' to all links that have the 'car' mode
		// and define that all links belong to one waiting time estimation group (i.e.
		// we estimate an overall waiting time average over all links).
		AvConfigurator.configureCarLinks(scenario);
		AvConfigurator.configureUniformWaitingTimeGroup(scenario);

		Controler controller = new Controler(scenario);
		EqasimConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new AucklandModule(cmd));
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new AvModeChoiceModule(cmd));
		controller.addOverridingModule(new AucklandAvModule(cmd));

		// Set up Amodeus on top of MATSim + eqasim + av
		File workingDirectory = MultiFileTools.getDefaultWorkingDirectory();
		ScenarioOptions scenarioOptions = new ScenarioOptions(workingDirectory, ScenarioOptionsBase.getDefault());

		MatsimAmodeusDatabase db = MatsimAmodeusDatabase.initialize(scenario.getNetwork(),
				new AucklandReferenceFrame());

		controller.addOverridingModule(new AVModule(false));
		controller.addOverridingModule(new AmodeusModule());
		controller.addOverridingModule(new AmodeusDispatcherModule());
		controller.addOverridingModule(new AmodeusVehicleGeneratorModule());
		controller.addOverridingModule(new AmodeusVehicleToVSGeneratorModule());
		controller.addOverridingModule(new AmodeusDatabaseModule(db));
		controller.addOverridingModule(new AmodeusVirtualNetworkModule(scenarioOptions));
		controller.addOverridingModule(new DatabaseModule());

		// This is not totally obvious, but we need to adjust the QSim components if we
		// have AVs
		controller.configureQSimComponents(configurator -> {
			EqasimTransitQSimModule.configure(configurator);
			AVQSimModule.configureComponents(configurator);
		});

		controller.run();
	}
}
