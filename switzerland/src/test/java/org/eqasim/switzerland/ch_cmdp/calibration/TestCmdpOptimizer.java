package org.eqasim.switzerland.ch_cmdp.calibration;

import static org.junit.Assert.*;
import java.nio.file.*;
import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.junit.Test;

public class TestCmdpOptimizer {
    @Test public void keepsLocalCheckoutAndReportsSubprocessFailures() throws Exception {
        Path local = Files.createTempDirectory("local-cmdp-optimizer");
        Files.writeString(local.resolve("run.py"), "local utility changes must remain intact");
        var config = new CalibrationConfigGroup();
        config.setRunCalibration(true);
        config.setOptimizerPath(local.toString());
        config.setStartIteration(0);
        // The Java launcher rejects run.py, giving a deterministic nonzero subprocess exit.
        config.setPythonPath(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        var optimizer = new CmdpOptimizer(config) {
            @Override protected void cloneRepository() { fail("Must not clone over an existing optimizer"); }
            @Override protected void checkoutRepoBranch() { fail("Must not reset an existing optimizer"); }
        };
        assertThrows(IllegalStateException.class, () -> optimizer.run(0,
                local.resolve("output.yml").toString(), "input.yml", "variables"));
        assertEquals("local utility changes must remain intact", Files.readString(local.resolve("run.py")));
    }
}
