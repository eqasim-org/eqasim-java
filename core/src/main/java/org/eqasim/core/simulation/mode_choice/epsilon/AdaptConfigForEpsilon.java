package org.eqasim.core.simulation.mode_choice.epsilon;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.util.Map;

public class AdaptConfigForEpsilon {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine  = new CommandLine.Builder(args).requireOptions("input-config-path", "output-config-path").build();

        Config config = ConfigUtils.loadConfig(commandLine.getOptionStrict("input-config-path"), new EqasimConfigGroup(), new DiscreteModeChoiceConfigGroup());
        commandLine.applyConfiguration(config);

        DiscreteModeChoiceConfigGroup discreteModeChoiceConfigGroup = (DiscreteModeChoiceConfigGroup) config.getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
        discreteModeChoiceConfigGroup.setSelector(SelectorModule.MAXIMUM);

        EqasimConfigGroup eqasimConfigGroup = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);


        for(Map.Entry<String, String> entry: eqasimConfigGroup.getEstimators().entrySet()) {
            if(entry.getValue().startsWith(EpsilonModule.EPSILON_UTILITY_PREFIX)) {
                continue;
            }
            eqasimConfigGroup.setEstimator(entry.getKey(), EpsilonModule.EPSILON_UTILITY_PREFIX + entry.getValue());
        }

        ConfigUtils.writeConfig(config, commandLine.getOptionStrict("output-config-path"));
    }
}
