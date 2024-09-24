package org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_stop_selection;

import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;



public class ClosestAccessEgressStopSelector implements AccessEgressStopSelector {

    @Override
    public Facility getAccessFacility(RoutingRequest request, QuadTree<Facility> candidateFacilities) {
        return candidateFacilities.getClosest(request.getFromFacility().getCoord().getX(), request.getFromFacility().getCoord().getY());
    }

    @Override
    public Facility getEgressFacility(RoutingRequest request, QuadTree<Facility> candidateFacilities) {
        return candidateFacilities.getClosest(request.getToFacility().getCoord().getX(), request.getToFacility().getCoord().getY());
    }
}
