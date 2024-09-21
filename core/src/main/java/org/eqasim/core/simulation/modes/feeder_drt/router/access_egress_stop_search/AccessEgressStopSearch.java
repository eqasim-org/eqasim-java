package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;

public interface AccessEgressStopSearch {
    QuadTree<Facility> getAccessFacilities();
    QuadTree<Facility> getEgressFacilities();
}
