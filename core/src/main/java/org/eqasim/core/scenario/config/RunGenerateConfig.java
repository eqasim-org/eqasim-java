package org.eqasim.core.scenario.config;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class RunGenerateConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path", "prefix", "sample-size", "threads") //
				.allowOptions("random-seed") //
				.build();

		Config config = ConfigUtils.createConfig(EqasimConfigurator.getConfigGroups());

		String prefix = cmd.getOptionStrict("prefix");
		double sampleSize = Double.parseDouble(cmd.getOptionStrict("sample-size"));
		int randomSeed = cmd.getOption("random-seed").map(Integer::parseInt).orElse(0);
		int threads = Integer.parseInt(cmd.getOptionStrict("threads"));

		new GenerateConfig(cmd, prefix, sampleSize, randomSeed, threads).run(config);

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}
}
