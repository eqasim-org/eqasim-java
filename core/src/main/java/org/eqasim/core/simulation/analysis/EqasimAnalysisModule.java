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
import org.matsim.core.controler.listener.ControlerListener;
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
			//Using a static method as a provider causes it to be called even when the bind is not performed
			//So to avoid the potential problems, we use the provider below
			addControlerListenerBinding().toProvider(new Provider<>() {

				@Inject
				EqasimConfigGroup config;
				@Inject
				MultiModeDrtConfigGroup drtConfig;
				@Inject
				OutputDirectoryHierarchy outputDirectory;
				@Inject
				Network network;
				@Inject
				VehicleRegistry vehicleRegistry;

				@Override
				public ControlerListener get() {
					return new DrtAnalysisListener(config, drtConfig, outputDirectory, network, vehicleRegistry);
				}
			});
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
}
