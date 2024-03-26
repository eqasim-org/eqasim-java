package org.eqasim.core.simulation.modes.drt.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.plusOne.PlusOneRebalancingStrategyParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.utils.misc.Time;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AdaptConfigForDrt {

    public static void adapt(Config config, Map<String, String> vehiclesPathByDrtMode, Map<String, String> operationalSchemes, Map<String, String> drtUtilityEstimators, Map<String, String> drtCostModels, String qsimEndtime, String modeAvailability) {
        if(!config.getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
            config.addModule(new DvrpConfigGroup());
        }
        if(!config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
            config.addModule(new MultiModeDrtConfigGroup());
        }
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);

        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        // Add DRT to the available modes
        if(modeAvailability != null) {
            dmcConfig.setModeAvailability(modeAvailability);
        }


        // Add DRT to cached modes
        Set<String> cachedModes = new HashSet<>(dmcConfig.getCachedModes());
        cachedModes.addAll(vehiclesPathByDrtMode.keySet());
        dmcConfig.setCachedModes(cachedModes);

        Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
        tripConstraints.add(EqasimModeChoiceModule.DRT_WALK_CONSTRAINT);
        dmcConfig.setTripConstraints(tripConstraints);

        for(String drtMode: vehiclesPathByDrtMode.keySet()) {
            DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
            drtConfigGroup.mode = drtMode;
            drtConfigGroup.operationalScheme = DrtConfigGroup.OperationalScheme.valueOf(operationalSchemes.get(drtMode));
            drtConfigGroup.stopDuration = 15.0;
            drtConfigGroup.maxWaitTime = 600;
            drtConfigGroup.maxTravelTimeAlpha = 1.5;
            drtConfigGroup.maxTravelTimeBeta = 300.0;
            drtConfigGroup.vehiclesFile  = vehiclesPathByDrtMode.get(drtMode);

            DrtInsertionSearchParams searchParams = new ExtensiveInsertionSearchParams();
            drtConfigGroup.addDrtInsertionSearchParams(searchParams);

            RebalancingParams rebalancingParams = new RebalancingParams();
            rebalancingParams.interval  =1800;
            rebalancingParams.addParameterSet(new PlusOneRebalancingStrategyParams());
            drtConfigGroup.addParameterSet(rebalancingParams);

            DrtZonalSystemParams drtZonalSystemParams = new DrtZonalSystemParams();
            drtZonalSystemParams.zonesGeneration  = DrtZonalSystemParams.ZoneGeneration.GridFromNetwork;
            drtZonalSystemParams.cellSize = 500.0;
            drtZonalSystemParams.targetLinkSelection = DrtZonalSystemParams.TargetLinkSelection.mostCentral;
            drtConfigGroup.addParameterSet(drtZonalSystemParams);

            multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);

            // Set up choice model
            EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
            eqasimConfig.setCostModel(drtMode, drtCostModels.get(drtMode));
            eqasimConfig.setEstimator(drtMode, drtUtilityEstimators.get(drtMode));

            PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(drtMode);
            config.planCalcScore().addModeParams(modeParams);
        }

        DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtConfigGroup, config.planCalcScore(), config.plansCalcRoute());

        // Additional requirements
        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);
        config.qsim().setEndTime(Time.parseOptionalTime(qsimEndtime).seconds());
        config.qsim().setSimEndtimeInterpretation(QSimConfigGroup.EndtimeInterpretation.onlyUseEndtime);

    }

    public static Map<String, Map<String, String>> extractDrtInfo(String[] drtModeNames, Map<String, String[]> values) {
        Map<String, Map<String, String>> result = new HashMap<>();
        if(drtModeNames.length == 0) {
            throw new IllegalStateException("No drt modes provided");
        }
        for(String key: values.keySet()) {
            String[] currentElements = values.get(key);
            Map<String, String> resultingMap;

            if(drtModeNames.length == currentElements.length) {
                resultingMap = IntStream.range(0, drtModeNames.length).boxed().collect(Collectors.toMap(integer -> drtModeNames[integer], (Function<Integer, String>) integer -> currentElements[integer]));
            } else {
                if(currentElements.length != 1) {
                    throw new IllegalStateException(String.format("When the number of provided drt mode names is not equal to the number of provided %s," +
                            " only one %s should be provided, it will be used for all the drt modes ", key, key));
                }
                resultingMap = Arrays.stream(drtModeNames).collect(Collectors.toMap(mode -> mode, mode -> currentElements[0]));
            }
            result.put(key, resultingMap);
        }
        return result;
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-config-path", "output-config-path", "vehicles-paths")
                .allowOptions("mode-names")
                .allowOptions("mode-availability")
                .allowOptions("configurator-class")
                .allowOptions("operational-schemes")
                .allowOptions("cost-models", "estimators")
                .allowOptions("qsim-endtime")
                .build();


        String inputConfigPath = cmd.getOptionStrict("input-config-path");
        String outputConfigPath = cmd.getOptionStrict("output-config-path");
        String[] modeNames = Arrays.stream(cmd.getOption("mode-names").orElse("drt").split(",")).toList().toArray(String[]::new);
        String[] vehiclesPath = cmd.getOptionStrict("vehicles-paths").split(",");
        String qsimEndtime = cmd.getOption("qsim-endtime").orElse("30:00:00");
        String[] costModel = cmd.getOption("cost-models").orElse(EqasimModeChoiceModule.ZERO_COST_MODEL_NAME).split(",");
        String[] estimator = cmd.getOption("estimators").orElse(EqasimModeChoiceModule.DRT_ESTIMATOR_NAME).split(",");
        String[] operationalSchemes = cmd.getOption("operational-schemes").orElse(DrtConfigGroup.OperationalScheme.door2door.toString()).split(",");


        Map<String, String[]> toExtract = new HashMap<>();
        toExtract.put("vehicles-paths", Arrays.stream(vehiclesPath).map(p -> Path.of(outputConfigPath).getParent().toAbsolutePath().relativize(Path.of(p).toAbsolutePath()).toString()).toArray(String[]::new));
        toExtract.put("cost-models", costModel);
        toExtract.put("estimators", estimator);
        toExtract.put("operational-schemes", operationalSchemes);

        Map<String, Map<String, String>> info = extractDrtInfo(modeNames, toExtract);


        EqasimConfigurator configurator;
        if(cmd.hasOption("configurator-class")) {
            configurator = ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict("configurator-class"), EqasimConfigurator.class);
        } else {
            configurator = new EqasimConfigurator();
        }

        Config config = ConfigUtils.loadConfig(inputConfigPath, configurator.getConfigGroups());

        adapt(config, info.get("vehicles-paths"), info.get("operational-schemes"), info.get("estimators"), info.get("cost-models"), qsimEndtime, cmd.getOption("mode-availability").orElse(null));

        cmd.applyConfiguration(config);

        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}