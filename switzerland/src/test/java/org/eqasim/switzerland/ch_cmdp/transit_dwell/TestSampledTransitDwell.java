package org.eqasim.switzerland.ch_cmdp.transit_dwell;

import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitDriverAgent;
import java.util.ArrayList;
import java.util.List;
import com.google.inject.Guice;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.*;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.Umlauf;
import org.matsim.pt.UmlaufStueckI;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestSampledTransitDwell {
    @Test
    public void suppliedFactoryReceivesScaledTimingsAndDelegateReceivesOriginalArguments() {
        var vehicle = vehicle(VehicleType.DoorOperationMode.parallel, 2.0, 3.0);
        var delegate = mock(TransitStopHandler.class);
        var delegateFactory = mock(TransitStopHandlerFactory.class);
        when(delegateFactory.createTransitStopHandler(any())).thenReturn(delegate);
        var handler = new SampledTransitStopHandler(delegateFactory, vehicle, 0.1);

        var captured = org.mockito.ArgumentCaptor.forClass(Vehicle.class);
        verify(delegateFactory).createTransitStopHandler(captured.capture());
        var timings = captured.getValue();
        assertNotSame(vehicle, timings);
        assertNotSame(vehicle.getType(), timings.getType());
        assertEquals(vehicle.getId(), timings.getId());
        assertEquals(20.0, VehicleUtils.getAccessTime(timings.getType()), 0.0);
        assertEquals(30.0, VehicleUtils.getEgressTime(timings.getType()), 0.0);
        assertEquals(VehicleType.DoorOperationMode.parallel, VehicleUtils.getDoorOperationMode(timings.getType()));
        assertEquals(2.0, VehicleUtils.getAccessTime(vehicle.getType()), 0.0);
        assertEquals(3.0, VehicleUtils.getEgressTime(vehicle.getType()), 0.0);

        var stop = stop("stop");
        var leaving = passengers("leave", 1);
        var entering = passengers("enter", 2);
        var access = mock(PassengerAccessEgress.class);
        var mobsimVehicle = mock(MobsimVehicle.class);
        when(delegate.handleTransitStop(stop, 10.0, leaving, entering, access, mobsimVehicle)).thenReturn(7.0);
        assertEquals(7.0, handler.handleTransitStop(stop, 10.0, leaving, entering, access, mobsimVehicle), 0.0);
        verify(delegate).handleTransitStop(same(stop), eq(10.0), same(leaving), same(entering),
                same(access), same(mobsimVehicle));
    }

    @Test
    public void sampleOnePreservesNativePassengerCallbacksAndTiming() {
        for (var mode : VehicleType.DoorOperationMode.values()) {
            Vehicle vehicle = vehicle(mode, 2.5, 1.5);
            Trace nativeTrace = serve(new ComplexTransitStopHandlerFactory().createTransitStopHandler(vehicle), 3, 2);
            Trace sampledTrace = serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 1.0), 3, 2);
            assertEquals(nativeTrace, sampledTrace);
        }
    }

    @Test
    public void passengerServiceIsScaledButDoorsAreNot() {
        for (var mode : VehicleType.DoorOperationMode.values()) {
            Vehicle vehicle = vehicle(mode, 2.0, 3.0);
            assertEquals(6.0, serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 1.0), 2, 0).duration, 0.0);
            assertEquals(42.0, serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 0.1), 2, 0).duration, 0.0);
            assertEquals(8.0, serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 1.0), 0, 2).duration, 0.0);
            assertEquals(62.0, serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 0.1), 0, 2).duration, 0.0);
            assertEquals(2.0, VehicleUtils.getAccessTime(vehicle.getType()), 0.0);
            assertEquals(3.0, VehicleUtils.getEgressTime(vehicle.getType()), 0.0);
            assertEquals(mode, VehicleUtils.getDoorOperationMode(vehicle.getType()));
        }
    }

    @Test
    public void mixedSerialAndParallelServiceMatchesNativeScaledRates() {
        for (var mode : VehicleType.DoorOperationMode.values()) {
            Trace expected = serve(new ComplexTransitStopHandlerFactory()
                    .createTransitStopHandler(vehicle(mode, 20.0, 30.0)), 3, 2);
            Trace actual = serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle(mode, 2.0, 3.0), 0.1), 3, 2);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void emptyStopsAndZeroServiceTimesKeepNativeBehavior() {
        Vehicle vehicle = vehicle(VehicleType.DoorOperationMode.serial, 0.0, 0.0);
        assertEquals(0.0, serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 0.1), 0, 0).duration, 0.0);
        assertEquals(serve(new ComplexTransitStopHandlerFactory().createTransitStopHandler(vehicle), 2, 1),
                serve(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 0.1), 2, 1));
    }

    @Test
    public void failedBoardingKeepsPassengerForRetryAndFactoryStateIsPerVehicle() {
        var factory = new SampledTransitStopHandlerFactory(0.1);
        Vehicle vehicle = vehicle(VehicleType.DoorOperationMode.serial, 2.0, 3.0);
        TransitStopHandler first = factory.createTransitStopHandler(vehicle);
        TransitStopHandler second = factory.createTransitStopHandler(vehicle);
        assertNotSame(first, second);
        var stop = stop("stop");
        var passenger = mock(PTPassengerAgent.class);
        var entering = new ArrayList<>(List.of(passenger));
        var mobsimVehicle = mock(MobsimVehicle.class);
        var access = mock(PassengerAccessEgress.class);
        when(access.handlePassengerEntering(passenger, mobsimVehicle, stop.getId(), 2.0)).thenReturn(true);
        assertEquals(1.0, first.handleTransitStop(stop, 0.0, new ArrayList<>(), entering, access, mobsimVehicle), 0.0);
        assertEquals(1.0, first.handleTransitStop(stop, 1.0, new ArrayList<>(), entering, access, mobsimVehicle), 0.0);
        assertEquals(1, entering.size());
        first.handleTransitStop(stop, 2.0, new ArrayList<>(), entering, access, mobsimVehicle);
        assertTrue(entering.isEmpty());
        verify(access).handlePassengerEntering(passenger, mobsimVehicle, stop.getId(), 1.0);
        verify(access).handlePassengerEntering(passenger, mobsimVehicle, stop.getId(), 2.0);
        // The other vehicle still needs to open its doors and makes no callback yet.
        var untouchedAccess = mock(PassengerAccessEgress.class);
        second.handleTransitStop(stop, 2.0, new ArrayList<>(), new ArrayList<>(List.of(passenger)), untouchedAccess, mobsimVehicle);
        verifyNoInteractions(untouchedAccess);
    }

    @Test
    public void scheduleHoldingRemainsInNativeDriverAndIsNotScaled() throws Exception {
        checkScheduleHolding(false);
        checkScheduleHolding(true);
    }

    private void checkScheduleHolding(boolean sbb) throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var events = mock(EventsManager.class);
        var qsim = mock(QSim.class);
        when(qsim.getScenario()).thenReturn(scenario);
        when(qsim.getEventsManager()).thenReturn(events);
        var internal = mock(InternalInterface.class);
        when(internal.getMobsim()).thenReturn(qsim);
        var scheduleFactory = scenario.getTransitSchedule().getFactory();
        var first = stop("first");
        var last = stop("last");
        var firstRouteStop = scheduleFactory.createTransitRouteStop(first, 0.0, 50.0);
        firstRouteStop.setAwaitDepartureTime(true);
        var lastRouteStop = scheduleFactory.createTransitRouteStop(last, 100.0, 100.0);
        var networkRoute = RouteUtils.createLinkNetworkRouteImpl(first.getLinkId(), last.getLinkId());
        var route = scheduleFactory.createTransitRoute(Id.create("route", TransitRoute.class), networkRoute,
                List.of(firstRouteStop, lastRouteStop), "bus");
        var line = scheduleFactory.createTransitLine(Id.create("line", TransitLine.class));
        var departure = scheduleFactory.createDeparture(Id.create("departure", Departure.class), 0.0);
        var part = mock(UmlaufStueckI.class);
        when(part.isFahrt()).thenReturn(true);
        when(part.getCarRoute()).thenReturn(networkRoute);
        when(part.getRoute()).thenReturn(route);
        when(part.getLine()).thenReturn(line);
        when(part.getDeparture()).thenReturn(departure);
        var umlauf = mock(Umlauf.class);
        when(umlauf.getId()).thenReturn(Id.create("umlauf", Umlauf.class));
        when(umlauf.getUmlaufStuecke()).thenReturn(List.of(part));
        TransitDriverAgentImpl driver;
        if (sbb) {
            // SBB's constructor and arrival hook are package-private in the installed version.
            var constructor = SBBTransitDriverAgent.class.getDeclaredConstructor(Umlauf.class, String.class,
                    TransitStopAgentTracker.class, InternalInterface.class);
            constructor.setAccessible(true);
            driver = constructor.newInstance(umlauf, "pt", new TransitStopAgentTracker(events), internal);
        } else {
            driver = new TransitDriverAgentImpl(umlauf, "pt", new TransitStopAgentTracker(events), internal);
        }
        var vehicle = vehicle(VehicleType.DoorOperationMode.serial, 2.0, 3.0);
        var transitVehicle = mock(TransitVehicle.class);
        when(transitVehicle.getVehicle()).thenReturn(vehicle);
        when(transitVehicle.getStopHandler()).thenReturn(new SampledTransitStopHandler(new ComplexTransitStopHandlerFactory(), vehicle, 0.1));
        driver.setVehicle(transitVehicle);
        if (sbb) {
            var arrive = SBBTransitDriverAgent.class.getDeclaredMethod("arrive", TransitRouteStop.class, double.class);
            arrive.setAccessible(true);
            arrive.invoke(driver, firstRouteStop, 10.0);
        }
        assertEquals(sbb ? 1.0 : 40.0, driver.handleTransitStop(first, 10.0), 0.0);
        assertEquals(1.0, driver.handleTransitStop(first, 49.0), 0.0);
        assertEquals(0.0, driver.handleTransitStop(first, 50.0), 0.0);
    }

    @Test
    public void qsimOverrideUsesPopulationSampleAndDisabledLeavesFactoryUntouched() {
        for (boolean enabled : new boolean[]{false, true}) {
            Config config = ConfigUtils.createConfig(new EqasimConfigGroup(), new SampledTransitDwellConfigGroup());
            config.transit().setUseTransit(true);
            EqasimConfigGroup.get(config).setSampleSize(0.1);
            ConfigUtils.addOrGetModule(config, SampledTransitDwellConfigGroup.class).setEnabled(enabled);
            var baselineFactory = new ComplexTransitStopHandlerFactory();
            AbstractQSimModule baseline = new AbstractQSimModule() {
                @Override protected void configureQSim() {
                    bind(TransitStopHandlerFactory.class).toInstance(baselineFactory);
                }
            };
            var module = AbstractQSimModule.overrideQSimModules(List.of(baseline), List.of(new SampledTransitDwellQSimModule()));
            module.setConfig(config);
            var injector = Guice.createInjector(binder -> binder.requireExplicitBindings(), module);
            var factory = injector.getInstance(TransitStopHandlerFactory.class);
            if (enabled) {
                assertTrue(factory instanceof SampledTransitStopHandlerFactory);
                assertEquals(42.0, serve(factory.createTransitStopHandler(
                        vehicle(VehicleType.DoorOperationMode.serial, 2.0, 3.0)), 2, 0).duration, 0.0);
            } else {
                assertSame(baselineFactory, factory);
            }
        }
    }

    @Test
    public void invalidSampleFactorsAreRejected() {
        for (double sample : new double[]{0.0, -0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new SampledTransitStopHandlerFactory(sample));
        }
    }

    private record Trace(double duration, List<String> callbacks) { }

    private Trace serve(TransitStopHandler handler, int boarding, int alighting) {
        List<PTPassengerAgent> entering = passengers("enter", boarding);
        List<PTPassengerAgent> leaving = passengers("leave", alighting);
        List<String> callbacks = new ArrayList<>();
        var vehicle = mock(MobsimVehicle.class);
        var stop = stop("stop");
        var access = mock(PassengerAccessEgress.class);
        var stopId = stop.getId();
        var linkId = stop.getLinkId();
        when(access.handlePassengerEntering(any(), same(vehicle), eq(stopId), anyDouble())).thenAnswer(call -> {
            callbacks.add("enter:" + ((PTPassengerAgent) call.getArgument(0)).getId() + ":" + call.getArgument(3));
            return true;
        });
        when(access.handlePassengerLeaving(any(), same(vehicle), eq(linkId), anyDouble())).thenAnswer(call -> {
            callbacks.add("leave:" + ((PTPassengerAgent) call.getArgument(0)).getId() + ":" + call.getArgument(3));
            return true;
        });
        double now = 0.0;
        while (now < 1000.0) {
            double wait = handler.handleTransitStop(stop, now, leaving, entering, access, vehicle);
            if (wait == 0.0) {
                assertTrue(entering.isEmpty());
                assertTrue(leaving.isEmpty());
                assertEquals(boarding + alighting, callbacks.size());
                return new Trace(now, callbacks);
            }
            assertEquals(1.0, wait, 0.0); // Preserve native per-second calls; never scale the returned wait.
            now += wait;
        }
        throw new AssertionError("Stop handler did not finish");
    }

    private List<PTPassengerAgent> passengers(String prefix, int count) {
        List<PTPassengerAgent> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var passenger = mock(PTPassengerAgent.class);
            when(passenger.getId()).thenReturn(Id.createPersonId(prefix + i));
            result.add(passenger);
        }
        return result;
    }

    private TransitStopFacility stop(String id) {
        var stop = mock(TransitStopFacility.class);
        when(stop.getId()).thenReturn(Id.create(id, TransitStopFacility.class));
        when(stop.getLinkId()).thenReturn(Id.createLinkId(id));
        return stop;
    }

    private Vehicle vehicle(VehicleType.DoorOperationMode mode, double access, double egress) {
        var type = VehicleUtils.getFactory().createVehicleType(Id.create("type", VehicleType.class));
        VehicleUtils.setAccessTime(type, access);
        VehicleUtils.setEgressTime(type, egress);
        VehicleUtils.setDoorOperationMode(type, mode);
        return VehicleUtils.createVehicle(Id.createVehicleId("vehicle"), type);
    }
}
