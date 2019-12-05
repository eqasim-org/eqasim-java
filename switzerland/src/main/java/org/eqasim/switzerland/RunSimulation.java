package org.eqasim.switzerland;

import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.congestion.CongestionUtilityModule;
import org.eqasim.switzerland.congestion.ScenarioResultListener;
import org.eqasim.switzerland.congestion.ScenarioResultWriter;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("config", "mode-parameter", "cost-parameter") //
				.allowOptions("use-congestion-fix", "inertia") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Boolean useCongestionFix = false;

		if (cmd.hasOption("use-congestion-fix")) {
			useCongestionFix = true;
			DiscreteModeChoiceConfigGroup.getOrCreate(config).setTripEstimator("congestion");
		}

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));

		double inertia = Double.parseDouble(cmd.getOption("inertia").orElse("0"));
		controller.addOverridingModule(new CongestionUtilityModule(inertia));

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
				if ((int) person.getAttributes().getAttribute("age") <= 5) {
					person.getAttributes().putAttribute("bikeAvailability", false);
				}
			}
		}

		// add new events listener
		List<Id<Link>> linkIds = new LinkedList<>();
		linkIds.add(Id.createLinkId("602433"));
		ScenarioResultListener listener = new ScenarioResultListener(scenario.getNetwork(), linkIds, 900);
		controller.addControlerListener(listener);
		controller.run();

		// write analysis output to file
		String analysisFile = controller.getControlerIO().getOutputPath();
		if (useCongestionFix) {
			analysisFile += "/analysis-with-fix-inertia-" + inertia + ".csv";
		} else {
			analysisFile += "/analysis-no-fix.csv";
		}
		new ScenarioResultWriter(listener.getResults()).write(analysisFile);

	}
}
