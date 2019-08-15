package org.eqasim.core.components.config;

import java.util.function.Consumer;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class ConfigAdapter {
	static public void run(String[] args, ConfigGroup[] modules, Consumer<Config> adapter)
			throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"), modules);
		adapter.accept(config);

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}

}
