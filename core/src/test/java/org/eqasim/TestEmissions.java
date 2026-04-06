package org.eqasim;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.emissions.RunComputeEmissionsEvents;
import org.eqasim.core.components.emissions.RunExportEmissionsNetwork;
import org.eqasim.core.components.emissions.SafeOsmHbefaMapping;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.examples.ExamplesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class TestEmissions {

	private static final Logger log = LogManager.getLogger(TestEmissions.class);

	@Before
	public void setUp() throws IOException {
		// Attempt to remove leftover directories from previous runs (Windows file locks eventually release)
		FileUtils.deleteQuietly(new File("melun_test"));
		URL fixtureUrl = getClass().getResource("/melun");
		FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("melun_test/input"));
		var coldAverageFile = "sample_41_EFA_ColdStart_vehcat_2020average.csv";
		var coldDetailedFile = "sample_41_EFA_ColdStart_SubSegm_2020detailed.csv";
		var hotAverageFile = "sample_41_EFA_HOT_vehcat_2020average.csv";
		var hotDetailedFile = "sample_41_EFA_HOT_SubSegm_2020detailed.csv";
		for (String file : new String[] { coldAverageFile, coldDetailedFile, hotAverageFile, hotDetailedFile }) {
			Files.copy(ExamplesUtils.class.getResourceAsStream("/test/scenarios/emissions-sampleScenario/" + file),
					Paths.get("melun_test/input/", file), REPLACE_EXISTING);
		}
	}

	@After
	public void tearDown() {
		try {
			FileUtils.deleteDirectory(new File("melun_test"));
		} catch (IOException e) {
			log.warn("Could not fully delete melun_test directory : {}", e.getMessage());
		}
	}

	private void runMelunSimulation() throws ConfigurationException {
		EqasimConfigurator eqasimConfigurator = new TestConfigurator();
		Config config = ConfigUtils.loadConfig("melun_test/input/config.xml");
		eqasimConfigurator.updateConfig(config);
		((ControllerConfigGroup) config.getModules().get(ControllerConfigGroup.GROUP_NAME))
				.setOutputDirectory("melun_test/output");

		Scenario scenario = ScenarioUtils.createScenario(config);
		eqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		eqasimConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		eqasimConfigurator.configureController(controller);
		controller.run();
	}

	private void runAddHbefa() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new MatsimVehicleReader(vehicles).readFile("melun_test/input/vehicles.xml.gz");

		for (VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			Attributes hbefa_attributes = vehicleType.getEngineInformation().getAttributes();
			hbefa_attributes.putAttribute("HbefaVehicleCategory", "pass. car");
			hbefa_attributes.putAttribute("HbefaTechnology", "diesel");
			hbefa_attributes.putAttribute("HbefaSizeClass", "not specified");
			hbefa_attributes.putAttribute("HbefaEmissionsConcept", "PC D Euro-3");
		}

		MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
		writer.writeFile("melun_test/input/vehicles.xml.gz");

	}

	private void runModifyConfig() {
		Config config = ConfigUtils.loadConfig("melun_test/input/config.xml");
		config.controller().setOutputDirectory("melun_test/output");
		ConfigUtils.writeConfig(config, "melun_test/input/config.xml");
	}

	private void runModifyNetwork() {
		Config config = ConfigUtils.loadConfig("melun_test/input/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()) {

			// this forces the OSM Mapping code to use URB/Local/50 as it the only thing we
			// have in the sample HBEFA.
			// We intentionally leave the 'pedestrian' links here to test the SafeOsmHbefaMapping class
			String linkType = NetworkUtils.getType(link);
            if (linkType != null) {
				if (!linkType.equals("pedestrian")) {
					continue;
				}
			}
			NetworkUtils.setType(link, "tertiary");
			link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, 50 / 3.6);
		}
		NetworkUtils.writeNetwork(network, "melun_test/input/network.xml.gz");
	}

	private void runMelunEmissions() throws CommandLine.ConfigurationException, IOException {
		Map<String, Long> counts = countLegs("melun_test/output/output_events.xml.gz");
		Assert.assertEquals(4641, (long) counts.get("car"));
		Assert.assertEquals(1554, (long) counts.get("car_passenger"));
		Assert.assertEquals(11910, (long) counts.get("walk"));
		Assert.assertEquals(638, (long) counts.getOrDefault("bike", 0L));
		Assert.assertEquals(2251, (long) counts.get("pt"));

		SafeOsmHbefaMapping.defaultType = "URB/Local/50";

		RunComputeEmissionsEvents.main(new String[] { "--config-path", "melun_test/input/config.xml",
				"--hbefa-cold-avg", "sample_41_EFA_ColdStart_vehcat_2020average.csv", "--hbefa-hot-avg",
				"sample_41_EFA_HOT_vehcat_2020average.csv", "--hbefa-cold-detailed",
				"sample_41_EFA_ColdStart_SubSegm_2020detailed.csv", "--hbefa-hot-detailed",
				"sample_41_EFA_HOT_SubSegm_2020detailed.csv",
				"--eqasim-configurator", TestConfigurator.class.getName() });

		assertEquals(640230, countLines(new File("melun_test/output/output_emissions_events.xml.gz")));

		RunExportEmissionsNetwork.main(new String[] { "--config-path", "melun_test/input/config.xml",
				"--pollutants", "PM,CO,NOx,Unknown", "--time-bin-size", "3600",
				"--eqasim-configurator", TestConfigurator.class.getName() });

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures("melun_test/output/emissions_network.shp");

		// NOTE: Locally, I always get 42235 (old : 32527) lines here. On Github CI, it is always
		// 32528. No clue why this is, but all the previous tests on the events line
		// length etc. pass without a problem ...

		assertEquals(42235, features.size());

		SimpleFeature feature = features.stream().filter(f -> f.getAttribute("link").toString().equals("163994")
				& f.getAttribute("time").toString().equals("43200")).findFirst().orElse(null);
		assertNotNull(feature);

		double expectedPm = 0.059894542675388;
		double expectedCo = 0.893473893036039;
		double expectedNox = 1.080511804659364;
		double expectedUnknown = Double.NaN;

		assertEquals(expectedPm, feature.getAttribute("PM"));
		assertEquals(expectedCo, feature.getAttribute("CO"));
		assertEquals(expectedNox, feature.getAttribute("NOx"));
		assertEquals(expectedUnknown, feature.getAttribute("Unknown"));

		// TODO : test RunComputeEmissionsGrid
	}

	@Test
	public void runTestEmissions() throws CommandLine.ConfigurationException, IOException {
		runAddHbefa();
		runModifyConfig();
		runModifyNetwork();
		runMelunSimulation();
		runMelunEmissions();
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

	static long countLines(File file) throws IOException {
		String line;
		int lines = 0;

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));

		while ((line = reader.readLine()) != null) {
			lines++;
		}

		reader.close();
		return lines;
	}
}
