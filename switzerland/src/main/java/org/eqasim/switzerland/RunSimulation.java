package org.eqasim.switzerland;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.switzerland.congestion.*;
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
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("config", "mode-parameter", "cost-parameter") //
				.allowOptions("use-congestion-fix", "inertia", "link-ids", "find-worst-delays") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Boolean useCongestionFix = false;

		if (cmd.hasOption("use-congestion-fix")) {
			useCongestionFix = true;
			DiscreteModeChoiceConfigGroup.getOrCreate(config).setTripEstimator("congestion");

			if (!(cmd.hasOption("inertia") && cmd.hasOption("link-ids"))) {
				throw new ConfigurationException("Inertia and link ids must be specified");
			}
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

		// add bike availability to children under 6
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().contains("freight")) {
				if ((int) person.getAttributes().getAttribute("age") <= 5) {
					person.getAttributes().putAttribute("bikeAvailability", false);
				}
			}
		}


		double inertia = Double.parseDouble(cmd.getOption("inertia").orElse("0"));
		List<Id<Link>> linkIds = new LinkedList<>();
		LinkResultsListener linkResultsListener = new LinkResultsListener(scenario.getNetwork(), linkIds, 900);

		// set up events listener
		if (useCongestionFix) {
			// set up congestion utility module
			controller.addOverridingModule(new CongestionUtilityModule(inertia));

			// get link ids
			List<String> idList = Arrays.asList(cmd.getOptionStrict("link-ids").split(","));
			for (String id : idList) {
				linkIds.add(Id.createLinkId(id));
			}
			System.out.println("Link ids to listen to : " + linkIds);

			// add listener
			controller.addControlerListener(linkResultsListener);
			controller.getEvents().addHandler(linkResultsListener);
		}

		// set up events listener
		WorstDelayListener worstDelayListener = new WorstDelayListener(scenario.getNetwork());
		if (cmd.hasOption("find-worst-delays")) {
			controller.addControlerListener(worstDelayListener);
			controller.getEvents().addHandler(worstDelayListener);
		}

		controller.run();

		// write analysis output to file
		if (useCongestionFix) {
			String analysisFile = controller.getControlerIO().getOutputPath();
			if (useCongestionFix) {
				analysisFile += "/analysis-with-fix-inertia-" + inertia + ".csv";
			} else {
				analysisFile += "/analysis-no-fix.csv";
			}
			new LinkResultsWriter(linkResultsListener.getResults()).write(analysisFile);
		}

		if (cmd.hasOption("find-worst-delays")) {
			String outputFile = controller.getControlerIO().getOutputPath();
			if (useCongestionFix) {
				outputFile += "/worst-delays-with-fix-inertia-" + inertia + ".csv";
			} else {
				outputFile += "/worst-delays-no-fix.csv";
			}
			new WorstDelayWriter(worstDelayListener.getResults()).write(outputFile);
		}

	}
}
