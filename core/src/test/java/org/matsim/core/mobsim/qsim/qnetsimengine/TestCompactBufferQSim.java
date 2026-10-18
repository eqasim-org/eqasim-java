package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.List;
import java.util.ArrayList;
import org.junit.Test;
import org.matsim.core.config.groups.QSimConfigGroup;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestCompactBufferQSim {
    @Test(timeout = 60_000) public void customFlowCalculatorsRetainPreviousVehicleAndTimeGapInputs() {
        String previous = System.getProperty(QVehicleBuffer.PROPERTY);
        try {
            for (var traffic : QSimConfigGroup.TrafficDynamics.values()) {
                List<String> previousCalls = null, previousEvents = null;
                for (boolean compact : new boolean[]{false, true}) {
                    System.setProperty(QVehicleBuffer.PROPERTY, Boolean.toString(compact));
                    var calls = new ArrayList<String>();
                    // A subclass must retain its override, rather than taking the
                    // exact-class fast path for MATSim's standard calculator.
                    var calculator = new org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.DefaultFlowEfficiencyCalculator() {
                        @Override public double calculateFlowEfficiency(QVehicle vehicle, QVehicle preceding,
                                Double gap, org.matsim.api.core.v01.network.Link link,
                                org.matsim.api.core.v01.Id<org.matsim.lanes.Lane> lane) {
                            assertEquals(preceding == null, gap == null);
                            calls.add(link.getId() + ":" + vehicle.getId() + ":"
                                    + (preceding == null ? "first" : preceding.getId()) + ":" + gap);
                            return super.calculateFlowEfficiency(vehicle, preceding, gap, link, lane)
                                    * (gap == null || gap > 3 ? 1.1 : 0.9);
                        }
                    };
                    var events = TestFastQNetsimEngine.run(1, QSimConfigGroup.LinkDynamics.FIFO,
                            true, traffic, true, calculator);
                    assertFalse(calls.isEmpty());
                    if (previousCalls != null) {
                        assertEquals(previousCalls, calls);
                        assertEquals(previousEvents, events);
                    }
                    previousCalls = calls; previousEvents = events;
                }
            }
        } finally {
            if (previous == null) System.clearProperty(QVehicleBuffer.PROPERTY);
            else System.setProperty(QVehicleBuffer.PROPERTY, previous);
        }
    }

    @Test(timeout = 180_000) public void bufferStoragePreservesAllEventsAcrossTrafficAndCapacityModels() {
        String previous = System.getProperty(QVehicleBuffer.PROPERTY);
        try {
            for (int threads : new int[]{1, 4}) {
                for (var dynamics : QSimConfigGroup.LinkDynamics.values()) {
                    for (var traffic : QSimConfigGroup.TrafficDynamics.values()) {
                        for (boolean fastCapacity : new boolean[]{false, true}) {
                            System.setProperty(QVehicleBuffer.PROPERTY, "false");
                            List<String> reference = TestFastQNetsimEngine.run(threads, dynamics, true, traffic, fastCapacity);
                            System.setProperty(QVehicleBuffer.PROPERTY, "true");
                            assertEquals("threads=" + threads + " dynamics=" + dynamics + " traffic=" + traffic
                                            + " fastCapacity=" + fastCapacity, reference,
                                    TestFastQNetsimEngine.run(threads, dynamics, true, traffic, fastCapacity));
                        }
                    }
                }
            }
        } finally {
            if (previous == null) System.clearProperty(QVehicleBuffer.PROPERTY);
            else System.setProperty(QVehicleBuffer.PROPERTY, previous);
        }
    }
}
