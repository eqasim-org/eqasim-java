package org.eqasim.switzerland.ch_cmdp.runtime;

import java.util.Map;
import org.matsim.core.config.ReflectiveConfigGroup;

/** Optional Swiss simulation settings; explicit command-line options take precedence. */
public final class SimulationRuntimeConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "switzerlandSimulation";
    private boolean teleportBike = false;

    public SimulationRuntimeConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter("teleportBike")
    public boolean isTeleportBike() {
        return teleportBike;
    }

    @StringSetter("teleportBike")
    public void setTeleportBike(boolean teleportBike) {
        this.teleportBike = teleportBike;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put("teleportBike", "Teleport bikes at 4 m/s with beeline distance factor 1.4, "
                + "without changing the network. Default: false. --teleport-bike overrides this setting. "
                + "Incompatible with --dsim-simulate-bikes true; DSim already teleports bikes by default.");
        return comments;
    }
}
