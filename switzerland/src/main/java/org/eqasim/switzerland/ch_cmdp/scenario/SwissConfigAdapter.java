package org.eqasim.switzerland.ch_cmdp.scenario;

import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.eqasim.switzerland.ch_cmdp.walk.MobisWalkTravelTime;
import org.eqasim.switzerland.ch_cmdp.walk.WalkConfigGroup;
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

    protected static String walkModel = "mobis";
    protected static double walkV0 = MobisWalkTravelTime.DEFAULT_V0_M_S;
    protected static double walkK = MobisWalkTravelTime.DEFAULT_K;
    protected static double walkUphillPenalty = MobisWalkTravelTime.DEFAULT_UPHILL_PENALTY;
    protected static double walkFlatGrade = MobisWalkTravelTime.DEFAULT_FLAT_GRADE;
    protected static double walkDetourFactor = MobisWalkTravelTime.DEFAULT_DETOUR_FACTOR;

    public static void run(String[] args, SwitzerlandConfigurator configurator, Consumer<Config> adapter)
            throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "downsamplingRate", "replanningRate", "prefix") //
                .allowOptions("activity-list", "hasFreight", "carCostModel", "routeBikeInNetwork","routingDistanceUtility",
                              "countsFile", "countSpecialRegionPath", "speedsFile", "speedsSpecialRegionPath",
                              "walk-model", "walk-v0_m_s", "walk-k", "walk-uphill_penalty", "walk-flat_grade", "walk-detour_factor") //
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
        setWalkModelParameters(cmd, config);
        adapter.accept(config);

        new ConfigWriter(config).write(cmd.getOptionStrict("output-path"));
    }

    private static void setWalkModelParameters(CommandLine cmd, Config config){
        // Start from this input config on every call, then override only explicitly supplied options.
        var walking = ConfigUtils.addOrGetModule(config, WalkConfigGroup.class);
        walkModel = walking.getModel();
        walkV0 = walking.getV0MetersPerSecond();
        walkK = walking.getK();
        walkUphillPenalty = walking.getUphillPenalty();
        walkFlatGrade = walking.getFlatGrade();
        walkDetourFactor = walking.getDetourFactor();
        if  (cmd.hasOption("walk-model")) {
            walkModel = cmd.getOption("walk-model").get();
        }
        if (cmd.hasOption("walk-v0_m_s")) {
            walkV0 = Double.parseDouble(cmd.getOption("walk-v0_m_s").get());
        }
        if (cmd.hasOption("walk-k")) {
            walkK = Double.parseDouble(cmd.getOption("walk-k").get());
        }
        if (cmd.hasOption("walk-uphill_penalty")) {
            walkUphillPenalty =  Double.parseDouble(cmd.getOption("walk-uphill_penalty").get());
        }
        if (cmd.hasOption("walk-flat_grade")) {
            walkFlatGrade = Double.parseDouble(cmd.getOption("walk-flat_grade").get());
        }
        if (cmd.hasOption("walk-detour_factor")) {
            walkDetourFactor = Double.parseDouble(cmd.getOption("walk-detour_factor").get());
        }
    }

    protected static void setCustomActivities(String activityList) {
        hasCustomActivities = true;
        activityTypes = Arrays.asList(activityList.split("\\s*,\\s*"));
    }
}
