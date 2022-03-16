package org.eqasim.switzerland.scenario;

import org.matsim.core.config.*;

import java.util.function.Consumer;


public class SwissConfigAdapter {
    static public void run(String[] args, ConfigGroup[] modules, Consumer<Config> adapter)
            throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path") //
                .allowOptions("activity_list") //
                .build();

        if(cmd.hasOption("activity_list")) {
            CustomActivities.run(cmd.getOption("activity_list").get());
        }

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"), modules);
        adapter.accept(config);

        new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
    }
}
