package org.eqasim.core.scenario.config;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.util.Arrays;
import java.util.List;

public class RunGenerateConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path", "prefix", "sample-size", "threads") //
				.allowOptions("random-seed", "activity-list") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.createConfig(configurator.getConfigGroups());

		String prefix = cmd.getOptionStrict("prefix");
		double sampleSize = Double.parseDouble(cmd.getOptionStrict("sample-size"));
		int randomSeed = cmd.getOption("random-seed").map(Integer::parseInt).orElse(0);
		int threads = Integer.parseInt(cmd.getOptionStrict("threads"));

		//if allow options for activity-list or mode-list exists then send those to generateconfig
		if (cmd.hasOption("activity-list")) {
			List<String> activity = Arrays.asList(cmd.getOption("activity-list").get().split("\\s*,\\s*"));
			new GenerateCustomConfig(cmd, prefix, sampleSize, randomSeed, threads, activity).run(config);
		} else {
			new GenerateConfig(cmd, prefix, sampleSize, randomSeed, threads).run(config);
		}

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}
}
