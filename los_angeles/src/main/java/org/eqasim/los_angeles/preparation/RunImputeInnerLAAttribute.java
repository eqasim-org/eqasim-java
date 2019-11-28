package org.eqasim.los_angeles.preparation;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunImputeInnerLAAttribute {
	public static void main(String[] args)
			throws MalformedURLException, IOException, ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("la-path", "input-path", "output-path", "attribute-name") //
				.build();

		// Load IRIS
		Set<LATract> iris = LATract.read(new File(cmd.getOptionStrict("la-path")));

		// Load population
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		PopulationReader populationReader = new PopulationReader(scenario);
		populationReader.readFile(cmd.getOptionStrict("input-path"));

		// Impute attribute
		ImputeInnerLAAttribute imputeInnerSFAttribute = new ImputeInnerLAAttribute(iris);
		imputeInnerSFAttribute.run(scenario.getPopulation(), cmd.getOptionStrict("attribute-name"));

		// Write population
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(cmd.getOptionStrict("output-path"));
	}
}
