package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eqasim.core.simulation.modes.feeder_drt.config.AccessEgressStopSearchParams;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.Arrays;

public class TransitStopByIdAccessEgressStopSearchParameterSet extends AccessEgressStopSearchParams {

    public static final String NAME = "TransitStopByIdAccessEgressStopSearch";

    @Parameter
    @NotNull
    @NotBlank
    public String ids;

    public TransitStopByIdAccessEgressStopSearchParameterSet() {
        super(NAME);
    }


    public IdSet<TransitStopFacility> getTransitStopIds() {
        IdSet<TransitStopFacility> idSet = new IdSet<>(TransitStopFacility.class);
        Arrays.stream(ids.split(",")).map(id -> Id.create(id, TransitStopFacility.class)).forEach(idSet::add);
        return idSet;
    }
}
