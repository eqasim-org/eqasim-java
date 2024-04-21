package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector;

import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

public interface AccessEgressStopsSelector {
    Facility getAccessFacility(RoutingRequest request);
    Facility getEgressFacility(RoutingRequest request);
}
