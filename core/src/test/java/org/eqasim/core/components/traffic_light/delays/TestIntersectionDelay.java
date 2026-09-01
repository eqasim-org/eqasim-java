package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.TimeBinManager;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.Vehicle;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestIntersectionDelay {
    private static final double TIME = 100.0;
    private static final Id<Vehicle> VEHICLE_ID = Id.create("vehicle", Vehicle.class);

    @Test
    public void zeroDelayDoesNotCreateLastDelayedIntersection() {
        UnsignalizedIntersectionDelay unsignalizedDelay = mock(UnsignalizedIntersectionDelay.class);
        IntersectionDelay intersectionDelay = createIntersectionDelay(unsignalizedDelay);
        Link zeroDelayLink = createLink("zero", 0.0);
        Link nearbyDelayedLink = createLink("nearby", 10.0);

        when(unsignalizedDelay.getDelay(zeroDelayLink, TIME)).thenReturn(0.0F);
        when(unsignalizedDelay.getDelay(nearbyDelayedLink, TIME)).thenReturn(5.0F);

        assertEquals(0.0, intersectionDelay.calculateCrossingPenalty(zeroDelayLink, TIME, VEHICLE_ID), 0.0);
        assertEquals(5.0, intersectionDelay.calculateCrossingPenalty(nearbyDelayedLink, TIME, VEHICLE_ID), 0.0);
    }

    @Test
    public void zeroDelayDoesNotReplaceExistingLastDelayedIntersection() {
        UnsignalizedIntersectionDelay unsignalizedDelay = mock(UnsignalizedIntersectionDelay.class);
        IntersectionDelay intersectionDelay = createIntersectionDelay(unsignalizedDelay);
        Link firstDelayedLink = createLink("first", 0.0);
        Link zeroDelayLink = createLink("zero", 100.0);
        Link secondDelayedLink = createLink("second", 110.0);

        when(unsignalizedDelay.getDelay(firstDelayedLink, TIME)).thenReturn(5.0F);
        when(unsignalizedDelay.getDelay(zeroDelayLink, TIME)).thenReturn(0.0F);
        when(unsignalizedDelay.getDelay(secondDelayedLink, TIME)).thenReturn(5.0F);

        assertEquals(5.0, intersectionDelay.calculateCrossingPenalty(firstDelayedLink, TIME, VEHICLE_ID), 0.0);
        assertEquals(0.0, intersectionDelay.calculateCrossingPenalty(zeroDelayLink, TIME, VEHICLE_ID), 0.0);
        assertEquals(5.0, intersectionDelay.calculateCrossingPenalty(secondDelayedLink, TIME, VEHICLE_ID), 0.0);
    }

    @Test
    public void nearbyDelayIsSuppressedAfterPositiveDelay() {
        UnsignalizedIntersectionDelay unsignalizedDelay = mock(UnsignalizedIntersectionDelay.class);
        IntersectionDelay intersectionDelay = createIntersectionDelay(unsignalizedDelay);
        Link firstDelayedLink = createLink("first", 0.0);
        Link nearbyDelayedLink = createLink("nearby", 10.0);
        Link distantDelayedLink = createLink("distant", 40.0);

        when(unsignalizedDelay.getDelay(firstDelayedLink, TIME)).thenReturn(5.0F);
        when(unsignalizedDelay.getDelay(nearbyDelayedLink, TIME)).thenReturn(5.0F);
        when(unsignalizedDelay.getDelay(distantDelayedLink, TIME)).thenReturn(5.0F);

        assertEquals(5.0, intersectionDelay.calculateCrossingPenalty(firstDelayedLink, TIME, VEHICLE_ID), 0.0);
        assertEquals(0.0, intersectionDelay.calculateCrossingPenalty(nearbyDelayedLink, TIME, VEHICLE_ID), 0.0);
        assertEquals(5.0, intersectionDelay.calculateCrossingPenalty(distantDelayedLink, TIME, VEHICLE_ID), 0.0);
    }

    private IntersectionDelay createIntersectionDelay(UnsignalizedIntersectionDelay unsignalizedDelay) {
        DelaysConfigGroup config = new DelaysConfigGroup();
        config.setActivateUnsignalized(true);
        config.setStartingIteration(0);
        config.setMinimumDistanceBetweenDelays(30.0);

        TimeBinManager timeBinManager = mock(TimeBinManager.class);
        when(timeBinManager.getStartTime()).thenReturn(0.0);
        when(timeBinManager.getEndTime()).thenReturn(1000.0);

        CrossingPenalty delegate = (link, time, vehicleId) -> 0.0;
        return new IntersectionDelay(config, mock(TrafficLightDelay.class), unsignalizedDelay,
                timeBinManager, delegate);
    }

    private Link createLink(String id, double x) {
        Node toNode = mock(Node.class);
        when(toNode.getCoord()).thenReturn(new Coord(x, 0.0));

        Link link = mock(Link.class);
        when(link.getId()).thenReturn(Id.createLinkId(id));
        when(link.getToNode()).thenReturn(toNode);
        return link;
    }
}
