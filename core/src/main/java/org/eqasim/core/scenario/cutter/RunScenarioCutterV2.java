package org.eqasim.core.scenario.cutter;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RunScenarioCutterV2 {
	static public void main(String[] args)
			throws ConfigurationException, IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path", "extent-path") //
				.allowOptions("threads", "prefix", "extent-attribute", "extent-value", "plans-path", "events-path") //
				.allowOptions("skip-routing")
				.build();

		RunScenarioCutter.main(args);

		String prefix = cmd.getOption("prefix").orElse("");
		EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		eqasimConfigurator.configureScenario(scenario);
		new PopulationReader(scenario).readFile(Paths.get(cmd.getOptionStrict("output-path"), prefix+"population.xml.gz").toString());
		IdSet<Person> personIds = new IdSet<>(Person.class);
		scenario.getPopulation().getPersons().values().stream().map(Person::getId).forEach(personIds::add);

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), eqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);
		scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(scenario.getConfig().plans().getInputFileURL(scenario.getConfig().getContext()).getPath());

		IdSet<Person> personsToRemove = new IdSet<>(Person.class);
		scenario.getPopulation().getPersons().values().stream().map(Person::getId).filter(personId -> !personIds.contains(personId)).forEach(personsToRemove::add);

		personsToRemove.forEach(scenario.getPopulation()::removePerson);

		new PopulationWriter(scenario.getPopulation()).write(Paths.get(cmd.getOptionStrict("output-path"), prefix+"population_extended.xml.gz").toString());

		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		String networkPath = Paths.get(cmd.getOptionStrict("config-path"), "..", scenario.getConfig().network().getInputFile()).toAbsolutePath().normalize().toString();

		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		scenario.getNetwork().getLinks().values()
				.stream().filter(link -> extent.isInside(link.getFromNode().getCoord()) && extent.isInside(link.getFromNode().getCoord()))
				.forEach(link -> {
					Set<String> linkModes = new HashSet<>(link.getAllowedModes());
					link.getAllowedModes().stream().toList().stream().map(mode -> "inside_"+mode).forEach(linkModes::add);
					link.setAllowedModes(linkModes);
				});
		new NetworkWriter(scenario.getNetwork()).write(Paths.get(cmd.getOptionStrict("output-path"), prefix+"network_extended.xml.gz").toString());
	}
}
