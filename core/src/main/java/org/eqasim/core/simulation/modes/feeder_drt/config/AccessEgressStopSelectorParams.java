package org.eqasim.core.simulation.modes.feeder_drt.config;

import jakarta.validation.constraints.NotNull;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector.ClosestAccessEgressStopSelectorParameterSet;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.ReflectiveConfigGroup;

public class AccessEgressStopSelectorParams extends ReflectiveConfigGroupWithConfigurableParameterSets {
    public static final String NAME = "accessEgressStopSelector";

    @NotNull
    private ReflectiveConfigGroup accessEgressStopSelectorParams;

    public AccessEgressStopSelectorParams() {
        super(NAME);
        this.addDefinition(ClosestAccessEgressStopSelectorParameterSet.NAME, ClosestAccessEgressStopSelectorParameterSet::new,
                () -> this.accessEgressStopSelectorParams,
                params -> this.accessEgressStopSelectorParams = (ReflectiveConfigGroup) params);
    }

    public ReflectiveConfigGroup getAccessEgressStopSelectorParams() {
        return accessEgressStopSelectorParams;
    }
}
