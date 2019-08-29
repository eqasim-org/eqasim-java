package org.eqasim.switzerland;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.travel_times.handlers.TravelTimeHandler;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		SwitzerlandConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new SwissModeChoiceModule(cmd));

		// handlers for travel times on problematic links
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Key.get(TravelTime.class, Names.named("car"))).to(QSimFreeSpeedTravelTime.class);
			}
		});

		// links we want to track
		List<String> linkIdStrings = Arrays.asList("602433", "353721", "781183", "437648", "644662",
				"920875", "321711", "1936", "580900", "635236", "455636", "485300", "825376", "58190",
				"806135", "665248", "288481", "302670", "208899", "485299");
		Collection<Id<Link>> linkIds = new LinkedList<>();
		for (String linkIdString : linkIdStrings) {
			linkIds.add(Id.createLinkId(linkIdString));
		}

		TravelTimeHandler travelTimeHandler = new TravelTimeHandler(scenario, linkIds);
		controller.addControlerListener(travelTimeHandler);
		controller.getEvents().addHandler(travelTimeHandler);
		//

		controller.run();
	}
}


