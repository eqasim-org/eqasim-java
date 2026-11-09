package org.eqasim.core.components.raptor;

import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestTransferPenaltyLookup {
    @Test
    public void matchesNestedMapsIncludingMissingPairsCollisionsAndOverwrites() {
        var config = new RaptorStaticConfig();
        Map<String, Map<String, Double>> expected = new HashMap<>();
        // Aa / BB and their combinations deliberately share String.hashCode().
        String[] modes = {"bus", "rail", "tram", "Aa", "BB", "AaAa", "BBBB", "", null};
        assertFalse(config.isUseModeToModeTransferPenalty());
        check(config, expected, modes);
        Random random = new Random(439);
        for (int i = 0; i < 150; i++) {
            String from = modes[random.nextInt(modes.length)];
            String to = modes[random.nextInt(modes.length)];
            double value = switch (i % 7) {
                case 0 -> -0.0;
                case 1 -> Double.NaN;
                case 2 -> Double.POSITIVE_INFINITY;
                case 3 -> Double.NEGATIVE_INFINITY;
                default -> random.nextDouble() * 10 - 5;
            };
            config.addModeToModeTransferPenalty(from, to, value);
            expected.computeIfAbsent(from, key -> new HashMap<>()).put(to, value);
            assertTrue(config.isUseModeToModeTransferPenalty());
            check(config, expected, modes);
        }
    }

    @Test
    public void immutableLookupCanBeReadByConcurrentRoutingWorkers() throws Exception {
        var config = new RaptorStaticConfig();
        config.addModeToModeTransferPenalty("rail", "bus", 0.218);
        config.addModeToModeTransferPenalty("bus", "rail", 0.177);
        try (var executor = Executors.newFixedThreadPool(6)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int worker = 0; worker < 6; worker++) tasks.add(() -> {
                for (int i = 0; i < 50_000; i++) {
                    assertEquals(0.218, config.getModeToModeTransferPenalty("rail", "bus"), 0);
                    assertEquals(0.177, config.getModeToModeTransferPenalty("bus", "rail"), 0);
                    assertEquals(0.0, config.getModeToModeTransferPenalty("unknown", "rail"), 0);
                }
                return true;
            });
            for (var future : executor.invokeAll(tasks)) assertTrue(future.get());
        }
    }

    private static void check(RaptorStaticConfig config, Map<String, Map<String, Double>> expected,
            String[] modes) {
        for (String from : modes) for (String to : modes) {
            double value = expected.getOrDefault(from, java.util.Collections.emptyMap()).getOrDefault(to, 0.0);
            // Use distinct but equal String objects to detect identity-only errors.
            double actual = config.getModeToModeTransferPenalty(from == null ? null : new String(from),
                    to == null ? null : new String(to));
            assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(actual));
        }
        assertEquals(0.0, config.getModeToModeTransferPenalty("missing", "missing"), 0);
    }
}
