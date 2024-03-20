package org.eqasim.core.simulation.modes.feeder_drt.config;

import jakarta.validation.constraints.NotBlank;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.core.config.ReflectiveConfigGroup;

public class FeederDrtConfigGroup extends ReflectiveConfigGroup implements Modal {
    public static final String GROUP_NAME = "feederDrt";
    public FeederDrtConfigGroup() {
        super(GROUP_NAME);
    }

    @Parameter
    @Comment("The name by which the pt mode is known to the agents. Most usually pt")
    @NotBlank
    public String ptModeName = "pt";

    @Parameter
    @Comment("The name of the drt mode to use for access and egress segments")
    @NotBlank
    public String accessEgressModeName = "drt";

    @Parameter
    @Comment("Mode which will be handled by PassengerEngine and VrpOptimizer (passengers'/customers' perspective)")
    @NotBlank
    public String mode = "feederDrt";

    @Override
    public String getMode() {
        return this.mode;
    }
}
