package org.eqasim.core.components.config;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

public class ConfigAdapter {
	static public void run(String[] args, ConfigGroup[] modules, ConfigAdapterConsumer adapter)
			throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "prefix") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"), modules);
		adapter.accept(config, cmd.getOptionStrict("prefix"));

		new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
	}
	
	public interface ConfigAdapterConsumer {
		void accept(Config config, String prefix);
	}
}
