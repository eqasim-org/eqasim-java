package org.eqasim.core.simulation.modes.feeder_drt.config;

import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class MultiModeFeederDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<FeederDrtConfigGroup> {

    public static final String GROUP_NAME = "multiModeFeederDrt";

    public MultiModeFeederDrtConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        if (type.equals(FeederDrtConfigGroup.GROUP_NAME)) {
            return new FeederDrtConfigGroup();
        } else {
            throw new IllegalArgumentException("Unsupported parameter set type: " + type);
        }
    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if (set instanceof FeederDrtConfigGroup) {
            super.addParameterSet(set);
        } else {
            throw new IllegalArgumentException("Unsupported parameter set class: " + set);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<FeederDrtConfigGroup> getModalElements() {
        return (Collection<FeederDrtConfigGroup>) this.getParameterSets(FeederDrtConfigGroup.GROUP_NAME);
    }

    public Map<String, FeederDrtConfigGroup> getModeConfigs() {
        return this.getModalElements().stream().collect(Collectors.toMap(FeederDrtConfigGroup::getMode, cfg -> cfg));
    }

    @Parameter
    @Comment("Whether or not to perform the analysis for feeder drt services. If set to true, will follow the analysis interval specified in the configuration of the eqasim module")
    public boolean performAnalysis=true;
}
