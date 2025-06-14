package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AttributeCrossingPenalty implements CrossingPenalty {
    static public String ATTRIBUTE = "crossingPenalty";

    private IdMap<Link, Double> delays = new IdMap<>(Link.class);
    private double defaultValue;

    public AttributeCrossingPenalty(IdMap<Link, Double> delays, double defaultValue) {
        this.delays = delays;
        this.defaultValue = defaultValue;
    }

    @Override
    public double calculateCrossingPenalty(Link link) {
        return delays.getOrDefault(link.getId(), defaultValue);
    }

    public static AttributeCrossingPenalty sbuild(Network network, double defaultValue) {
        IdMap<Link, Double> delays = new IdMap<>(Link.class);

        for (Link link : network.getLinks().values()) {
            Double delay = (Double) link.getAttributes().getAttribute(ATTRIBUTE);

            if (delay != null) {
                delays.put(link.getId(), delay);
            }
        }

        return new AttributeCrossingPenalty(delays, defaultValue);
    }
}
