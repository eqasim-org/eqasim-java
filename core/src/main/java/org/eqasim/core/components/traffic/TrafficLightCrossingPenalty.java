package org.eqasim.core.components.traffic;

import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class TrafficLightCrossingPenalty implements CrossingPenalty {

    private final TrafficLightDelay tlDelays;
    private final CrossingPenalty delegate;

    public TrafficLightCrossingPenalty(TrafficLightDelay tlDelays, CrossingPenalty delegate) {
        this.tlDelays = tlDelays;
        this.delegate = delegate;
    }

    @Override
    public double calculateCrossingPenalty(Link link, double time) {
        double tlValue = tlDelays.getDelay(link, time);
        return tlValue >0.0 ? tlValue : delegate.calculateCrossingPenalty(link, time);
    }

    public static TrafficLightCrossingPenalty build(Network network, CrossingPenalty delegate,
                                                    TrafficLightDelay tlDelays) {
        // here delegate is the attribute crossing penalty
        return new TrafficLightCrossingPenalty(tlDelays, delegate);
    }
}
