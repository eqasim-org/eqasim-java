package org.eqasim.core.simulation.modes.parking_aware_car.mode_choice;

import jakarta.validation.constraints.NotNull;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;

public class ParkingAwareNetworkModeChoiceConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String GROUP_NAME = "modeChoice";

    public enum PenaltyType {DETAILED, ZERO}

    @Parameter
    @NotNull
    private PenaltyType penaltyType;


    @Parameter
    private double rate = 0.3;

    public ParkingAwareNetworkModeChoiceConfigGroup() {
        super(GROUP_NAME);
    }

    public PenaltyType getPenaltyType() {
        return penaltyType;
    }

    public void setPenaltyType(PenaltyType penaltyType) {
        this.penaltyType = penaltyType;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}
