package org.eqasim.switzerland.ch_cmdp.scenario;

import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
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
    protected static double routingDistanceUtility = 0.0;
    protected static String prefix = "";

    protected static String carCostModel = "simple";
    protected static boolean routeBikeInNetwork = false;

    protected static String countSpecialRegionPath = "";
    protected static String speedsSpecialRegionPath = "";
    protected static String speedsFile = "";
    protected static String countsFile = "";

    public static void run(String[] args, SwitzerlandConfigurator configurator, Consumer<Config> adapter)
            throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "downsamplingRate", "replanningRate", "prefix") //
                .allowOptions("activity-list", "hasFreight", "carCostModel", "routeBikeInNetwork","routingDistanceUtility",
                              "countsFile", "countSpecialRegionPath", "speedsFile", "speedsSpecialRegionPath") //
                .build();

        if (cmd.hasOption("activity-list")) {
            setCustomActivities(cmd.getOption("activity-list").get());
        }

        if (cmd.hasOption("hasFreight")) {
            hasFreight = cmd.getOption("hasFreight").get().equalsIgnoreCase("true");;
        }

        if (cmd.hasOption("routingDistanceUtility")) {
            routingDistanceUtility = Double.parseDouble(cmd.getOption("routingDistanceUtility").get());
        }

        if (cmd.hasOption("carCostModel")) {
            carCostModel = cmd.getOption("carCostModel").get();
        }

        if (cmd.hasOption("routeBikeInNetwork")) {
            routeBikeInNetwork = cmd.getOption("routeBikeInNetwork").get().equalsIgnoreCase("true");
        }

        if (cmd.hasOption("countSpecialRegionPath")) {
            countSpecialRegionPath = cmd.getOption("countSpecialRegionPath").get();
        }
        if (cmd.hasOption("speedsSpecialRegionPath")) {
            speedsSpecialRegionPath = cmd.getOption("speedsSpecialRegionPath").get();
        }

        if (cmd.hasOption("speedsFile")) {
            speedsFile = cmd.getOption("speedsFile").get();
        }
        if (cmd.hasOption("countsFile")) {
            countsFile = cmd.getOption("countsFile").get();
        }

        replanningRate = Double.parseDouble(cmd.getOptionStrict("replanningRate"));

        downsamplingRate = Double.parseDouble(cmd.getOptionStrict("downsamplingRate"));

        prefix = cmd.getOptionStrict("prefix");

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
