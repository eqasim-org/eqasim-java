package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic.AttributeCrossingPenalty;
import org.eqasim.core.components.traffic.DefaultCrossingPenalty;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.DelaysModule;
import org.eqasim.core.components.traffic_light.TimeBinManager;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.Vehicle;
import org.eqasim.core.components.flow.FlowUtils;
import org.matsim.vehicles.Vehicles;
import java.util.Map;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.IdMap;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestIntersectionDelay {
    private static final double TIME = 100.0;
    private static final Id<Vehicle> VEHICLE_ID = Id.create("vehicle", Vehicle.class);
    private static final Id<Vehicle> BUS_ID = Id.create("transitVehicle", Vehicle.class);

    @Test
    public void moduleIdentifiesBusesByRouteMode() {
        Scenario scenario = mock(Scenario.class);
        TransitSchedule schedule = mock(TransitSchedule.class);
        TransitLine line = mock(TransitLine.class);
        TransitRoute route = mock(TransitRoute.class);
        Departure departure = mock(Departure.class);
        when(scenario.getTransitSchedule()).thenReturn(schedule);
        when(scenario.getVehicles()).thenReturn(mock(Vehicles.class));
        when(scenario.getTransitVehicles()).thenReturn(mock(Vehicles.class));
        when(schedule.getTransitLines()).thenReturn(Map.of(Id.create("line", TransitLine.class), line));
        when(line.getRoutes()).thenReturn(Map.of(Id.create("route", TransitRoute.class), route));
        when(route.getDepartures()).thenReturn(Map.of(Id.create("departure", Departure.class), departure));
        when(departure.getVehicleId()).thenReturn(BUS_ID);
        DelaysConfigGroup config = new DelaysConfigGroup();
        config.setStartingIteration(0);
        config.setActivateTl(true);
        config.setApplyTlToBuses(false);
        Link link = createLink("intersection", 0.0);
        TrafficLightDelay tl = mock(TrafficLightDelay.class);
        when(tl.hasTrafficLight(link)).thenReturn(true);
        when(tl.getDelay(link, TIME)).thenReturn(12.0F);
        TimeBinManager bins = mock(TimeBinManager.class);
        when(bins.getEndTime()).thenReturn(1000.0);
        for (String mode : new String[]{"bus", "tram"}) {
            when(route.getTransportMode()).thenReturn(mode);
            IntersectionDelay delay = new DelaysModule().provudeIntersectionDelay(config, tl,
                    mock(UnsignalizedIntersectionDelay.class), bins, mock(DefaultCrossingPenalty.class), new FlowUtils(scenario));
            assertEquals(mode.equals("bus") ? 0.0 : 12.0,
                    delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
        }
    }

    @Test
    public void attributeFallbackPreservesTimeAndVehicleId() {
        Link link = createLink("intersection", 0.0);
        CrossingPenalty delegate = mock(CrossingPenalty.class);
        when(delegate.calculateCrossingPenalty(link, TIME, BUS_ID)).thenReturn(7.0);
        AttributeCrossingPenalty penalty = new AttributeCrossingPenalty(new IdMap<>(Link.class), delegate);
        assertEquals(7.0, penalty.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
    }

    @Test
    public void busSwitchesAreIndependentAndDoNotAffectCars() {
        for (boolean signalized : new boolean[]{false, true}) {
            for (boolean applyTl : new boolean[]{false, true}) {
                for (boolean applyUnsignalized : new boolean[]{false, true}) {
                    DelaysConfigGroup config = new DelaysConfigGroup();
                    config.setApplyTlToBuses(applyTl);
                    config.setApplyUnsignalizedToBuses(applyUnsignalized);
                    Link link = createLink("intersection", 0.0);
                    IntersectionDelay delay = createBusDelay(config, link, signalized, true);
                    assertEquals((signalized ? applyTl : applyUnsignalized) ? (signalized ? 12.0 : 5.0) : 0.0,
                            delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
                    assertEquals(signalized ? 12.0 : 5.0,
                            delay.calculateCrossingPenalty(link, TIME, VEHICLE_ID), 0.0);
                    assertEquals(signalized ? 12.0 : 5.0,
                            delay.calculateCrossingPenalty(link, TIME, null), 0.0);
                }
            }
        }
    }

    @Test
    public void constantReplacesBothBusDelaysAndRespectsPrioritySwitches() {
        for (boolean signalized : new boolean[]{false, true}) {
            for (double constant : new double[]{0.0, 3.0}) {
                DelaysConfigGroup config = new DelaysConfigGroup();
                config.setConstantBusDelay(constant);
                Link link = createLink("intersection", 0.0);
                IntersectionDelay delay = createBusDelay(config, link, signalized, true);
                assertEquals(constant, delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
                assertEquals(signalized ? 12.0 : 5.0,
                        delay.calculateCrossingPenalty(link, TIME, VEHICLE_ID), 0.0);
                config.setApplyTlToBuses(false);
                config.setApplyUnsignalizedToBuses(false);
                delay = createBusDelay(config, link, signalized, true);
                assertEquals(0.0, delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
            }
        }
    }

    @Test
    public void constantDoesNotDelayOrdinaryRoadConnections() {
        DelaysConfigGroup config = new DelaysConfigGroup();
        config.setConstantBusDelay(3.0);
        Link link = createLink("connection", 0.0);
        IntersectionDelay delay = createBusDelay(config, link, false, false);
        assertEquals(0.0, delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
    }

    @Test
    public void busSettingsKeepTimeAndIterationLimits() {
        DelaysConfigGroup config = new DelaysConfigGroup();
        config.setConstantBusDelay(3.0);
        config.setApplyTlToBuses(false);
        Link link = createLink("intersection", 0.0);
        IntersectionDelay delay = createBusDelay(config, link, true, true);
        assertEquals(2.0, delay.calculateCrossingPenalty(link, 1001.0, BUS_ID), 0.0);
        delay.updateIteration(-1);
        assertEquals(2.0, delay.calculateCrossingPenalty(link, TIME, BUS_ID), 0.0);
    }

    @Test
    public void iterationResetClearsPreviousVehiclePositions() {
        UnsignalizedIntersectionDelay unsignalized = mock(UnsignalizedIntersectionDelay.class);
        IntersectionDelay delay = createIntersectionDelay(unsignalized);
        Link link = createLink("intersection", 0.0);
        when(unsignalized.getDelay(link, TIME)).thenReturn(5.0F);
        assertEquals(5.0, delay.calculateCrossingPenalty(link, TIME, VEHICLE_ID), 0.0);
        assertEquals(0.0, delay.calculateCrossingPenalty(link, TIME, VEHICLE_ID), 0.0);
        delay.updateIteration(1);
        assertEquals(5.0, delay.calculateCrossingPenalty(link, TIME, VEHICLE_ID), 0.0);
    }

    @Test
    public void busParametersCanBeReadFromConfig() {
        DelaysConfigGroup config = new DelaysConfigGroup();
        config.addParam("applyTlToBuses", "false");
        config.addParam("applyUnsignalizedToBuses", "false");
        config.addParam("constantBusDelay", "4.5");
        assertEquals(false, config.isApplyTlToBuses());
        assertEquals(false, config.isApplyUnsignalizedToBuses());
        assertEquals(4.5, config.getConstantBusDelay(), 0.0);
    }

    @Test
    public void invalidBusConstantsAreRejected() {
        for (double value : new double[]{-2.0, -0.5, Double.NaN, Double.POSITIVE_INFINITY}) {
            org.junit.Assert.assertThrows(IllegalArgumentException.class,
                    () -> new DelaysConfigGroup().setConstantBusDelay(value));
        }
    }

    private IntersectionDelay createBusDelay(DelaysConfigGroup config, Link link,
                                             boolean signalized, boolean intersection) {
        config.setActivateTl(true);
        config.setActivateUnsignalized(true);
        config.setStartingIteration(0);
        TrafficLightDelay tl = mock(TrafficLightDelay.class);
        when(tl.hasTrafficLight(link)).thenReturn(signalized);
        when(tl.getDelay(link, TIME)).thenReturn(signalized ? 12.0F : TrafficLightDelay.NO_TL);
        UnsignalizedIntersectionDelay unsignalized = mock(UnsignalizedIntersectionDelay.class);
        when(unsignalized.considerLink(link)).thenReturn(intersection);
        when(unsignalized.getDelay(link, TIME)).thenReturn(intersection ? 5.0F : 0.0F);
        TimeBinManager bins = mock(TimeBinManager.class);
        when(bins.getStartTime()).thenReturn(0.0);
        when(bins.getEndTime()).thenReturn(1000.0);
        FlowUtils flowUtils = mock(FlowUtils.class);
        when(flowUtils.isBus(BUS_ID)).thenReturn(true);
        return new IntersectionDelay(config, tl, unsignalized, bins,
                (l, t, id) -> 2.0, flowUtils);
    }

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
