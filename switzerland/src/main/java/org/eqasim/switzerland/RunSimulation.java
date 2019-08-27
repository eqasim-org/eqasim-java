package org.eqasim.switzerland;

import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.travel_times.handlers.TravelTimeHandler;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Collection;
import java.util.LinkedList;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
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
		Vehicle2DriverEventHandler vehicle2DriverEventHandler = new Vehicle2DriverEventHandler();
		controller.getEvents().addHandler(vehicle2DriverEventHandler);

		Collection<Id<Link>> linkIds = new LinkedList<>();
		linkIds.add(Id.createLinkId("602433"));

		TravelTimeHandler travelTimeHandler = new TravelTimeHandler(scenario, linkIds, vehicle2DriverEventHandler);
		controller.addControlerListener(travelTimeHandler);
		controller.getEvents().addHandler(travelTimeHandler);
		//

		controller.run();
	}
}


