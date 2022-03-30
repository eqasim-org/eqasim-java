package org.eqasim.switzerland.scenario;

import org.matsim.core.config.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


public class SwissConfigAdapter {
    protected static Boolean hasCustomActivities = false;
    protected static List<String> activityTypes;

    public static void run(String[] args, ConfigGroup[] modules, Consumer<Config> adapter)
            throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path") //
                .allowOptions("activity-list") //
                .build();

        if(cmd.hasOption("activity-list")) {
            setCustomActivities(cmd.getOption("activity-list").get());
        }

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("input-path"), modules);
        adapter.accept(config);

        new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
    }

    protected static void setCustomActivities (String activityList) {
        hasCustomActivities = true;
        activityTypes = Arrays.asList(activityList.split("\\s*,\\s*"));
    }
}