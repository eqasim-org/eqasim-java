package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ClosestAccessEgressStopSelectorParameterSet extends ReflectiveConfigGroup {

    public static final String NAME = "closestAccessEgressStopSelector";
    public ClosestAccessEgressStopSelectorParameterSet() {
        super(NAME);
    }
    @Parameter
    @Comment("Comma separated list of PT transit modes (rail, bus...) where intermodality can happen, leave empty to allow intermodality everywhere")
    public String accessEgressTransitStopModes;

    @Parameter
    @Comment("A regex that if matches with the from facility id (resp to facility id) will result in an access (resp egress) drt leg not being constructed. Leave empty to consider access and egress segments at all times")
    public String skipAccessAndEgressAtFacilities;

    public Collection<String> getAccessEgressTransitStopModes() {
        if(this.accessEgressTransitStopModes.length() == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(this.accessEgressTransitStopModes.split(",")).toList();
    }
}
