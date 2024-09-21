package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import org.eqasim.core.simulation.modes.feeder_drt.config.AccessEgressStopSearchParams;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TransitStopByModeAccessEgressStopSearchParameterSet extends AccessEgressStopSearchParams {

    public static final String NAME = "TransitStopByModeAccessEgressStopSearch";

    @Parameter
    @Comment("Comma separated list of PT transit modes (rail, bus...) where intermodality can happen, leave empty to allow intermodality everywhere")
    public String accessEgressTransitStopModes;

    public TransitStopByModeAccessEgressStopSearchParameterSet() {
        super(NAME);
    }

    public Collection<String> getAccessEgressTransitStopModes() {
        if(this.accessEgressTransitStopModes.length() == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(this.accessEgressTransitStopModes.split(",")).toList();
    }
}
