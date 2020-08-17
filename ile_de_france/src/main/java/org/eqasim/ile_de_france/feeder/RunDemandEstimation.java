package org.eqasim.ile_de_france.feeder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Injector;

public class RunDemandEstimation {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.allowOptions("threads", "events-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		
		TravelTime travelTime = new FreeSpeedTravelTime();

		if (cmd.hasOption("events-path")) {
			RoadNetwork roadNetwork = new RoadNetwork(scenario.getNetwork());
			travelTime = RecordedTravelTime.readFromEvents(new File(cmd.getOptionStrict("events-path")), roadNetwork, 7.0 * 3600.0,
					9.0 * 3600.0, 15.0 * 60.0);
		}
		
		final TravelTime finalTravelTime = travelTime;

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						addTravelTimeBinding("car").toInstance(finalTravelTime);
					}
				})
				.build();

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		new DemandEstimator(injector, writer).run(numberOfThreads);

		writer.close();
	}
}
