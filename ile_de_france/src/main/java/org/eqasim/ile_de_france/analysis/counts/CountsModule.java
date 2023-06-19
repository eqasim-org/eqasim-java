package org.eqasim.ile_de_france.analysis.counts;

import java.io.File;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.ile_de_france.analysis.counts.calibration.CalibrationManager;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CountsModule extends AbstractModule {
	private final DailyCounts counts;
	private final boolean useCalibration;

	public CountsModule(CommandLine cmd, boolean useCalibration) {
		this.useCalibration = useCalibration;

		try {
			counts = new CountsReader().read(new File(cmd.getOptionStrict("counts-path")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(CountsListener.class);
	}

	@Provides
	@Singleton
	public CountsListener provideCountsListener(EqasimConfigGroup eqasimConfigGroup, EventsManager eventsManager,
			OutputDirectoryHierarchy outputDirectoryHierarchy, Network network, DailyCounts counts) {
		CalibrationManager calibrationManager = null;

		if (useCalibration) {
			calibrationManager = new CalibrationManager(counts, network, eqasimConfigGroup.getSampleSize());
		}

		return new CountsListener(eqasimConfigGroup, eventsManager, outputDirectoryHierarchy, network, counts,
				calibrationManager);
	}
}
