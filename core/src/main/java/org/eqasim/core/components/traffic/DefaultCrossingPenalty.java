package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class DefaultCrossingPenalty implements CrossingPenalty {
    private final IdSet<Link> active;
    private final double crossingPenalty;

    public DefaultCrossingPenalty(IdSet<Link> active, double crossingPenalty) {
        this.active = active;
        this.crossingPenalty = crossingPenalty;
    }

    @Override
    public double calculateCrossingPenalty(Link link) {
        return active.contains(link.getId()) ? crossingPenalty : 0.0;
    }

    static public DefaultCrossingPenalty build(Network network, double crossingPenalty) {
        IdSet<Link> active = new IdSet<>(Link.class);

        for (Link link : network.getLinks().values()) {
            boolean isMajor = true;

            for (Link other : link.getToNode().getInLinks().values()) {
                if (other.getCapacity() >= link.getCapacity()) {
                    isMajor = false;
                }
            }

            if (!isMajor && link.getToNode().getInLinks().size() != 1) {
                active.add(link.getId());
            }
        }

        return new DefaultCrossingPenalty(active, crossingPenalty);
    }
}
