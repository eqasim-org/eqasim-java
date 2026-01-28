package org.sutlab.hannover;

import org.eqasim.core.components.calibration.CalibrationConfigGroup;
import org.eqasim.core.components.calibration.CalibrationModule;
import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibrationModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.sutlab.hannover.mode_choice.HannoverModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class HannoverConfigurator extends EqasimConfigurator {
	public HannoverConfigurator(CommandLine cmd) {
		super(cmd);

		// calibration functionality
		registerConfigGroup(new AlphaCalibratorConfig(), true);
		registerModule(new FastCalibrationModule(), AlphaCalibratorConfig.GROUP_NAME);

		// registerConfigGroup(new CalibrationConfigGroup(), true);
		// registerModule(new CalibrationModule(), CalibrationConfigGroup.GROUP_NAME);

		// registerConfigGroup(new NetworkCalibrationConfigGroup(), true);
		// registerModule(new networkCalibrationModule(), NetworkCalibrationConfigGroup.GROUP_NAME);

		registerModule(new HannoverModeChoiceModule(cmd));
	}
}