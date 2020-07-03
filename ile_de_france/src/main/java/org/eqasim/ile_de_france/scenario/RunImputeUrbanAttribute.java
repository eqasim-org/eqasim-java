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
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class RunImputeUrbanAttribute {
	static public void main(String[] args) throws ConfigurationException, MalformedURLException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-population-path", "input-network-path", "output-population-path",
						"output-network-path", "extent-path") //
				.allowOptions("extent-attribute", "extent-value") //
				.build();

		// Load extent
		File extentPath = new File(cmd.getOptionStrict("extent-path"));
		Optional<String> extentAttribute = cmd.getOption("extent-attribute");
		Optional<String> extentValue = cmd.getOption("extent-value");
		ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

		// Load population
		File populationPath = new File(cmd.getOptionStrict("input-population-path"));

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);

		new PopulationReader(scenario).readFile(populationPath.toString());

		// Load network
		File networkPath = new File(cmd.getOptionStrict("input-network-path"));
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());

		// Add attributes
		new ImputeUrbanAttribute(extent).run(scenario.getPopulation(), scenario.getNetwork());

		// Write population
		File outputPopulationPath = new File(cmd.getOptionStrict("output-population-path"));
		new PopulationWriter(scenario.getPopulation()).write(outputPopulationPath.toString());

		// Write network
		File outputNetworkPath = new File(cmd.getOptionStrict("output-network-path"));
		new NetworkWriter(scenario.getNetwork()).write(outputNetworkPath.toString());
	}
}
