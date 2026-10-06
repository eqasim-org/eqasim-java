package org.eqasim.switzerland.ch_cmdp.tolls;

import org.matsim.api.core.v01.network.Link;

/** Reads the monetary cost per traversal directly from a link's numeric toll attribute. */
public class Tolls {
    public double getToll(Link link) {
        Object value = link.getAttributes().getAttribute("toll");
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}
