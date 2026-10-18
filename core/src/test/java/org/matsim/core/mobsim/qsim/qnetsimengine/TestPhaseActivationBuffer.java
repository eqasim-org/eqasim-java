package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestPhaseActivationBuffer {
    @Test(timeout = 20_000) public void concurrentProducersPublishOnceAndPreserveTheirOrderAcrossPhases() throws Exception {
        var buffer = new PhaseActivationBuffer<Integer>(512);
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (int phase = 0; phase < 300; phase++) {
                var futures = new ArrayList<Future<?>>();
                for (int p = 0; p < 8; p++) {
                    int producer = p;
                    futures.add(pool.submit(() -> {
                        for (int i = 0; i < 64; i++) buffer.add(producer * 64 + i);
                    }));
                }
                // This join is the producer/consumer phase barrier.
                for (var future : futures) future.get();
                assertEquals(512, buffer.size());
                int[] expected = new int[8];
                buffer.drainTo(value -> assertEquals(expected[value / 64]++, value % 64));
                for (int count : expected) assertEquals(64, count);
                assertEquals(0, buffer.size());
            }
        }
        var field = PhaseActivationBuffer.class.getDeclaredField("entries");
        field.setAccessible(true);
        for (Object value : (Object[]) field.get(buffer)) assertNull(value);
    }

    @Test public void invalidOwnershipFailsInsteadOfDroppingActivations() {
        var buffer = new PhaseActivationBuffer<Object>(1);
        buffer.add(new Object());
        assertThrows(IllegalStateException.class, () -> buffer.add(new Object()));
    }
}
