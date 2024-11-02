package org.eqasim.switzerland.scenario;

import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.matsim.core.config.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SwissConfigAdapter {
    protected static Boolean hasCustomActivities = false;
    protected static List<String> activityTypes;

    protected static Boolean hasFreight = false;
    protected static double downsamplingRate = 1.0;
    protected static double replanningRate = 0.05;

    public static void run(String[] args, SwitzerlandConfigurator configurator, Consumer<Config> adapter)
            throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "downsamplingRate", "replanningRate") //
                .allowOptions("activity-list", "hasFreight") //
                .build();

        if (cmd.hasOption("activity-list")) {
            setCustomActivities(cmd.getOption("activity-list").get());
        }

        if (cmd.hasOption("hasFreight")) {
            hasFreight = true;
        }

        replanningRate = Double.parseDouble(cmd.getOptionStrict("replanningRate"));

        downsamplingRate = Double.parseDouble(cmd.getOptionStrict("downsamplingRate"));

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"));
        configurator.updateConfig(config);
        adapter.accept(config);

        new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
    }

    protected static void setCustomActivities(String activityList) {
        hasCustomActivities = true;
        activityTypes = Arrays.asList(activityList.split("\\s*,\\s*"));
    }
}