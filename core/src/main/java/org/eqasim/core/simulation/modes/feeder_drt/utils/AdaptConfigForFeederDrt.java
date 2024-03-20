package org.eqasim.core.simulation.modes.feeder_drt.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.modes.drt.utils.AdaptConfigForDrt;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.config.MultiModeFeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.EqasimFeederDrtModeChoiceModule;
import org.eqasim.core.simulation.modes.feeder_drt.mode_choice.constraints.FeederDrtConstraint;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.pt.config.TransitConfigGroup;

import java.util.*;
import java.util.stream.Collectors;

public class AdaptConfigForFeederDrt {

    public static void adapt(Config config, Map<String, String> basePtModes, Map<String, String> baseDrtModes, Map<String, String> utilityEstimators, String modeAvailability) {
        if(!config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
            throw new IllegalStateException(String.format("Cannot add module '%s' if module '%s' is not present already. You can use '%s' to configure it.", MultiModeFeederDrtConfigGroup.GROUP_NAME, MultiModeDrtConfigGroup.GROUP_NAME, AdaptConfigForDrt.class.getCanonicalName()));
        }

        if(!config.getModules().containsKey(MultiModeFeederDrtConfigGroup.GROUP_NAME)) {
            config.addModule(new MultiModeFeederDrtConfigGroup());
        }

        MultiModeFeederDrtConfigGroup multiModeFeederDrtConfigGroup = (MultiModeFeederDrtConfigGroup) config.getModules().get(MultiModeFeederDrtConfigGroup.GROUP_NAME);
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
        TransitConfigGroup transitConfigGroup = (TransitConfigGroup) config.getModules().get(TransitConfigGroup.GROUP_NAME);
        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
        EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(config);

        // Add DRT to the available modes
        if(modeAvailability != null) {
            dmcConfig.setModeAvailability(modeAvailability);
        }


        //This constraint need to be added
        dmcConfig.getTripConstraints().add(FeederDrtConstraint.NAME);

        // Add DRT to cached modes
        Set<String> cachedModes = new HashSet<>(dmcConfig.getCachedModes());
        cachedModes.addAll(basePtModes.keySet());
        dmcConfig.setCachedModes(cachedModes);

        List<String> drtModes = multiModeDrtConfigGroup.modes().toList();

        for(String feederDrtMode: basePtModes.keySet()) {
            FeederDrtConfigGroup feederDrtConfigGroup = new FeederDrtConfigGroup();
            feederDrtConfigGroup.mode = feederDrtMode;
            feederDrtConfigGroup.ptModeName = basePtModes.get(feederDrtMode);
            if(!transitConfigGroup.getTransitModes().contains(feederDrtConfigGroup.ptModeName)) {
                throw new IllegalStateException(String.format("PT mode '%s' supplied for '%s' is not registered as a transit mode in the '%s' config group", feederDrtConfigGroup.ptModeName, feederDrtMode, TransitConfigGroup.GROUP_NAME));
            }
            feederDrtConfigGroup.accessEgressModeName = baseDrtModes.get(feederDrtMode);
            if(!drtModes.contains(feederDrtConfigGroup.accessEgressModeName)) {
                throw new IllegalStateException(String.format("DRT mode '%s' supplied for '%s' is not registered in the '%s' config group. You can use '%s' to configure it", feederDrtConfigGroup.accessEgressModeName, feederDrtMode, MultiModeDrtConfigGroup.GROUP_NAME, AdaptConfigForDrt.class.getCanonicalName()));
            }
            multiModeFeederDrtConfigGroup.addParameterSet(feederDrtConfigGroup);

            eqasimConfigGroup.setEstimator(feederDrtMode, utilityEstimators.get(feederDrtMode));

            PlanCalcScoreConfigGroup.ActivityParams activityParams = new PlanCalcScoreConfigGroup.ActivityParams(feederDrtMode + " interaction");
            config.planCalcScore().addActivityParams(activityParams);
            activityParams.setTypicalDuration(1);
            activityParams.setScoringThisActivityAtAll(false);
        }
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args)
                .requireOptions("input-config-path", "output-config-path")
                .allowOptions("mode-names")
                .allowOptions("base-drt-modes")
                .allowOptions("base-pt-modes")
                .allowOptions("estimators")
                .allowOptions("mode-availability")
                .allowOptions("configurator-class")
                .build();

        String inputConfigPath = cmd.getOptionStrict("input-config-path");
        String outputConfigPath = cmd.getOptionStrict("output-config-path");
        String[] modeNames = Arrays.stream(cmd.getOption("mode-names").orElse("feeder_drt").split(",")).collect(Collectors.toSet()).toArray(String[]::new);
        String[] baseDrtModes = cmd.getOption("base-drt-modes").orElse("drt").split(",");
        String[] basePtModes = cmd.getOption("base-pt-modes").orElse("pt").split(",");
        String[] estimators = cmd.getOption("estimators").orElse(EqasimFeederDrtModeChoiceModule.FEEDER_DRT_ESTIMATOR_NAME).split(",");


        Map<String, String[]> toExtract = new HashMap<>();
        toExtract.put("base-drt-modes", baseDrtModes);
        toExtract.put("base-pt-modes", basePtModes);
        toExtract.put("estimators", estimators);

        Map<String, Map<String, String>> info = AdaptConfigForDrt.extractDrtInfo(modeNames, toExtract);

        EqasimConfigurator configurator;
        if(cmd.hasOption("configurator-class")) {
            configurator = ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict("configurator-class"), EqasimConfigurator.class);
        } else {
            configurator = new EqasimConfigurator();
        }

        Config config = ConfigUtils.loadConfig(inputConfigPath, configurator.getConfigGroups());
        configurator.addOptionalConfigGroups(config);

        adapt(config, info.get("base-pt-modes"), info.get("base-drt-modes"), info.get("estimators"), cmd.getOption("mode-availability").orElse(null));

        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}
