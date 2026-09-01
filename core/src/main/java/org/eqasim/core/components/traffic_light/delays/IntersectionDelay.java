package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.TimeBinManager;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import java.util.concurrent.ConcurrentHashMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class IntersectionDelay implements CrossingPenalty {

    private final ConcurrentHashMap<Id<Vehicle>, Coord> lastDelayCoordinates = new ConcurrentHashMap<>();
    private final double minimumDistanceBetweenDelays; // meters
    private final double minimumDistanceBetweenDelaysSquared; // meters
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
        this.minimumDistanceBetweenDelaysSquared = minimumDistanceBetweenDelays * minimumDistanceBetweenDelays;
        this.applyUnsignalizedDelays = delayConfigGroup.isUnsignalizedActivated();
        this.applyTrafficLightDelays = delayConfigGroup.isTlActivated();
        this.startingIteration = delayConfigGroup.getStartingIteration();

        this.delegate = delegate;
        this.timeBinManager = timeBinManager;
        this.trafficLightDelays = trafficLightDelays;
        this.unsignalizedIntersectionDelay = unsignalizedIntersectionDelay;
    }

    public double calculateCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        // 2. Check if the traffic light delays and unsignalized intersection delays are activated
        // and check if a delay should be added based on the previous crossing position
        boolean timeOutOfBounds = time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime();
        boolean noneOfDelaysActivated = !applyUnsignalizedDelays && !applyTrafficLightDelays;
        boolean isBeforeStartingIteration = currentIteration < startingIteration;
        if (timeOutOfBounds || noneOfDelaysActivated|| isBeforeStartingIteration) {
            return delegate.calculateCrossingPenalty(link, time, vehicleId);
        }

        if (!isFarEnoughFromLastDelayedIntersection(link, vehicleId)) {
            // only add a delay if the vehicle has not crossed an intersection recently
            return 0.0;
        }
        // At this point, we know that at least one of the delays is activated and that we can add a delay based on the last intersection, time, iteration
        // 3. If the traffic light delays are not activated, we return the unsignalized intersection delay
        double delay;
        if (!applyTrafficLightDelays) {
            delay = unsignalizedIntersectionDelay.getDelay(link, time);
        } else {
            // 4. If the traffic light delays are activated, we calculate the delay based on the traffic light delays
            // and the unsignalized intersection delays if activated

            //---- 4.1 get first the traffic light delay
            float tlValue = trafficLightDelays.getDelay(link, time);

            //---- 4.2 In these cases, we return the crossing penalty of unsignalized intersections if activated or delegate
            if (returnUnsignalizedDelayInsteadOfTlDelay(tlValue)) {
                if (applyUnsignalizedDelays) {
                    delay = unsignalizedIntersectionDelay.getDelay(link, time);
                } else {
                    delay = delegate.calculateCrossingPenalty(link, time, vehicleId);
                }
            } else {
                //---- 4.4 Otherwise, the returned value is the actual delay
                delay = tlValue;
            }
        }

        if (delay > 0.0 && vehicleId != null) {
            recordLastDelayedIntersection(vehicleId, link.getToNode().getCoord());
        }
        return delay;
    }

    private boolean returnUnsignalizedDelayInsteadOfTlDelay(float tlValue) {
        // If the link has no traffic light, we return the unsignalized intersection delay
        return tlValue == TrafficLightDelay.NO_TL ||
                tlValue == TrafficLightDelay.OUT_OF_BOUNDS ||
                tlValue == TrafficLightDelay.INCORRECT_DELAY;
    }

    private boolean isFarEnoughFromLastDelayedIntersection(Link link, Id<Vehicle> vehicleId) {
        if (vehicleId == null) {
            return true; // If we do not have a vehicle ID, we cannot check the last crossing position, so we allow adding the delay
        }

        // Only positive, applied delays are recorded in this map.
        Coord lastDelayLocation = lastDelayCoordinates.get(vehicleId);
        Coord nextIntersectionLocation = link.getToNode().getCoord();

        // If the vehicle has not crossed an intersection before, we should apply the delay
        if (lastDelayLocation == null){
            return true;
        }

        // Calculate squared distance to avoid the expensive sqrt call
        double xDiff = lastDelayLocation.getX() - nextIntersectionLocation.getX();
        double yDiff = lastDelayLocation.getY() - nextIntersectionLocation.getY();
        double distance2 = xDiff * xDiff + yDiff * yDiff;
        return distance2 > minimumDistanceBetweenDelaysSquared;
    }

    private void recordLastDelayedIntersection(Id<Vehicle> vehicleId, Coord coord) {
        lastDelayCoordinates.put(vehicleId, coord);
    }

    public void updateIteration(int iteration) {
        this.currentIteration = iteration;
    }


}
