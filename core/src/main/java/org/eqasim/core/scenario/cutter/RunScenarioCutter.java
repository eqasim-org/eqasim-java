package org.eqasim.core.scenario.cutter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.scenario.cutter.facilities.CleanHomeFacilities;
import org.eqasim.core.scenario.cutter.facilities.FacilitiesCutter;
import org.eqasim.core.scenario.cutter.network.MinimumNetworkFinder;
import org.eqasim.core.scenario.cutter.network.NetworkCutter;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.cutter.outside.OutsideActivityAdapter;
import org.eqasim.core.scenario.cutter.population.CleanHouseholds;
import org.eqasim.core.scenario.cutter.population.PopulationCutter;
import org.eqasim.core.scenario.cutter.population.PopulationCutterModule;
import org.eqasim.core.scenario.cutter.population.RemoveEmptyPlans;
import org.eqasim.core.scenario.cutter.transit.DefaultStopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.StopSequenceCrossingPointFinder;
import org.eqasim.core.scenario.cutter.transit.TransitScheduleCutter;
import org.eqasim.core.scenario.cutter.transit.TransitVehiclesCutter;
import org.eqasim.core.scenario.routing.PopulationRouter;
import org.eqasim.core.scenario.routing.PopulationRouterModule;
import org.eqasim.core.scenario.validation.ScenarioValidator;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Injector;

public class RunScenarioCutter {
	static public void main(String[] args)
			throws ConfigurationException, MalformedURLException, IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path", "extent-path") //
				.allowOptions("threads", "prefix", "extent-attribute", "extent-value") //
				.build();

		// Load some configuration
		String prefix = cmd.getOption("prefix").orElse("");
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		File outputDirectory = new File(cmd.getOptionStrict("output-path")).getAbsoluteFile();
		ScenarioWriter.checkOutputDirectory(outputDirectory);

		// Load scenario extent
		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		// Load scenario
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		// Check validity before cutting
		ScenarioValidator scenarioValidator = new ScenarioValidator();
		scenarioValidator.checkScenario(scenario);

		// Prepare road network
		RoadNetwork roadNetwork = new RoadNetwork(scenario.getNetwork());

		// Cut population
		Injector populationCutterInjector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new PopulationCutterModule(extent, numberOfThreads, 40)) //
				.build();

		PopulationCutter populationCutter = populationCutterInjector.getInstance(PopulationCutter.class);
		populationCutter.run(scenario.getPopulation());

		// ... and remove empty plans
		RemoveEmptyPlans removeEmptyPlans = new RemoveEmptyPlans();
		removeEmptyPlans.run(scenario.getPopulation());

		// ... and make outside activities consistent
		OutsideActivityAdapter outsideActivityAdapter = new OutsideActivityAdapter(roadNetwork);
		outsideActivityAdapter.run(scenario.getPopulation(), scenario.getActivityFacilities());

		// ... and make households consistent
		CleanHouseholds cleanHouseholds = new CleanHouseholds(scenario.getPopulation());
		cleanHouseholds.run(scenario.getHouseholds());

		// Cut transit
		StopSequenceCrossingPointFinder stopSequenceCrossingPointFinder = new DefaultStopSequenceCrossingPointFinder(
				extent);
		TransitScheduleCutter transitScheduleCutter = new TransitScheduleCutter(extent,
				stopSequenceCrossingPointFinder);
		transitScheduleCutter.run(scenario.getTransitSchedule());

		TransitVehiclesCutter transitVehiclesCutter = new TransitVehiclesCutter(scenario.getTransitSchedule());
		transitVehiclesCutter.run(scenario.getTransitVehicles());

		// Cut facilities
		CleanHomeFacilities cleanHomeFacilities = new CleanHomeFacilities(scenario.getPopulation());
		cleanHomeFacilities.run(scenario.getActivityFacilities());

		FacilitiesCutter facilitiesCutter = new FacilitiesCutter(extent, scenario.getPopulation());
		facilitiesCutter.run(scenario.getActivityFacilities(), true);

		// Cut network
		MinimumNetworkFinder minimumNetworkFinder = new MinimumNetworkFinder(extent, roadNetwork, numberOfThreads, 20);
		NetworkCutter networkCutter = new NetworkCutter(extent, scenario, minimumNetworkFinder);
		networkCutter.run(scenario.getNetwork());

		// "Cut" config
		// (we need to reload it, because it has become locked at this point)
		config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
		ConfigCutter configCutter = new ConfigCutter(prefix);
		configCutter.run(config);

		// Final routing
		Injector routingInjector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, 100, false)) //
				.build();

		PopulationRouter router = routingInjector.getInstance(PopulationRouter.class);
		router.run(scenario.getPopulation());

		// Check validity after cutting
		scenarioValidator.checkScenario(scenario);

		// Write scenario
		ScenarioWriter scenarioWriter = new ScenarioWriter(config, scenario, prefix);
		scenarioWriter.run(outputDirectory);
	}
}
