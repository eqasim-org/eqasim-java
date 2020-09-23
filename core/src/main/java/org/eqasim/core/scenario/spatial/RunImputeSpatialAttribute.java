package org.eqasim.core.scenario.spatial;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eqasim.core.scenario.SpatialUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.locationtech.jts.geom.Polygon;
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

public class RunImputeSpatialAttribute {
	static public void main(String[] args)
			throws ConfigurationException, MalformedURLException, IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("input-population-path", "input-network-path", "output-population-path",
						"output-network-path") //
				.requireOptions("shape-path", "shape-attribute", "shape-value", "attribute") //
				.build();

		if (cmd.hasOption("input-population-path") ^ cmd.hasOption("output-population-path")) {
			throw new IllegalStateException("Both input and output path must be given for the population.");
		}

		if (cmd.hasOption("input-network-path") ^ cmd.hasOption("output-network-path")) {
			throw new IllegalStateException("Both input and output path must be given for the network.");
		}

		// Load shape
		String shapeAttribute = cmd.getOptionStrict("shape-attribute");
		String shapeValue = cmd.getOptionStrict("shape-value");
		URL shapeUrl = new File(cmd.getOptionStrict("shape-path")).toURI().toURL();

		Polygon shape = SpatialUtils.loadPolygon(shapeUrl, shapeAttribute, shapeValue);

		// Set up imputation

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);

		String attribute = cmd.getOptionStrict("attribute");
		ImputeSpatialAttribute algorithm = new ImputeSpatialAttribute(shape, attribute);

		// Load network
		if (cmd.hasOption("input-network-path")) {
			File networkPath = new File(cmd.getOptionStrict("input-network-path"));
			new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());
			algorithm.run(scenario.getNetwork());
			new NetworkWriter(scenario.getNetwork()).write(cmd.getOptionStrict("output-network-path"));
		}

		// Load population
		if (cmd.hasOption("input-population-path")) {
			File populationPath = new File(cmd.getOptionStrict("input-population-path"));
			new PopulationReader(scenario).readFile(populationPath.toString());
			algorithm.run(scenario.getPopulation());
			new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-population-path"));
		}
	}
}
