package org.eqasim.switzerland.ch_cmdp.mode_choice;

import static org.junit.Assert.*;
import org.eqasim.switzerland.ch_cmdp.calibration.AlphaCantonCalibrator;
import org.eqasim.switzerland.ch_cmdp.calibration.AlphaClusterCalibrator;
import org.eqasim.switzerland.ch_cmdp.calibration.GlobalAlphaCalibrator;
import org.eqasim.switzerland.ch_cmdp.calibration.KeepCantonAlphaCalibrator;
import org.junit.Test;

public class TestFastCalibrationSelection {
    @Test
    public void moduleSelectsAllFourLevels() {
        assertEquals(GlobalAlphaCalibrator.class, SwissModeChoiceModule.calibrationClass("global"));
        assertEquals(AlphaCantonCalibrator.class, SwissModeChoiceModule.calibrationClass("canton"));
        assertEquals(AlphaClusterCalibrator.class, SwissModeChoiceModule.calibrationClass("cluster"));
        assertEquals(KeepCantonAlphaCalibrator.class,
                SwissModeChoiceModule.calibrationClass("keep_canton:geneva;vaud"));
        assertEquals(KeepCantonAlphaCalibrator.class,
                SwissModeChoiceModule.calibrationClass(" KEEP_CANTON:geneva;vaud "));
    }

    @Test
    public void invalidLevelsAndEmptySelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SwissModeChoiceModule.calibrationClass("keep_canton:"));
        assertThrows(IllegalArgumentException.class, () -> SwissModeChoiceModule.calibrationClass("keep_canton"));
        assertThrows(IllegalArgumentException.class, () -> SwissModeChoiceModule.calibrationClass("other"));
    }
}
