package org.eqasim.core.simulation.calibration;

import java.io.File;

import org.matsim.core.controler.AbstractModule;

public class CalibrationModule extends AbstractModule {
	@Override
	public void install() {
		CalibrationConfigGroup config = CalibrationConfigGroup.get(getConfig());

		if (config.getEnable()) {
			if (!new File(config.getReferencePath()).exists()) {
				throw new IllegalStateException("Reference path does not exist: " + config.getReferencePath());
			}

			addControlerListenerBinding().to(CalibrationOutputListener.class);
		}
	}
}
