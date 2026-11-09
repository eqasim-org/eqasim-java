package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import org.matsim.core.router.RoutingRequest;

/** Run-local person index and exact departure-time bits; no attributes, coordinates or digest. */
record PtRouteKey(int person, long departure, long generation) {
    static PtRouteKey of(RoutingRequest request, long generation) {
        if (request.getPerson() == null || !Double.isFinite(request.getDepartureTime())) return null;
        // MATSim's interned person IDs provide a unique integer for the duration of this run.
        return new PtRouteKey(request.getPerson().getId().index(),
                Double.doubleToLongBits(request.getDepartureTime()), generation);
    }
}
