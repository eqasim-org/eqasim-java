package org.eqasim.core.simulation.modes.feeder_drt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search.TransitStopByModeAccessEgressStopSearchParameterSet;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.dvrp.run.Modal;

public class FeederDrtConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {
    public static final String GROUP_NAME = "feederDrt";
    public FeederDrtConfigGroup() {
        super(GROUP_NAME);

        //AccessEgressStopSearch
        this.addDefinition(TransitStopByModeAccessEgressStopSearchParameterSet.NAME, TransitStopByModeAccessEgressStopSearchParameterSet::new, () -> this.accessEgressStopSearchParams,
                params -> this.accessEgressStopSearchParams = (TransitStopByModeAccessEgressStopSearchParameterSet) params);
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

    @Parameter
    @Comment("A regex that if matches with the from facility id (resp to facility id) will result in an access (resp egress) drt leg not being constructed. Leave empty to consider access and egress segments at all times")
    public String skipAccessAndEgressAtFacilities;

    @NotNull
    public AccessEgressStopSearchParams accessEgressStopSearchParams;

    public enum AccessEgressStopSelection {CLOSEST};

    @Parameter
    @NotNull
    public AccessEgressStopSelection accessEgressStopSelection = AccessEgressStopSelection.CLOSEST;

    @Override
    public String getMode() {
        return this.mode;
    }
}
