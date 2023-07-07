package org.eqasim.ile_de_france;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
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
import org.matsim.core.utils.misc.CRCChecksum;

public class TestCorisica {
	@Before
	public void setUp() throws IOException {
		URL fixtureUrl = getClass().getClassLoader().getResource("corsica");
		Assert.assertNotNull(fixtureUrl);
		FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("corsica_test/base_inputs"));
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.deleteDirectory(new File("corsica_test"));
	}

	@Test
	public void testCorsicaPipeline()
			throws ConfigurationException, InterruptedException, MalformedURLException, IOException {

		Assert.assertEquals(3162, countPersons("corsica_test/base_inputs/corsica_population.xml.gz"));

		URL referenceUrl = getClass().getClassLoader().getResource("corsica_reference_outputs");
		Assert.assertNotNull(referenceUrl);

		// Run the simulation
		{
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/base_inputs/corsica_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/simulation_output", //
			});

			compareDirs(new File("corsica_test", "simulation_output"), new File(referenceUrl.getPath(), "base_simulation_output"));

			Assert.assertEquals(3162, countPersons("corsica_test/simulation_output/output_plans.xml.gz"));

			Map<String, Long> counts = countLegs("corsica_test/simulation_output/output_events.xml.gz");
			Assert.assertEquals(7781, (long) counts.get("car"));
			Assert.assertEquals(894, (long) counts.get("car_passenger"));
			Assert.assertEquals(2091, (long) counts.get("walk"));
			Assert.assertEquals(2, (long) counts.get("bike"));
			Assert.assertEquals(47, (long) counts.get("pt"));
		}

		// Cut the scenario based on output plans
		{
			RunScenarioCutter.main(new String[] { //
					"--config-path", "corsica_test/base_inputs/corsica_config.xml", //
					"--config:plans.inputPlansFile", "../simulation_output/output_plans.xml.gz", //
					"--extent-path", "corsica_test/base_inputs/extent.shp", //
					"--threads", "4", //
					"--prefix", "cut_", //
					"--output-path", "corsica_test/cut_inputs", //
			});
			compareDirs(new File("corsica_test", "cut_inputs"), new File(referenceUrl.getPath(), "cut_inputs"));
			Assert.assertEquals(1286, countPersons("corsica_test/cut_inputs/cut_population.xml.gz"));
		}

		// Run the cut simulation
		{
			RunSimulation.main(new String[] { //
					"--config-path", "corsica_test/cut_inputs/cut_config.xml", //
					"--config:controler.lastIteration", "2", // ,
					"--config:controler.outputDirectory", "corsica_test/cut_output", //
			});

			compareDirs(new File("corsica_test", "cut_output"), new File(referenceUrl.getPath(), "cut_simulation_output"));
			Assert.assertEquals(1286, countPersons("corsica_test/cut_output/output_plans.xml.gz"));

			Map<String, Long> counts = countLegs("corsica_test/cut_output/output_events.xml.gz");
			Assert.assertEquals(3001, (long) counts.get("car"));
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

	private void compareFiles(File file1, File file2) {
		Assert.assertEquals(String.format("Files not corresponding %s - %s", file1.getAbsolutePath(), file2.getAbsolutePath()), CRCChecksum.getCRCFromFile(file1.getPath()), CRCChecksum.getCRCFromFile(file2.getPath()));
	}

	private void compareDirs(File dir1, File dir2)
	{
		File[] list1 = dir1.listFiles(), list2 = dir2.listFiles();
		Assert.assertNotNull(list1);
		Assert.assertNotNull(list2);
		Assert.assertEquals(list1.length, list2.length);
		for (int i = 0; i < list2.length; i++) {
			Assert.assertEquals(list1[i].isFile(), list2[i].isFile());
			Assert.assertEquals(list2[i].isDirectory(), list2[i].isDirectory());
			if (list1[i].isFile()) {
				compareFiles(list1[i], list2[i]);
			} else if (list1[i].isDirectory()) {
				compareDirs(list1[i], list2[i]);
			}
		}
	}

	static long countPersons(String populationPath) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(populationPath);
		return scenario.getPopulation().getPersons().size();
	}
}
