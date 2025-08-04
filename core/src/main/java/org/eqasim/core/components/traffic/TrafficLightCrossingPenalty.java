package org.eqasim.core.components.traffic;

import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.delays.UnsignalizedIntersectionDelay;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

public class TrafficLightCrossingPenalty implements CrossingPenalty {

    private final TrafficLightDelay tlDelays;
    private final CrossingPenalty delegate;
    private final UnsignalizedIntersectionDelay usDelays;
    private final IdMap<Vehicle, Coord> lastDelayCoordinates = new IdMap<Vehicle, Coord>(Vehicle.class);
    private final double minimumDistanceBetweenDelays = 30.0; // meters


    public TrafficLightCrossingPenalty(TrafficLightDelay tlDelays, UnsignalizedIntersectionDelay usDelays, CrossingPenalty delegate) {
        this.tlDelays = tlDelays;
        this.delegate = delegate;
        this.usDelays = usDelays;
    }

    @Override
    public double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        if (!shouldAddDelay(link, vehicleId)) {
            return 0.0;
        }

        // get first the traffic light delay
        double tlValue = tlDelays.getDelay(link, time, vehicleId);

        // In these cases, we return the crossing penalty of unsignalized intersections
        if (tlValue== TrafficLightDelay.NO_TL || tlValue == TrafficLightDelay.BEFORE_TL ||
                tlValue == TrafficLightDelay.OUT_OF_BOUNDS || tlValue == TrafficLightDelay.INCORRECT_DELAY) {
            Double usValue = usDelays.getDelay(link, time);
            return usValue!=null ? usValue:delegate.calculateCrossingPenalty(link);
        }

        // If there's explicitly no delay, return 0
        if (tlValue == TrafficLightDelay.NO_DELAY) {
            return 0.0;
        }
        // Otherwise, the returned value is the actual delay
        return tlValue;
    }

    private boolean shouldAddDelay(Link link, Id<Vehicle> vehicleId) {
        // link should have a traffic light
        Coord lastDelayLocation = lastDelayCoordinates.get(vehicleId);
        Coord nextIntersectionLocation = link.getToNode().getCoord();

        // If the vehicle has not crossed a traffic light before, we should apply the delay
        if (lastDelayLocation == null){
            lastDelayCoordinates.put(vehicleId, nextIntersectionLocation);
            return true;
        }

        // Calculate distance from last traffic light position
        double distanceSinceLastTrafficLight = CoordUtils.calcEuclideanDistance(lastDelayLocation, nextIntersectionLocation);

        // if distance higher than the threshold, we should apply the traffic light delay, and update the last known traffic light position
        if (distanceSinceLastTrafficLight > minimumDistanceBetweenDelays){
            // Update the last known traffic light position to current link's end
            lastDelayCoordinates.put(vehicleId, nextIntersectionLocation);
            return true;
        }else {
            return false;
        }
    }







    public static TrafficLightCrossingPenalty build(Network network, CrossingPenalty delegate,
                                                    TrafficLightDelay tlDelays, UnsignalizedIntersectionDelay usDelays) {
        // here delegate is the attribute crossing penalty
        return new TrafficLightCrossingPenalty(tlDelays, usDelays, delegate);
    }


}
