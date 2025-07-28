package org.eqasim.core.components.traffic;

import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

public class TrafficLightCrossingPenalty implements CrossingPenalty {

    private final TrafficLightDelay tlDelays;
    private final CrossingPenalty delegate;

    public TrafficLightCrossingPenalty(TrafficLightDelay tlDelays, CrossingPenalty delegate) {
        this.tlDelays = tlDelays;
        this.delegate = delegate;
    }

    @Override
    public double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        double tlValue = tlDelays.getDelay(link, time, vehicleId);
        // in these special cases, we delegate to the original crossing penalty
        if (tlValue== TrafficLightDelay.NO_TL || tlValue == TrafficLightDelay.BEFORE_TL ||
                tlValue == TrafficLightDelay.OUT_OF_BOUNDS || tlValue == TrafficLightDelay.INCORRECT_DELAY) {
            return delegate.calculateCrossingPenalty(link);
        }
        // If there's explicitly no delay, return 0
        if (tlValue == TrafficLightDelay.NO_DELAY) {
            return 0.0;
        }
        // Otherwise, the returned value is the actual delay
        return tlValue;
    }

    public static TrafficLightCrossingPenalty build(Network network, CrossingPenalty delegate,
                                                    TrafficLightDelay tlDelays) {
        // here delegate is the attribute crossing penalty
        return new TrafficLightCrossingPenalty(tlDelays, delegate);
    }
}
