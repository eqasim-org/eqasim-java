package org.eqasim.ile_de_france.scenario;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Optional;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class RunImputeUrbanAttribute {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "extent-path") //
				.allowOptions("extent-attribute", "extent-value") //
				.build();

		// Load extent
		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		// Load population
		File inputPath = new File(cmd.getOptionStrict("input-path"));

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);

		new PopulationReader(scenario).readFile(inputPath.toString());

		// Add attributes
		new ImputeUrbanAttribute(extent).run(scenario.getPopulation());

		// Write population
		File outputPath = new File(cmd.getOptionStrict("output-path"));
		new PopulationWriter(scenario.getPopulation()).write(outputPath.toString());
	}
}
