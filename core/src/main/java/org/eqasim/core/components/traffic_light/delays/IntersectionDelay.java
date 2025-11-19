package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

public class IntersectionDelay implements CrossingPenalty {

    private final IdMap<Vehicle, Coord> lastDelayCoordinates = new IdMap<Vehicle, Coord>(Vehicle.class);
    private final double minimumDistanceBetweenDelays; // meters
    private final TrafficLightDelay trafficLightDelays;
    private final UnsignalizedIntersectionDelay unsignalizedIntersectionDelay;

    private final TimeBinManager timeBinManager;
    private final boolean applyUnsignalizedDelays; // Flag to apply unsignalized intersection delays
    private final boolean applyTrafficLightDelays; // Flag to apply traffic light delays
    private final int startingIteration;
    private int currentIteration = 0;
    private final CrossingPenalty delegate;

    public IntersectionDelay(DelaysConfigGroup delayConfigGroup,
                             TrafficLightDelay trafficLightDelays,
                             UnsignalizedIntersectionDelay unsignalizedIntersectionDelay,
                             TimeBinManager timeBinManager,
                             CrossingPenalty delegate) {
        this.minimumDistanceBetweenDelays = delayConfigGroup.getMinimumDistanceBetweenDelays();
        this.applyUnsignalizedDelays = delayConfigGroup.isUnsignalizedActivated();
        this.applyTrafficLightDelays = delayConfigGroup.isTlActivated();
        this.startingIteration = delayConfigGroup.getStartingIteration();

        this.delegate = delegate;
        this.timeBinManager = timeBinManager;
        this.trafficLightDelays = trafficLightDelays;
        this.unsignalizedIntersectionDelay = unsignalizedIntersectionDelay;
    }

    public double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        // 1. Check if the traffic light delays and unsignalized intersection delays are activated
        // and check if a delay should be added based on the previous crossing position
        boolean timeOutOfBounds = time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime();
        boolean noneOfDelaysActivated = !applyUnsignalizedDelays && !applyTrafficLightDelays;
        boolean isBeforeStartingIteration = currentIteration < startingIteration;
        if (timeOutOfBounds || noneOfDelaysActivated|| isBeforeStartingIteration) {
            return delegate.calculateCrossingPenalty(link, time, vehicleId);
        }

        if (!couldAddDelayBasedOnLastIntersection(link, vehicleId)) {
            // only add a delay if the vehicle has not crossed an intersection recently
            return 0.0;
        }
        // At this point, we know that at least one of the delays is activated and that we can add a delay based on the last intersection, time, iteration
        // 2. If the traffic light delays are not activated, we return the unsignalized intersection delay
        if (!applyTrafficLightDelays) {
            return unsignalizedIntersectionDelay.getDelay(link, time);
        }
        // 3. If the traffic light delays are activated, we calculate the delay based on the traffic light delays
        // and the unsignalized intersection delays if activated

        //---- 3.1 get first the traffic light delay
        double tlValue = trafficLightDelays.getDelay(link, time);

        //---- 3.2 In these cases, we return the crossing penalty of unsignalized intersections if activated or delegate
        if (returnUnsignalizedDelayInsteadOfTlDelay(tlValue)) {
            if (applyUnsignalizedDelays) {
                return unsignalizedIntersectionDelay.getDelay(link, time);
            } else {
                return delegate.calculateCrossingPenalty(link, time, vehicleId);
            }
        }

        //---- 3.4 Otherwise, the returned value is the actual delay
        return tlValue;
    }

    private boolean returnUnsignalizedDelayInsteadOfTlDelay(double tlValue) {
        // If the link has no traffic light, we return the unsignalized intersection delay
        return (Math.abs(tlValue-TrafficLightDelay.NO_TL)<1e-6 ||
                Math.abs(tlValue-TrafficLightDelay.OUT_OF_BOUNDS)<1e-6 ||
                Math.abs(tlValue-TrafficLightDelay.INCORRECT_DELAY)<1e-6) ;
    }

    private boolean couldAddDelayBasedOnLastIntersection(Link link, Id<Vehicle> vehicleId) {
        // In this function I check if the vehicle has crossed a traffic light recently
        // If it has, and the distance is lower than the threshold, we do not apply the delay again
        Coord lastDelayLocation = lastDelayCoordinates.get(vehicleId);
        Coord nextIntersectionLocation = link.getToNode().getCoord();

        // If the vehicle has not crossed an intersection before, we should apply the delay
        if (lastDelayLocation == null){
            updateLastDelayCoordinates(vehicleId, nextIntersectionLocation);
            return true;
        }

        // Calculate distance from last intersection where a delay was applied
        double distanceSinceLastDelay = CoordUtils.calcEuclideanDistance(lastDelayLocation, nextIntersectionLocation);

        // if distance higher than the threshold, we should apply the traffic light delay, and update the last known traffic light position
        if (distanceSinceLastDelay > minimumDistanceBetweenDelays){
            // Update the last delay coordinates
            updateLastDelayCoordinates(vehicleId, nextIntersectionLocation);
            return true;
        }else {
            return false;
        }
    }

    private void updateLastDelayCoordinates(Id<Vehicle> vehicleId, Coord coord) {
        lastDelayCoordinates.put(vehicleId, coord);
    }

    public void updateIteration(int iteration) {
        this.currentIteration = iteration;
    }


}
