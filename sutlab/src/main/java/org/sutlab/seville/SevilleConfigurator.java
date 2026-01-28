package org.sutlab.seville;

import org.eqasim.core.components.fast_calibration.AlphaCalibratorConfig;
import org.eqasim.core.components.fast_calibration.FastCalibrationModule;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.core.config.CommandLine;
import org.sutlab.seville.mode_choice.SevilleModeChoiceModule;

public class SevilleConfigurator extends EqasimConfigurator {
	public SevilleConfigurator(CommandLine cmd) {
		super(cmd);

		// calibration functionality
		registerConfigGroup(new AlphaCalibratorConfig(), true);
		registerModule(new FastCalibrationModule(), AlphaCalibratorConfig.GROUP_NAME);

		// registerConfigGroup(new CalibrationConfigGroup(), true);
		// registerModule(new CalibrationModule(), CalibrationConfigGroup.GROUP_NAME);

		// registerConfigGroup(new NetworkCalibrationConfigGroup(), true);
		// registerModule(new networkCalibrationModule(), NetworkCalibrationConfigGroup.GROUP_NAME);

		registerModule(new SevilleModeChoiceModule(cmd));
	}
}