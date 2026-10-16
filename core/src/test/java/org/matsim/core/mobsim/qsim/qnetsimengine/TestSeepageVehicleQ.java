package org.matsim.core.mobsim.qsim.qnetsimengine;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.vehicles.VehicleUtils;

public class TestSeepageVehicleQ {
    private static final Set<String> MODES = Set.of("bike", "scooter");

    @Test public void mixedQueuesMatchNativeIncludingEqualTimesAndChangedExitTimes() {
        compare(741, 5000);
    }

    @Test public void scratchQueuesAreIndependentBetweenWorkers() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 4; i++) {
                int seed = i;
                tasks.add(executor.submit(() -> compare(seed, 1500)));
            }
            for (var task : tasks) task.get();
        }
    }

    @Test public void arrivalRemovalDoesNotReadDetachedDriverAndQueueCanBeReused() {
        var queue = new SeepageVehicleQ(MODES);
        var bike = vehicle(0, "bike", 5);
        queue.add(bike);
        bike.setDriver(null); // Native arrival callbacks may detach the driver before removal.
        assertTrue(queue.remove(bike));
        assertNull(queue.peekSeepageVehicle(100));
        var car = vehicle(1, "car", 1);
        queue.add(car);
        queue.add(vehicle(2, "scooter", 2));
        var iterator = queue.iterator();
        while (iterator.hasNext()) if (iterator.next() != car) iterator.remove();
        assertSame(car, queue.peekSeepageVehicle(100));
        queue.clear();
        queue.addFirst(vehicle(3, "bus", 100));
        queue.add(vehicle(4, "bike", 3));
        assertEquals("4", queue.peekSeepageVehicle(3).getId().toString());
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test public void signedZeroAndNonFiniteExitTimesRetainNativeOrdering() {
        var nativeQueue = new PassingVehicleQ();
        var optimized = new SeepageVehicleQ(MODES);
        double[] exits = { Double.NEGATIVE_INFINITY, -0.0, 0.0, 1, Double.POSITIVE_INFINITY, Double.NaN };
        int id = 0;
        for (double exit : exits) for (String mode : List.of("car", "bike", "scooter")) {
            var vehicle = vehicle(id++, mode, exit);
            nativeQueue.add(vehicle); optimized.add(vehicle);
        }
        for (double now : exits) {
            assertSame(nativeSelection(nativeQueue, now), optimized.peekSeepageVehicle(now));
            assertArrayEquals(nativeQueue.toArray(), optimized.toArray());
        }
    }

    private static void compare(int seed, int operations) {
        Random random = new Random(seed);
        var nativeQueue = new PassingVehicleQ();
        var optimized = new SeepageVehicleQ(MODES);
        List<QVehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < operations; i++) {
            int operation = random.nextInt(10);
            if (vehicles.isEmpty() || operation < 5) {
                String mode = List.of("car", "car", "bus", "bike", "scooter").get(random.nextInt(5));
                var vehicle = vehicle(i, mode, random.nextInt(16));
                if (operation == 0) {
                    nativeQueue.addFirst(vehicle); optimized.addFirst(vehicle);
                } else {
                    nativeQueue.add(vehicle); optimized.add(vehicle);
                }
                vehicles.add(vehicle);
            } else if (operation == 5) {
                // Transit rehandling can update a queued vehicle's exit time in place.
                vehicles.get(random.nextInt(vehicles.size())).setEarliestLinkExitTime(random.nextInt(16));
            } else if (operation == 6) {
                var vehicle = vehicles.remove(random.nextInt(vehicles.size()));
                assertEquals(nativeQueue.remove(vehicle), optimized.remove(vehicle));
            } else if (operation == 7) {
                var vehicle = nativeQueue.poll();
                assertSame(vehicle, optimized.poll()); vehicles.remove(vehicle);
            } else if (operation == 8 && random.nextInt(10) == 0) {
                nativeQueue.clear(); optimized.clear(); vehicles.clear();
            }
            double now = random.nextInt(20) - 2;
            QVehicle expected = nativeSelection(nativeQueue, now);
            assertSame("seed=" + seed + " operation=" + i, expected, optimized.peekSeepageVehicle(now));
            assertSame(expected, optimized.peekSeepageVehicle(now)); // Looking must not change either queue.
            assertArrayEquals(nativeQueue.toArray(), optimized.toArray()); // Preserve native heap iteration order.
            if (expected != null && random.nextBoolean()) {
                assertTrue(nativeQueue.remove(expected)); assertTrue(optimized.remove(expected));
                vehicles.remove(expected);
            }
        }
    }

    /** Original QueueWithBuffer.peekFromVehQueue, before the four-bike restriction. */
    private static QVehicle nativeSelection(PassingVehicleQ queue, double now) {
        QVehicle selected = queue.peek();
        var copy = new PassingVehicleQ();
        copy.addAll(queue);
        while (!copy.isEmpty()) {
            QVehicle vehicle = copy.poll();
            if (vehicle.getEarliestLinkExitTime() <= now && MODES.contains(vehicle.getDriver().getMode())) {
                return vehicle;
            }
        }
        return selected;
    }

    private static QVehicle vehicle(int id, String mode, double exit) {
        var type = VehicleUtils.createVehicleType(Id.create(mode, org.matsim.vehicles.VehicleType.class));
        var vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(id), type));
        MobsimDriverAgent driver = mock(MobsimDriverAgent.class);
        when(driver.getMode()).thenReturn(mode);
        vehicle.setDriver(driver); vehicle.setEarliestLinkExitTime(exit);
        return vehicle;
    }
}
