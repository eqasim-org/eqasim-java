package org.eqasim.san_francisco.bike.reader;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class BikeInfo {
    public Id<Link> linkId;
    public String facility;
    public Boolean opposite;
    public Boolean permitted;

    public BikeInfo(Id<Link> linkId, String facility, Boolean opposite, Boolean permitted) {
        this.linkId = linkId;
        this.facility = facility;
        this.opposite = opposite;
        this.permitted = permitted;
    }
}
