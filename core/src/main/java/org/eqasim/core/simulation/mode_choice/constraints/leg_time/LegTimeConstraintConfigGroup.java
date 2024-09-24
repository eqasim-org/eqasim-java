package org.eqasim.core.simulation.mode_choice.constraints.leg_time;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;

public class LegTimeConstraintConfigGroup extends ReflectiveConfigGroup {

    public final static String GROUP_NAME = "eqasim:legTimeConstraint";

    public LegTimeConstraintConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        if(LegTimeConstraintSingleLegConfigGroup.GROUP_NAME.equals(type)) {
            return new LegTimeConstraintSingleLegConfigGroup();
        }
        throw new IllegalStateException(String.format("Parameter set '%s' not supported", type));
    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if(set instanceof LegTimeConstraintSingleLegConfigGroup) {
            super.addParameterSet(set);
        } else {
            throw new IllegalStateException("Unsupported parameter set class: " + set);
        }
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        getSingleLegParameterSetByMainModeByLegMode();
    }

    public static LegTimeConstraintConfigGroup getOrCreate(Config config) {
        LegTimeConstraintConfigGroup legTimeConstraintConfigGroup = (LegTimeConstraintConfigGroup) config.getModules().get(LegTimeConstraintConfigGroup.GROUP_NAME);
        if(legTimeConstraintConfigGroup == null) {
            legTimeConstraintConfigGroup = new LegTimeConstraintConfigGroup();
            config.addModule(legTimeConstraintConfigGroup);
        }
        return legTimeConstraintConfigGroup;
    }

    public Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> getSingleLegParameterSetByMainModeByLegMode() {
        Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> singleLegParameterSetByMainModeByLegMode = new HashMap<>();
        Collection<? extends ConfigGroup> parameterSets = getParameterSets(LegTimeConstraintSingleLegConfigGroup.GROUP_NAME);
        for(ConfigGroup parameterSet: parameterSets) {
            if(!(parameterSet instanceof LegTimeConstraintSingleLegConfigGroup legTimeConstraintSingleLegConfigGroup)) {
                throw new IllegalStateException("This should not happen");
            }
            if(singleLegParameterSetByMainModeByLegMode.computeIfAbsent(legTimeConstraintSingleLegConfigGroup.mainMode, key -> new HashMap<>()).containsKey(legTimeConstraintSingleLegConfigGroup.legMode)) {
                throw new IllegalStateException(String.format("The combination (mainMode, legMode) = (%s, %s) appears more than once in the %s module config",
                        legTimeConstraintSingleLegConfigGroup.mainMode,
                        legTimeConstraintSingleLegConfigGroup.legMode,
                        GROUP_NAME));
            }
            singleLegParameterSetByMainModeByLegMode.get(legTimeConstraintSingleLegConfigGroup.mainMode)
                    .put(legTimeConstraintSingleLegConfigGroup.legMode, legTimeConstraintSingleLegConfigGroup);
        }
        return singleLegParameterSetByMainModeByLegMode;
    }
}
