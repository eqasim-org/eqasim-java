package org.eqasim.core.simulation.mode_choice.constraints.leg_time;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LegTimeConstraintSingleLegConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "singleLegTimeConstraintDefinition";

    @Parameter
    @Comment("Main mode in which the given leg type will be verified against the time slots")
    public @NotBlank String mainMode;

    @Parameter
    @Comment("Leg mode to verify against the time slots")
    public @NotBlank String legMode;

    @Parameter
    @Comment("Whether to ensure that both departure and arrival time are within the timeslots or departure time only")
    public @NotNull boolean checkBothDepartureAndArrivalTimes;

    public LegTimeConstraintSingleLegConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        if(TimeSlotConfigGroup.GROUP_NAME.equals(type)) {
            return new TimeSlotConfigGroup();
        }
        throw new IllegalStateException(String.format("Parameter set '%s' not supported", type));
    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if(set instanceof TimeSlotConfigGroup) {
            super.addParameterSet(set);
        } else {
            throw new IllegalStateException("Unsupported parameter set class: " + set);
        }
    }

    public List<TimeSlotConfigGroup> getTimeSlotsParameterSets() {
        List<TimeSlotConfigGroup> timeSlotsParameterSets = new ArrayList<>();
        Collection<? extends ConfigGroup> parameterSets = getParameterSets(TimeSlotConfigGroup.GROUP_NAME);
        for(ConfigGroup parameterSet: parameterSets) {
            if(!(parameterSet instanceof TimeSlotConfigGroup timeSlotsParameterSet)) {
                throw new IllegalStateException("This should not happen");
            }
            timeSlotsParameterSets.add(timeSlotsParameterSet);
        }
        return timeSlotsParameterSets;
    }

    @Override
    protected void checkConsistency(Config config) {
        if(getParameterSets().size() == 0) {
            throw new IllegalStateException(String.format("%s parameter set must have at least one %s child parameter set", LegTimeConstraintSingleLegConfigGroup.GROUP_NAME, TimeSlotConfigGroup.GROUP_NAME));
        }
        super.checkConsistency(config);
    }

    public static class TimeSlotConfigGroup extends ReflectiveConfigGroup {
        public static final String GROUP_NAME = "timeSlot";

        @Parameter
        @Comment("Begin time of the slot")
        public @NotNull double beginTime;

        @Parameter
        @Comment("End time of the slot")
        public @NotNull double endTime;

        public TimeSlotConfigGroup() {
            super(GROUP_NAME);
        }

        @Override
        protected void checkConsistency(Config config) {
            if(endTime < beginTime) {
                throw new IllegalStateException("beginTime must be less or equal than endTime");
            }
        }
    }
}
