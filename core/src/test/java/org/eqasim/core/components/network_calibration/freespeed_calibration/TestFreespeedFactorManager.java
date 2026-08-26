package org.eqasim.core.components.network_calibration.freespeed_calibration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Map;

import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.junit.Test;

public class TestFreespeedFactorManager {
    @Test
    public void testSkipWhenTripsAreInsufficient() {
        NetworkCalibrationConfigGroup config = createConfig(2);
        FreespeedFactorManager manager = new FreespeedFactorManager(config, config.getFreeSpeedCalibrationConfigGroup());
        FreespeedCalibrationKey key = new FreespeedCalibrationKey(1, "urban", 0);
        FreespeedFactorManager.GroupStats stats = createConsistentStats();

        manager.updateFactors(Map.of(key, stats), 1);

        FreespeedFactorManager.GroupDiagnostics diagnostics = manager.getDiagnosticsSnapshot().get(key);
        assertNotNull(diagnostics);
        assertEquals(Arrays.asList(
                FreespeedFactorManager.Decision.FIRST,
                FreespeedFactorManager.Decision.SKIPPED_INSUFFICIENT_TRIPS
        ), diagnostics.decisions.toList());
        assertTrue(Double.isNaN(diagnostics.lastErrors.getFromLast(0)));
        assertEquals(1.0, manager.getFactor(key), 1.0e-9);
    }

    @Test
    public void testFreezeRollbackAndResumeAfterNoImprovement() {
        NetworkCalibrationConfigGroup config = createConfig(1);
        FreespeedFactorManager manager = new FreespeedFactorManager(config, config.getFreeSpeedCalibrationConfigGroup());
        FreespeedCalibrationKey key = new FreespeedCalibrationKey(1, "urban", 0);
        FreespeedFactorManager.GroupStats stats = createConsistentStats();

        manager.updateFactors(Map.of(key, stats), 1);
        assertEquals(0.92, manager.getFactor(key), 1.0e-9);

        manager.updateFactors(Map.of(key, stats), 2);
        manager.updateFactors(Map.of(key, stats), 3);
        manager.updateFactors(Map.of(key, stats), 4);

        FreespeedFactorManager.GroupDiagnostics diagnosticsAfterFreeze = manager.getDiagnosticsSnapshot().get(key);
        assertEquals(1.0, manager.getFactor(key), 1.0e-9);
        assertEquals(1, diagnosticsAfterFreeze.frozen);
        assertEquals(3, diagnosticsAfterFreeze.noImprovementStreak);
        assertEquals(FreespeedFactorManager.Decision.SKIPPED_NO_MORE_IMPROVEMENT,
                diagnosticsAfterFreeze.decisions.getFromLast(0));

        manager.updateFactors(Map.of(key, stats), 5);
        manager.updateFactors(Map.of(key, stats), 6);

        FreespeedFactorManager.GroupDiagnostics diagnosticsWhileFrozen = manager.getDiagnosticsSnapshot().get(key);
        assertEquals(3, diagnosticsWhileFrozen.frozen);
        assertEquals(0, diagnosticsWhileFrozen.noImprovementStreak);
        assertTrue(diagnosticsWhileFrozen.lastlyFrozen);
        assertEquals(FreespeedFactorManager.Decision.SKIPPED_FROZEN,
                diagnosticsWhileFrozen.decisions.getFromLast(0));

        manager.updateFactors(Map.of(key, stats), 7);

        FreespeedFactorManager.GroupDiagnostics diagnosticsAfterResume = manager.getDiagnosticsSnapshot().get(key);
        assertEquals(0.92, manager.getFactor(key), 1.0e-9);
        assertEquals(FreespeedFactorManager.Decision.UPDATED, diagnosticsAfterResume.decisions.getFromLast(0));
        assertEquals(0, diagnosticsAfterResume.noImprovementStreak);
        assertFalse(diagnosticsAfterResume.lastlyFrozen);
    }

    private static NetworkCalibrationConfigGroup createConfig(int minTripsPerGroup) {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(true);
        FreeSpeedCalibrationConfigGroup freespeedConfig = config.getFreeSpeedCalibrationConfigGroup();
        freespeedConfig.setActivate(true);
        freespeedConfig.setMinTripsPerGroup(minTripsPerGroup);
        freespeedConfig.setLearningRate(0.5);
        freespeedConfig.setMinFactor(0.7);
        freespeedConfig.setMaxFactor(1.3);
        freespeedConfig.setUnboundedInitialUpdates(0);
        freespeedConfig.setFrozenIterations(3);
        return config;
    }

    private static FreespeedFactorManager.GroupStats createConsistentStats() {
        FreespeedFactorManager.GroupStats stats = new FreespeedFactorManager.GroupStats();
        stats.addStat(10.0, 5.0, 1.0, 100.0, 10.0);
        return stats;
    }
}


