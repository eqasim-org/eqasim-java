package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class AttributeCrossingPenalty implements CrossingPenalty {
    static public String ATTRIBUTE = "crossingPenalty";

    private IdMap<Link, Double> delays = new IdMap<>(Link.class);
    private CrossingPenalty delegate;

    public AttributeCrossingPenalty(IdMap<Link, Double> delays, CrossingPenalty delegate) {
        this.delays = delays;
        this.delegate = delegate;
    }

    @Override
    public double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        Double value = delays.get(link.getId());
        return value != null ? value : delegate.calculateCrossingPenalty(link);
    }

    public static AttributeCrossingPenalty sbuild(Network network, CrossingPenalty delegate) {
        IdMap<Link, Double> delays = new IdMap<>(Link.class);

        for (Link link : network.getLinks().values()) {
            Double delay = (Double) link.getAttributes().getAttribute(ATTRIBUTE);

            if (delay != null) {
                delays.put(link.getId(), delay);
            }
        }

        return new AttributeCrossingPenalty(delays, delegate);
    }
}
