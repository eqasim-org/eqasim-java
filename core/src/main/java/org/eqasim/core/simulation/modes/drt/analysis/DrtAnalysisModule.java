package org.eqasim.core.simulation.modes.drt.analysis;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class DrtAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(DrtAnalysisListener.class);
		bind(VehicleRegistry.class).asEagerSingleton();
		addEventHandlerBinding().to(VehicleRegistry.class);
		bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);
	}

	@Provides
	@Singleton
	public DrtAnalysisListener provideDrtAnalysisListener(EqasimConfigGroup eqasimConfig, MultiModeDrtConfigGroup drtConfig, OutputDirectoryHierarchy outputDirectoryHierarchy, Network network, VehicleRegistry vehicleRegistry) {
		return new DrtAnalysisListener(eqasimConfig, drtConfig, outputDirectoryHierarchy, network, vehicleRegistry);
	}
}
