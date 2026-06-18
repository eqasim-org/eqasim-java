package org.eqasim.core.scenario.config;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class RunGenerateConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path", "prefix", "sample-size", "threads") //
				.allowOptions("random-seed", "activity-types", EqasimConfigurator.CONFIGURATOR) //
				.build();

		EqasimConfigurator configurator = EqasimConfigurator.getInstance(cmd);
		Config config = ConfigUtils.createConfig();
		configurator.updateConfig(config);

		String prefix = cmd.getOptionStrict("prefix");
		double sampleSize = Double.parseDouble(cmd.getOptionStrict("sample-size"));
		int randomSeed = cmd.getOption("random-seed").map(Integer::parseInt).orElse(0);
		int threads = Integer.parseInt(cmd.getOptionStrict("threads"));
		Set<String> activityTypes = cmd.getOption("activity-types")
				.map(actTypes -> new HashSet<>(Arrays.stream(actTypes.split(actTypes)).toList()))
				.orElse(null);

		new GenerateConfig(cmd, prefix, sampleSize, randomSeed, threads, activityTypes).run(config);

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}
}
