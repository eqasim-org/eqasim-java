package org.eqasim.ile_de_france;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.scenario.cutter.RunScenarioCutter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigUtils;
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

	@Test
	public void testCorsicaPipeline()
			throws ConfigurationException, InterruptedException, MalformedURLException, IOException {

		Assert.assertEquals(3162, countPersons("corsica_test/corsica_population.xml.gz"));

		// Run the simulation
		{
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/corsica_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/simulation_output", //
			});

			Assert.assertEquals(3162, countPersons("corsica_test/simulation_output/output_plans.xml.gz"));

			Map<String, Long> counts = countLegs("corsica_test/simulation_output/output_events.xml.gz");
			Assert.assertEquals(7782, (long) counts.get("car"));
			Assert.assertEquals(894, (long) counts.get("car_passenger"));
			Assert.assertEquals(2089, (long) counts.get("walk"));
			Assert.assertEquals(2, (long) counts.get("bike"));
			Assert.assertEquals(47, (long) counts.get("pt"));
		}

		// Cut the scenario based on output plans
		{
			RunScenarioCutter.main(new String[] { //
					"--config-path", "corsica_test/corsica_config.xml", //
					"--config:plans.inputPlansFile", "simulation_output/output_plans.xml.gz", //
					"--extent-path", "corsica_test/extent.shp", //
					"--threads", "4", //
					"--prefix", "cut_", //
					"--output-path", "corsica_test", //
			});

			Assert.assertEquals(1286, countPersons("corsica_test/cut_population.xml.gz"));
		}

		// Run the cut simulation
		{
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/cut_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/cut_output", //
			});

			Assert.assertEquals(1286, countPersons("corsica_test/cut_output/output_plans.xml.gz"));

			Map<String, Long> counts = countLegs("corsica_test/cut_output/output_events.xml.gz");
			Assert.assertEquals(2997, (long) counts.get("car"));
			Assert.assertEquals(387, (long) counts.get("car_passenger"));
			Assert.assertEquals(847, (long) counts.get("walk"));
			Assert.assertEquals(0, (long) counts.getOrDefault("bike", 0L));
			Assert.assertEquals(6, (long) counts.get("pt"));
			Assert.assertEquals(95, (long) counts.get("outside"));
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
}
