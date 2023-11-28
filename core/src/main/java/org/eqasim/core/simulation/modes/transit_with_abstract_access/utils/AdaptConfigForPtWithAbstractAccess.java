package org.eqasim.core.simulation.modes.transit_with_abstract_access.utils;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.AbstractAccessModuleConfigGroup;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

import java.nio.file.Path;

public class AdaptConfigForPtWithAbstractAccess {



    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("input-config-path", "output-config-path", "mode-name", "accesses-file-path")
                .build();

        Config config = ConfigUtils.loadConfig(commandLine.getOptionStrict("input-config-path"), new DiscreteModeChoiceConfigGroup(), new EqasimConfigGroup());
        String mode = commandLine.getOptionStrict("mode-name");
        String outputConfigPath = commandLine.getOptionStrict("output-config-path");

        DiscreteModeChoiceConfigGroup dmcConfigGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
        dmcConfigGroup.getTripConstraints().add(EqasimModeChoiceModule.ABSTRACT_ACCESS_CONSTRAINT_NAME);

        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
        String ptEstimator = eqasimConfigGroup.getEstimators().get("pt");
        if (ptEstimator == null) {
            throw new IllegalStateException("No utility estimator defined for mode pt");
        }
        eqasimConfigGroup.setEstimator(mode, ptEstimator);

        PlanCalcScoreConfigGroup planCalcScoreConfigGroup = (PlanCalcScoreConfigGroup) config.getModules().get(PlanCalcScoreConfigGroup.GROUP_NAME);
        PlanCalcScoreConfigGroup.ModeParams modeParams = new PlanCalcScoreConfigGroup.ModeParams(TransitWithAbstractAccessRoutingModule.ABSTRACT_ACCESS_LEG_MODE_NAME);
        modeParams.setMarginalUtilityOfTraveling(-1);
        planCalcScoreConfigGroup.addModeParams(modeParams);

        AbstractAccessModuleConfigGroup abstractAccessModuleConfigGroup = new AbstractAccessModuleConfigGroup();
        abstractAccessModuleConfigGroup.setModeName(mode);

        Path path = Path.of(outputConfigPath).getParent().toAbsolutePath().relativize(Path.of(commandLine.getOptionStrict("accesses-file-path")).toAbsolutePath());
        abstractAccessModuleConfigGroup.setAccessItemsFilePath(path.toString());
        config.addModule(abstractAccessModuleConfigGroup);

        ConfigUtils.writeConfig(config, outputConfigPath);
    }
}
