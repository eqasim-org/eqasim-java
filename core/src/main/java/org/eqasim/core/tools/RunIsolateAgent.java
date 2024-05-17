package org.eqasim.core.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class RunIsolateAgent {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "agent-id")
				.allowOptions("eqasim-configurator-class")//
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		EqasimConfigurator configurator = cmd.hasOption("eqasim-configurator-class") ? ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict("eqasim-configurator-class"), EqasimConfigurator.class) : new EqasimConfigurator();

		// We need to do this in order to set up the appropriate route factories
		configurator.configureScenario(scenario);
		// Load population
		String inputPath = cmd.getOptionStrict("input-path");
		new PopulationReader(scenario).readFile(inputPath);

		// Reduce population
		Collection<Id<Person>> allIds = new HashSet<>(scenario.getPopulation().getPersons().keySet());
		allIds.removeAll(Arrays.asList(Id.createPersonId(cmd.getOptionStrict("agent-id"))));
		allIds.forEach(scenario.getPopulation()::removePerson);

		// Write population
		String outputPath = cmd.getOptionStrict("output-path");
		new PopulationWriter(scenario.getPopulation()).write(outputPath);
	}
}
