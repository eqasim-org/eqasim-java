package org.eqasim.sao_paulo.scenario;

import org.eqasim.core.scenario.config.GenerateConfig;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

public class GenerateSaoPauloConfig {
	private final GenerateConfig delegate;

	public GenerateSaoPauloConfig(CommandLine cmd, String prefix, double sampleSize, int randomSeed, int threads) {
		delegate = new GenerateConfig(cmd, prefix, sampleSize, randomSeed, threads);
	}

	public void run(Config config) throws ConfigurationException {
		delegate.run(config);

		config.transit().setVehiclesFile(null);
		config.households().setInputFile(null);
	}
}
