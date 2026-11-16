package org.eqasim.switzerland.ch_cmdp.scenario;

import static org.junit.Assert.*;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.ch_cmdp.walk.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.*;

public class TestAdaptWalkingConfig {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void omittedOptionsPreserveExistingModelAndParameters() throws Exception {
        var config = ConfigUtils.createConfig(new EqasimConfigGroup(), new DiscreteModeChoiceConfigGroup(), new WalkConfigGroup());
        var walking = ConfigUtils.addOrGetModule(config, WalkConfigGroup.class);
        walking.setModel("teleportation"); walking.setK(4);
        var preserved = ConfigUtils.addOrGetModule(adapt(config), WalkConfigGroup.class);
        assertEquals("teleportation", preserved.getModel()); assertEquals(4, preserved.getK(), 0);
        var switched = ConfigUtils.addOrGetModule(adapt(config, "--walk-model", "mobis"), WalkConfigGroup.class);
        assertEquals("mobis", switched.getModel()); assertEquals(4, switched.getK(), 0);
        // A later invocation with a fresh config must not inherit static options from a previous call.
        var fresh = ConfigUtils.addOrGetModule(adapt(ConfigUtils.createConfig(new EqasimConfigGroup(), new DiscreteModeChoiceConfigGroup())), WalkConfigGroup.class);
        assertEquals("mobis", fresh.getModel()); assertEquals(MobisWalkTravelTime.DEFAULT_K, fresh.getK(), 0);
    }

    @Test public void mainWritesWalkingOverridesAfterNormalAdaptation() throws Exception {
        var adapted = adapt(ConfigUtils.createConfig(new EqasimConfigGroup(), new DiscreteModeChoiceConfigGroup()),
                "--walk-model", "mobis", "--walk-v0_m_s", "2", "--walk-k", "1.1",
                "--walk-uphill_penalty", "0.4", "--walk-flat_grade", "0.01", "--walk-detour_factor", "1.2");
        var walking = ConfigUtils.addOrGetModule(adapted, WalkConfigGroup.class);
        assertEquals("mobis", walking.getModel());
        assertEquals(2, walking.getV0MetersPerSecond(), 0); assertEquals(1.1, walking.getK(), 0);
        assertEquals(.4, walking.getUphillPenalty(), 0); assertEquals(.01, walking.getFlatGrade(), 0);
        assertEquals(1.2, walking.getDetourFactor(), 0);
        // Existing adaptations retain their own settings: MOBIS coefficients live only in swissWalking.
        assertEquals(1.3, adapted.routing().getOrCreateModeRoutingParams("walk").getTeleportedModeSpeed(), 0);
        assertEquals(1.3, adapted.routing().getOrCreateModeRoutingParams("walk").getBeelineDistanceFactor(), 0);
    }

    private Config adapt(Config original, String... options) throws Exception {
        var folder = temporary.newFolder();
        var input = new java.io.File(folder, "input.xml"); var output = new java.io.File(folder, "output.xml");
        ConfigUtils.writeConfig(original, input.toString());
        var args = new java.util.ArrayList<>(java.util.List.of("--input-path", input.toString(), "--output-path", output.toString(),
                "--downsamplingRate", "1", "--replanningRate", "0.05", "--prefix", "test_"));
        args.addAll(java.util.List.of(options));
        String prefix = SwissConfigAdapter.prefix;
        double sampling = SwissConfigAdapter.downsamplingRate, replanning = SwissConfigAdapter.replanningRate;
        try {
            RunAdaptConfig.main(args.toArray(String[]::new));
        } finally {
            SwissConfigAdapter.prefix = prefix;
            SwissConfigAdapter.downsamplingRate = sampling; SwissConfigAdapter.replanningRate = replanning;
        }
        return ConfigUtils.loadConfig(output.toString(), new WalkConfigGroup());
    }
}
