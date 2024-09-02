package org.eqasim.core.simulation.modes.transit_with_abstract_access.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.TransitWithAbstractAbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.TransitWithAbstractAccessModeChoiceModule;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.mode_choice.constraints.TransitWithAbstractAccessConstraint;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AdaptConfigForTransitWithAbstractAccess {



    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-config-path", "output-config-path", "mode-name", "accesses-file-path")
                .build();

        Config config = ConfigUtils.loadConfig(commandLine.getOptionStrict("input-config-path"), new DiscreteModeChoiceConfigGroup(), new EqasimConfigGroup());
        String mode = commandLine.getOptionStrict("mode-name");
        String outputConfigPath = commandLine.getOptionStrict("output-config-path");

        DiscreteModeChoiceConfigGroup dmcConfigGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
        dmcConfigGroup.getTripConstraints().add(TransitWithAbstractAccessConstraint.NAME);

        Set<String> cachedModes = new HashSet<>(dmcConfigGroup.getCachedModes());
        cachedModes.add(mode);
        dmcConfigGroup.setCachedModes(cachedModes);

        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        eqasimConfigGroup.setEstimator(mode, TransitWithAbstractAccessModeChoiceModule.TRANSIT_WITH_ABSTRACT_ACCESS_UTILITY_ESTIMATOR_NAME);

        ScoringConfigGroup scoringConfigGroup = (ScoringConfigGroup) config.getModules().get(ScoringConfigGroup.GROUP_NAME);
        ScoringConfigGroup.ModeParams modeParams = new ScoringConfigGroup.ModeParams(TransitWithAbstractAccessRoutingModule.ABSTRACT_ACCESS_LEG_MODE_NAME);
        modeParams.setMarginalUtilityOfTraveling(-1);
        scoringConfigGroup.addModeParams(modeParams);

        TransitWithAbstractAbstractAccessModuleConfigGroup transitWithAbstractAbstractAccessModuleConfigGroup = new TransitWithAbstractAbstractAccessModuleConfigGroup();
        transitWithAbstractAbstractAccessModuleConfigGroup.setModeName(mode);

        Path path = Path.of(outputConfigPath).getParent().toAbsolutePath().relativize(Path.of(commandLine.getOptionStrict("accesses-file-path")).toAbsolutePath());
        transitWithAbstractAbstractAccessModuleConfigGroup.setAccessItemsFilePath(path.toString());
        config.addModule(transitWithAbstractAbstractAccessModuleConfigGroup);

        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}
