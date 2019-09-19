package org.eqasim.core.simulation.calibration;

import org.matsim.core.controler.AbstractModule;

public class CalibrationModule extends AbstractModule {
	@Override
	public void install() {
		CalibrationConfigGroup config = CalibrationConfigGroup.get(getConfig());
		
		if (config.getEnable()) {
			addControlerListenerBinding().to(CalibrationOutputListener.class);
		}
	}
}
