package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic.bike.BikeGradientBasedLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeTravelTime;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.TimeBinManager;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestBikeRoutingDelay {
    @Test
    public void routingIsRepeatableWithOrWithoutVehicleAndDoesNotChangeSimulationHistory() {
        var config = new DelaysConfigGroup();
        config.setActivateUnsignalized(true);
        config.setStartingIteration(0);
        config.setMinimumDistanceBetweenDelays(30);
        var bins = mock(TimeBinManager.class);
        when(bins.getEndTime()).thenReturn(1000.0);
        var unsignalized = mock(UnsignalizedIntersectionDelay.class);
        var delay = new IntersectionDelay(config, mock(TrafficLightDelay.class), unsignalized,
                bins, (link, time, id) -> 0);
        var calculator = new BikeGradientBasedLinkSpeedCalculator(delay);
        var routing = new BikeTravelTime(calculator);
        var vehicle = bike();
        var first = link("first", 0, 100);
        var nearby = link("nearby", 10, 100);
        var distant = link("distant", 100, 100);
        when(unsignalized.getDelay(any(Link.class), anyDouble())).thenReturn(10.0F);
        double freeflow = 100 / 5.9;

        for (Vehicle candidate : new Vehicle[]{vehicle, null}) {
            assertEquals(freeflow + 5, routing.getLinkTravelTime(first, 100, null, candidate), 1e-9);
            assertEquals(freeflow + 5, routing.getLinkTravelTime(first, 100, null, candidate), 1e-9);
        }
        // The routing calls must not suppress the first actual simulated delay.
        assertEquals(freeflow + 5, 100 / calculator.getMaximumBikeVelocity(vehicle, first, 100), 1e-9);

        // Routing ignores actual vehicle history, even for a nearby intersection.
        assertEquals(freeflow + 5, routing.getLinkTravelTime(nearby, 100, null, vehicle), 1e-9);
        // Exploring a distant alternative must not replace the actual last position.
        assertEquals(freeflow + 5, routing.getLinkTravelTime(distant, 100, null, vehicle), 1e-9);
        assertEquals(freeflow, 100 / calculator.getMaximumBikeVelocity(vehicle, nearby, 100), 1e-9);
        assertEquals(freeflow + 5, 100 / calculator.getMaximumBikeVelocity(vehicle, distant, 100), 1e-9);
    }

    @Test
    public void routingPreservesLowSpeedExemptionAndVehicleSpeedLimit() {
        var penalty = mock(org.eqasim.core.components.traffic.CrossingPenalty.class);
        var calculator = new BikeGradientBasedLinkSpeedCalculator(penalty);
        var routing = new BikeTravelTime(calculator);
        var link = link("slow", 0, 2.5);
        assertEquals(40, routing.getLinkTravelTime(link, 100, null, null), 1e-9);
        verifyNoInteractions(penalty);

        link.setFreespeed(100);
        var vehicle = bike();
        vehicle.getType().setMaximumVelocity(4);
        when(penalty.estimateCrossingPenalty(link, 100)).thenReturn(10.0);
        assertEquals(30, routing.getLinkTravelTime(link, 100, null, vehicle), 1e-9);
        verify(penalty).estimateCrossingPenalty(link, 100);
        verify(penalty, never()).calculateCrossingPenalty(any(), anyDouble(), any());
    }

    private static Vehicle bike() {
        var type = VehicleUtils.createVehicleType(Id.create("bike", org.matsim.vehicles.VehicleType.class));
        type.setNetworkMode("bike");
        type.setMaximumVelocity(10);
        return VehicleUtils.createVehicle(Id.createVehicleId("bike"), type);
    }

    private static Link link(String id, double x, double speed) {
        var network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId(id + "From"), new Coord(x - 100, 0));
        var to = factory.createNode(Id.createNodeId(id + "To"), new Coord(x, 0));
        var link = factory.createLink(Id.createLinkId(id), from, to);
        link.setLength(100);
        link.setFreespeed(speed);
        return link;
    }
}
