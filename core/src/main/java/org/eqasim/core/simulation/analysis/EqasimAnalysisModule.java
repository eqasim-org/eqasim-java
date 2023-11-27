package org.eqasim.core.simulation.analysis;

import com.google.inject.*;
import org.eqasim.core.analysis.filters.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.filters.DrtPersonAnalysisFilter;
import org.eqasim.core.analysis.legs.LegListener;
import org.eqasim.core.analysis.pt.PublicTransportLegListener;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.analysis.utils.VehicleRegistry;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class EqasimAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(AnalysisOutputListener.class);
		bind(DefaultPersonAnalysisFilter.class);
		if(getConfig().getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
			// Needed by the DrtPersonAnalysisFilter to know which vehicles belong to a DRT fleet
			bind(VehicleRegistry.class).asEagerSingleton();
			addEventHandlerBinding().to(VehicleRegistry.class);
			addControlerListenerBinding().to(DrtAnalysisListener.class);
			// Define filter for trip analysis
			bind(PersonAnalysisFilter.class).to(DrtPersonAnalysisFilter.class);
		} else {
			bind(PersonAnalysisFilter.class).to(DefaultPersonAnalysisFilter.class);
		}
	}

	@Provides
	@Singleton
	public TripListener provideTripListener(Network network, MainModeIdentifier mainModeIdentifier,
			PersonAnalysisFilter personFilter) {
		return new TripListener(network, mainModeIdentifier, personFilter);
	}

	@Provides
	@Singleton
	public LegListener provideLegListener(Network network, PersonAnalysisFilter personFilter) {
		return new LegListener(network, personFilter);
	}

	@Provides
	@Singleton
	public PublicTransportLegListener providePublicTransportListener(Network network, TransitSchedule schedule,
			PersonAnalysisFilter personFilter) {
		return new PublicTransportLegListener(schedule);
	}

	@Provides
	@Singleton
	public DrtAnalysisListener provideDrtAnalysisListener(EqasimConfigGroup config, MultiModeDrtConfigGroup drtConfig, OutputDirectoryHierarchy outputDirectory, Network network, VehicleRegistry vehicleRegistry) {
		return new DrtAnalysisListener(config, drtConfig, outputDirectory, network, vehicleRegistry);
	}
}
