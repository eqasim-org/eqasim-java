package org.eqasim.ile_de_france;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.RunInsertVehicles;
import org.eqasim.core.scenario.cutter.RunScenarioCutter;
import org.eqasim.core.standalone_mode_choice.RunStandaloneModeChoice;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class TestCorisica {
	@Before
	public void setUp() throws IOException {
		URL fixtureUrl = getClass().getClassLoader().getResource("corsica");
		FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("corsica_test"));
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(new File("corsica_test"));
	}

	private void adjustConfig() throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(new String[0]).build();
		IDFConfigurator configurator = new IDFConfigurator(cmd);
		Config config = ConfigUtils.loadConfig("corsica_test/corsica_config.xml");
		configurator.updateConfig(config);
		config.vehicles().setVehiclesFile("corsica_vehicles.xml.gz");
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setEstimator("bike", IDFModeChoiceModule.BICYCLE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator("bicycle", IDFModeChoiceModule.BICYCLE_ESTIMATOR_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
		dmcConfig.setModeAvailability(IDFModeChoiceModule.MODE_AVAILABILITY_NAME);

		RoutingConfigGroup routingConfig = config.routing();

		TeleportedModeParams bicycleRoutingParams = new TeleportedModeParams("bicycle");
		bicycleRoutingParams.setBeelineDistanceFactor(1.3);
		bicycleRoutingParams.setTeleportedModeSpeed(9.3 / 3.6);
		routingConfig.addTeleportedModeParams(bicycleRoutingParams);

		ScoringConfigGroup scoringConfig = config.scoring();
		ModeParams bicycleScoringParams = new ModeParams("bicycle");
		scoringConfig.addModeParams(bicycleScoringParams);

		new ConfigWriter(config).write("corsica_test/corsica_config.xml");
	}

	@Test
	public void testCorsicaPipeline()
			throws ConfigurationException, InterruptedException, MalformedURLException, IOException, ExecutionException {

		Assert.assertEquals(389, countPersons("corsica_test/corsica_population.xml.gz"));

		applyFixes("corsica_test/corsica_population.xml.gz");

		// Run the simulation
		{
			adjustConfig();
			
			RunInsertVehicles.main(new String[] { //
					"--config-path", "corsica_test/corsica_config.xml", //
					"--input-population-path", "corsica_test/corsica_population.xml.gz", //
					"--output-population-path", "corsica_test/corsica_population.xml.gz", //
					"--output-vehicles-path", "corsica_test/corsica_vehicles.xml.gz", //
			});
			
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/corsica_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/simulation_output", //
					"--config:eqasim.travelTimeRecordingInterval", "1000", //
			});

			Assert.assertEquals(389, countPersons("corsica_test/simulation_output/output_plans.xml.gz"));

			Map<String, Long> counts = countLegs("corsica_test/simulation_output/output_events.xml.gz");
			Assert.assertEquals(989, (long) counts.get("car"));
			Assert.assertEquals(129, (long) counts.get("car_passenger"));
			Assert.assertEquals(222, (long) counts.get("walk"));
			Assert.assertEquals(0, (long) counts.getOrDefault("bike", 0L));
			Assert.assertEquals(5, (long) counts.getOrDefault("bicycle", 0L));
			Assert.assertEquals(5, (long) counts.get("pt"));
		}

		// Run the mode choice + following simulation
		{
			RunStandaloneModeChoice.main(new String[]{
					"--config-path", "corsica_test/corsica_config.xml",
					"--recorded-travel-times-path", "corsica_test/simulation_output/eqasim_travel_times.bin.gz",
					"--write-input-csv-trips", "true",
					"--write-output-csv-trips", "true",
					"--config:standaloneModeChoice.outputDirectory", "corsica_test/mode_choice_output",
					"--eqasim-configurator", IDFConfigurator.class.getName(),
					"--simulate-after", RunSimulation.class.getName()
			});
		}

		// Cut the scenario based on output plans
		{			
			RunScenarioCutter.main(new String[] { //
					"--config-path", "corsica_test/corsica_config.xml", //
					"--extent-path", "corsica_test/extent.shp", //
					"--threads", "4", //
					"--prefix", "cut_", //
					"--output-path", "corsica_test", //
					"--eqasim-configurator", IDFConfigurator.class.getName()
			});

			Assert.assertEquals(171, countPersons("corsica_test/cut_population.xml.gz"));
		}

		// Run the cut simulation
		{			
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/cut_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/cut_output", //
					"--config:eqasim.travelTimeRecordingInterval", "1000", //
			});

			Map<String, Long> counts = countLegs("corsica_test/cut_output/output_events.xml.gz");
			Assert.assertEquals(422, (long) counts.get("car"));
			Assert.assertEquals(53, (long) counts.get("car_passenger"));
			Assert.assertEquals(102, (long) counts.get("walk"));
			Assert.assertEquals(0, (long) counts.getOrDefault("bike", 0L));
			Assert.assertEquals(1, (long) counts.getOrDefault("bicycle", 0L));
			Assert.assertEquals(0, (long) counts.getOrDefault("pt", 0L));
			Assert.assertEquals(6, (long) counts.get("outside"));
		}

		{
			RunStandaloneModeChoice.main(new String[] {
					"--config-path", "corsica_test/cut_config.xml",
					"--recorded-travel-times-path", "corsica_test/cut_output/eqasim_travel_times.bin.gz",
					"--config:DiscreteModeChoice.tourFinder", "IsolatedOutsideTrips",
					"--config:standaloneModeChoice.outputDirectory", "corsica_test/cut_output_mode_choice",
					"--config:standaloneModeChoice.removePersonsWithNoValidAlternatives", "true",
					"--write-input-csv-trips", "true",
					"--write-output-csv-trips", "true",
					"--eqasim-configurator", IDFConfigurator.class.getCanonicalName(),
					"--simulate-after", RunSimulation.class.getName()
			});
		}
	}

	static Map<String, Long> countLegs(String eventsPath) {
		EventsManager manager = EventsUtils.createEventsManager();

		Map<String, Long> counts = new HashMap<>();
		manager.addHandler((PersonDepartureEventHandler) event -> {
			counts.compute(event.getLegMode(), (k, v) -> v == null ? 1 : v + 1);
		});

		new MatsimEventsReader(manager).readFile(eventsPath);

		System.out.println("Counts:");
		for (Map.Entry<String, Long> entry : counts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		return counts;
	}

	static long countPersons(String populationPath) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationPath);
		return scenario.getPopulation().getPersons().size();
	}

	static void applyFixes(String populationPath) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationPath);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			person.getAttributes().putAttribute("householdConsumptionUnits", 1.0);
		}

		new PopulationWriter(scenario.getPopulation()).write(populationPath);
	}
}
