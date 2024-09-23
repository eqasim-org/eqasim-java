package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_search;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;

import java.util.Collection;

public interface AccessEgressStopSearch {
    Collection<Facility> getAccessFacilitiesCollection();
    QuadTree<Facility> getAccessFacilitiesQuadTree();
    Collection<Facility> getEgressFacilitiesCollection();
    QuadTree<Facility> getEgressFacilitiesQuadTree();
}
