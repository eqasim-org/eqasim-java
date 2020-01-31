package org.eqasim.sao_paulo.preparation;

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

public class RunImputeInnerSPAttribute {
	public static void main(String[] args)
			throws MalformedURLException, IOException, ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("sp-path", "input-path", "output-path", "attribute-name") //
				.build();

		// Load IRIS
		Set<SPTract> iris = SPTract.read(new File(cmd.getOptionStrict("sp-path")));

		// Load population
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		PopulationReader populationReader = new PopulationReader(scenario);
		populationReader.readFile(cmd.getOptionStrict("input-path"));

		// Impute attribute
		ImputeInnerSPAttribute imputeInnerSPAttribute = new ImputeInnerSPAttribute(iris);
		imputeInnerSPAttribute.run(scenario.getPopulation(), cmd.getOptionStrict("attribute-name"));

		// Write population
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(cmd.getOptionStrict("output-path"));
	}
}
