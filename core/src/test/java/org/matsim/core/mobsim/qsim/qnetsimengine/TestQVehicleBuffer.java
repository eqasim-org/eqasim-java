package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TestQVehicleBuffer {
    @Test public void wrappedAndGrowingQueuesMatchNativeOrderAndBitwiseCapacity() {
        var circular = QVehicleBuffer.create(true);
        var legacy = QVehicleBuffer.create(false);
        var vehicles = new QVehicle[8];
        for (int i = 0; i < vehicles.length; i++) vehicles[i] = mock(QVehicle.class);
        var random = new Random(81275);
        for (int i = 0; i < 100_000; i++) {
            if (circular.size() < 128 && (circular.isEmpty() || random.nextBoolean())) {
                QVehicle vehicle = vehicles[random.nextInt(vehicles.length)];
                double consumption = random.nextDouble() * 3;
                circular.add(vehicle, consumption); legacy.add(vehicle, consumption);
            } else assertSame(legacy.poll(), circular.poll());
            assertSame(legacy.peek(), circular.peek());
            assertEquals(legacy.size(), circular.size());
            double capacity = random.nextDouble() * 100;
            assertEquals(Double.doubleToLongBits(legacy.remainingFlowCapacity(capacity)),
                    Double.doubleToLongBits(circular.remainingFlowCapacity(capacity)));
            if (i % 997 == 0) {
                var actual = new ArrayList<QVehicle>(); circular.forEach(actual::add);
                var expected = new ArrayList<QVehicle>(); legacy.forEach(expected::add);
                assertEquals(expected, actual);
                circular.clear(); legacy.clear();
                assertNull(circular.poll()); assertNull(circular.peek());
            }
        }
    }

    @Test public void reusedBufferDoesNotAllocateOnTheMovementPath() {
        var management = ManagementFactory.getThreadMXBean();
        org.junit.Assume.assumeTrue(management instanceof com.sun.management.ThreadMXBean);
        var bean = (com.sun.management.ThreadMXBean) management;
        org.junit.Assume.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        var circular = QVehicleBuffer.create(true);
        var vehicle = mock(QVehicle.class);
        for (int i = 0; i < 100_000; i++) {
            circular.add(vehicle, 0.25); circular.remainingFlowCapacity(1); circular.poll();
        }
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        double remaining = 0;
        for (int i = 0; i < 100_000; i++) {
            circular.add(vehicle, 0.25);
            remaining += circular.remainingFlowCapacity(1);
            assertSame(vehicle, circular.poll());
        }
        long allocated = bean.getThreadAllocatedBytes(thread) - before;
        assertEquals(75_000, remaining, 0);
        assertTrue("Steady-state buffer operations allocated " + allocated + " bytes", allocated <= 1024);
    }

    @Test public void emptyingReleasesVehicleReferences() throws Exception {
        var circular = QVehicleBuffer.create(true);
        for (int i = 0; i < 37; i++) circular.add(mock(QVehicle.class), i / 7.0);
        while (!circular.isEmpty()) circular.poll();
        // Memory ownership matters: empty buffers must not retain parked/finished
        // vehicles even though their arrays are reused in later simulation ticks.
        for (var field : circular.getClass().getDeclaredFields()) {
            if (field.getType() == QVehicle[].class) {
                field.setAccessible(true);
                for (var vehicle : (QVehicle[]) field.get(circular)) assertNull(vehicle);
            }
        }
        circular.add(mock(QVehicle.class), Double.NaN);
        assertTrue(Double.isNaN(circular.remainingFlowCapacity(1)));
        circular.clear();
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(circular.remainingFlowCapacity(-0.0)));
    }
}
