package org.eqasim.core.simulation.modes.feeder_drt.config;

import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;

public class MultiModeFeederDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<FeederDrtConfigGroup> {

    public static final String GROUP_NAME = "multiModeFeederDrtModule";

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
}
