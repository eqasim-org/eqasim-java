package org.eqasim.examples.covid19;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCleanPopulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		String inputPath = cmd.getOptionStrict("input-path");
		String outputPath = cmd.getOptionStrict("output-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(inputPath);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			new ArrayList<>(person.getPlans()).forEach(person::removePlan);
		}

		new PopulationWriter(scenario.getPopulation()).write(outputPath);
	}
}
