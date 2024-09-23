package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_selection;

import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import org.matsim.core.utils.collections.QuadTree;

public interface AccessEgressStopSelector {
    Facility getAccessFacility(RoutingRequest request, QuadTree<Facility> candidateFacilities);
    Facility getEgressFacility(RoutingRequest request, QuadTree<Facility> candidateFacilities);
}
