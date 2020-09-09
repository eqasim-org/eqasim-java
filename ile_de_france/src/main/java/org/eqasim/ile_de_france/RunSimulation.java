package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.grand_paris.PersonUtilityModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowOptions("model", "urban-factor") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), IDFConfigurator.getConfigGroups());
		IDFConfigurator.configure(config);
		cmd.applyConfiguration(config);
		
		SwissRailRaptor.DIRECT_WALK_FACTOR = config.transitRouter().getDirectWalkFactor();

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		{
			double factor = cmd.getOption("urban-factor").map(Double::parseDouble).orElse(0.8);

			for (Link link : scenario.getNetwork().getLinks().values()) {
				Boolean isUrban = (Boolean) link.getAttributes().getAttribute("isUrban");

				if (isUrban != null && isUrban) {
					link.setCapacity(factor * link.getCapacity() / 0.8);
				}
			}
		}

		Controler controller = new Controler(scenario);
		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new PersonUtilityModule());
		controller.run();
	}
}