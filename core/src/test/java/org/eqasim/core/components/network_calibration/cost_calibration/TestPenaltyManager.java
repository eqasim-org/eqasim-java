package org.eqasim.core.components.network_calibration.cost_calibration;

import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestPenaltyManager {
    @Test
    public void testUpdateHasMaximumButNoMinimumStep() {
        NetworkCalibrationConfigGroup config = createConfig();
        PenaltyManager manager = new PenaltyManager(config, config.getCostCalibrationConfigGroup());
        PenaltyGroupKey key = new PenaltyGroupKey(1, true, 0);

        double clippedChange = manager.updatePenalty(key, 1.0, 0.5, 0.1);
        assertEquals(0.1, clippedChange, 1.0e-12);
        assertEquals(0.1, manager.getPenalty(key), 1.0e-12);

        double smallChange = manager.updatePenalty(key, 1.0e-3, 0.1, 0.1);
        assertEquals(1.0e-4, smallChange, 1.0e-12);
        assertEquals(0.1001, manager.getPenalty(key), 1.0e-12);
    }

    @Test
    public void testPenaltyBoundsStillApply() {
        NetworkCalibrationConfigGroup config = createConfig();
        PenaltyManager manager = new PenaltyManager(config, config.getCostCalibrationConfigGroup());
        PenaltyGroupKey key = new PenaltyGroupKey(1, true, 0);

        for (int i = 0; i < 10; i++) {
            manager.updatePenalty(key, 1.0, 0.1, 0.1);
        }
        assertEquals(0.3, manager.getPenalty(key), 1.0e-12);

        for (int i = 0; i < 10; i++) {
            manager.updatePenalty(key, -1.0, 0.1, 0.1);
        }
        assertEquals(-0.1, manager.getPenalty(key), 1.0e-12);
    }

    @Test
    public void testAverageOfLastFourPenalties() {
        NetworkCalibrationConfigGroup config = createConfig();
        config.getCostCalibrationConfigGroup().setMaxPenalty(1.0);
        PenaltyManager manager = new PenaltyManager(config, config.getCostCalibrationConfigGroup());
        PenaltyGroupKey key = new PenaltyGroupKey(1, true, 0);

        for (int i = 0; i < 5; i++) {
            manager.updatePenalty(key, 1.0, 0.5, 0.2);
        }

        assertEquals(0.7, manager.getAverageOfLastFourPenalties(key), 1.0e-12);
    }

    private static NetworkCalibrationConfigGroup createConfig() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(true);
        CostCalibrationConfigGroup costConfig = config.getCostCalibrationConfigGroup();
        costConfig.setActivate(true);
        costConfig.setMinPenalty(-0.1);
        costConfig.setMaxPenalty(0.3);
        return config;
    }
}
