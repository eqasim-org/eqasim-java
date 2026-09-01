package org.eqasim.core.components.network_calibration;

import org.eqasim.core.components.network_calibration.cost_calibration.CostCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.AgentAscsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.config.SubpopulationsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreeSpeedCalibrationConfigGroup;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestNetworkCalibrationConfigGroup {
    @Test
    public void testNonEmptyObjectiveOverridesChildActivation() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.getFreeSpeedCalibrationConfigGroup().setActivate(false);
        calibration.getAgentAscsCalibrationConfigGroup().setActivate(true);
        calibration.getSubpopulationsCalibrationConfigGroup().setActivate(true);

        calibration.setObjective("freespeed, subpopulations");

        assertFalse(calibration.getCostCalibrationConfigGroup().isActivated());
        assertTrue(calibration.getFreeSpeedCalibrationConfigGroup().isActivated());
        assertFalse(calibration.getAgentAscsCalibrationConfigGroup().isActivated());
        assertTrue(calibration.getSubpopulationsCalibrationConfigGroup().isActivated());
    }

    @Test
    public void testObjectiveAppliesToParameterSetsCreatedLater() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.setObjective("penalty, agent");

        assertTrue(calibration.getCostCalibrationConfigGroup().isActivated());
        assertFalse(calibration.getFreeSpeedCalibrationConfigGroup().isActivated());
        assertTrue(calibration.getAgentAscsCalibrationConfigGroup().isActivated());
        assertFalse(calibration.getSubpopulationsCalibrationConfigGroup().isActivated());
    }

    @Test
    public void testObjectiveIsReappliedAfterParameterSetsAreParsed() {
        Config config = ConfigUtils.createConfig();
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        config.addModule(calibration);
        calibration.setObjective("freespeed");

        // Simulate child activate parameters being read after the parent objective.
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.getFreeSpeedCalibrationConfigGroup().setActivate(false);
        calibration.applyContext(config);

        assertFalse(calibration.getCostCalibrationConfigGroup().isActivated());
        assertTrue(calibration.getFreeSpeedCalibrationConfigGroup().isActivated());
    }

    @Test
    public void testEmptyObjectiveLeavesChildActivationIndependent() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.setObjective("");
        calibration.getCostCalibrationConfigGroup().setActivate(true);

        assertTrue(calibration.isLinkPenaltyActivated());
        assertFalse(calibration.isFreeSpeedFactorActivated());
    }

    @Test
    public void testActivationAndCalibrationAreIndependentPerComponent() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.getFreeSpeedCalibrationConfigGroup().setActivate(true);
        calibration.getAgentAscsCalibrationConfigGroup().setActivate(true);
        calibration.getSubpopulationsCalibrationConfigGroup().setActivate(true);
        calibration.setCalibrate("freespeed, agent");

        assertTrue(calibration.isLinkPenaltyActivated());
        assertFalse(calibration.isLinkPenaltyCalibrationActivated());
        assertTrue(calibration.isFreeSpeedFactorActivated());
        assertTrue(calibration.isFreeSpeedFactorCalibrationActivated());
        assertTrue(calibration.isAgentAscsActivated());
        assertTrue(calibration.isAgentAscsCalibrationActivated());
        assertTrue(calibration.isSubpopulationsActivated());
        assertFalse(calibration.isSubpopulationsCalibrationActivated());
    }

    @Test
    public void testFixedComponentsDoNotRequireCalibrationInputs() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.setActivate(true);
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.getFreeSpeedCalibrationConfigGroup().setActivate(true);
        calibration.getAgentAscsCalibrationConfigGroup().setActivate(true);
        calibration.getSubpopulationsCalibrationConfigGroup().setActivate(true);
        calibration.setCalibrate("");

        NetworkCalibrationModule.validateConfiguration(calibration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalibratedFlowComponentRequiresCounts() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.setActivate(true);
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.setCalibrate("penalty");

        NetworkCalibrationModule.validateConfiguration(calibration);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownCalibrationComponentIsRejected() {
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        calibration.setActivate(true);
        calibration.getCostCalibrationConfigGroup().setActivate(true);
        calibration.setCalibrate("unknown");

        NetworkCalibrationModule.validateConfiguration(calibration);
    }

    @Test
    public void testChildParametersSurviveConfigRoundTrip() throws Exception {
        Config config = ConfigUtils.createConfig();
        NetworkCalibrationConfigGroup calibration = new NetworkCalibrationConfigGroup();
        config.addModule(calibration);
        calibration.setActivate(true);
        calibration.setCalibrate("penalty, agent");
        calibration.setCountsFile("counts.csv");

        CostCalibrationConfigGroup cost = calibration.getCostCalibrationConfigGroup();
        cost.setActivate(true);
        cost.setUpdateInterval(3);
        cost.setMaximumPenaltyUpdate(0.04);

        FreeSpeedCalibrationConfigGroup freespeed = calibration.getFreeSpeedCalibrationConfigGroup();
        freespeed.setActivate(true);
        freespeed.setUpdateInterval(7);
        freespeed.setMaxDistanceError(0.15);

        AgentAscsCalibrationConfigGroup agent = calibration.getAgentAscsCalibrationConfigGroup();
        agent.setActivate(true);
        agent.setUpdateInterval(11);
        agent.setGridRebuildUpdates("1,3");
        agent.setGridRebuildInitialCellSizes("9000,7000");
        agent.setGridRebuildMinCellSizes("1000,500");
        agent.setGridRebuildMaxPopulations("800,400");

        SubpopulationsCalibrationConfigGroup subpopulations = calibration.getSubpopulationsCalibrationConfigGroup();
        subpopulations.setActivate(true);
        subpopulations.setUpdateInterval(13);
        subpopulations.setCalibrateCrossBorder(false);
        subpopulations.setCrossBorderCloningIterations("12,24");
        subpopulations.setCrossBorderUpdateFraction(0.65);
        assertFalse(subpopulations.getParams().containsKey("parallelism"));

        Path path = Files.createTempFile("network-calibration-config", ".xml");
        try {
            ConfigUtils.writeConfig(config, path.toString());
            Config loadedConfig = ConfigUtils.loadConfig(path.toString(), new NetworkCalibrationConfigGroup());
            NetworkCalibrationConfigGroup loaded = NetworkCalibrationConfigGroup.getOrCreate(loadedConfig);

            assertTrue(loaded.isActivated());
            assertEquals(List.of("penalty", "agent"), loaded.getModulesToBeCalibratedAsList());
            assertEquals(3, loaded.getCostCalibrationConfigGroup().getUpdateInterval());
            assertEquals(0.04, loaded.getCostCalibrationConfigGroup().getMaximumPenaltyUpdate(), 1.0e-12);
            assertEquals(7, loaded.getFreeSpeedCalibrationConfigGroup().getUpdateInterval());
            assertEquals(0.15, loaded.getFreeSpeedCalibrationConfigGroup().getMaxDistanceError(), 1.0e-12);
            assertEquals(11, loaded.getAgentAscsCalibrationConfigGroup().getUpdateInterval());
            assertEquals(List.of(1, 3), loaded.getAgentAscsCalibrationConfigGroup().getGridRebuildUpdates());
            assertEquals(13, loaded.getSubpopulationsCalibrationConfigGroup().getUpdateInterval());
            assertFalse(loaded.getSubpopulationsCalibrationConfigGroup().isCrossBorderCalibrationEnabled());
            assertEquals(List.of(12, 24), loaded.getSubpopulationsCalibrationConfigGroup().getCrossBorderCloningIterations());
            assertEquals(0.65, loaded.getSubpopulationsCalibrationConfigGroup()
                    .getCrossBorderUpdateFraction(), 1.0e-12);
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void testCrossBorderCalibrationIsEnabledByDefault() {
        SubpopulationsCalibrationConfigGroup config = new SubpopulationsCalibrationConfigGroup();
        assertTrue(config.isCrossBorderCalibrationEnabled());
        assertEquals(0.05, config.getFlowUnderEstimationThreshold(), 1.0e-12);
        assertEquals(0.05, config.getFlowOverEstimationThreshold(), 1.0e-12);
    }

}
