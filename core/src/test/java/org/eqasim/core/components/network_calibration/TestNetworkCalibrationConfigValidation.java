package org.eqasim.core.components.network_calibration;

import org.junit.Assert;
import org.junit.Test;

public class TestNetworkCalibrationConfigValidation {
    @Test
    public void testFixedPenaltyModeRequiresPenaltyFile() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(false);
        config.setObjective("penalty");

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkCalibrationModule.validateConfiguration(config));
        Assert.assertTrue(error.getMessage().contains("penaltiesFile"));
    }

    @Test
    public void testFixedPenaltyModeAcceptsPenaltyFile() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(false);
        config.setObjective("penalty");
        config.setPenaltiesFile("penalties.csv");

        NetworkCalibrationModule.validateConfiguration(config);
    }

    @Test
    public void testFixedCapacityModeRequiresCapacityFile() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(false);
        config.setObjective("capacity");

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkCalibrationModule.validateConfiguration(config));
        Assert.assertTrue(error.getMessage().contains("capacitiesFile"));
    }

    @Test
    public void testFixedFreespeedModeRequiresFactorsFile() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(false);
        config.setObjective("freespeed");

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkCalibrationModule.validateConfiguration(config));
        Assert.assertTrue(error.getMessage().contains("freespeedFactorsFile"));
    }

    @Test
    public void testCalibratingPenaltyAndCapacityIsRejected() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(true);
        config.setObjective("capacity,penalty");
        config.setAverageCountsPerCategoryFile("counts.csv");

        IllegalArgumentException error = Assert.assertThrows(IllegalArgumentException.class,
                () -> NetworkCalibrationModule.validateConfiguration(config));
        Assert.assertTrue(error.getMessage().contains("Both capacity and penalty"));
    }

    @Test
    public void testCalibratingPenaltyWithCountsIsAccepted() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(true);
        config.setObjective("penalty");
        config.setAverageCountsPerCategoryFile("counts.csv");

        NetworkCalibrationModule.validateConfiguration(config);
    }
}

