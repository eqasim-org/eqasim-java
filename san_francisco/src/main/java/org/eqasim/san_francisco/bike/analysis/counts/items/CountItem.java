package org.eqasim.san_francisco.bike.analysis.counts.items;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CountItem {
    public Id<Link> linkId;
    public String mode;
    public double time;
    public int count;

    public CountItem(Id<Link> linkId, String mode, double time, int count) {
        this.linkId = linkId;
        this.mode = mode;
        this.time = time;
        this.count = count;
    }

    public void increase(int amount) {
        this.count += amount;
    }

    public void reset() {
        this.count = 0;
    }
}
