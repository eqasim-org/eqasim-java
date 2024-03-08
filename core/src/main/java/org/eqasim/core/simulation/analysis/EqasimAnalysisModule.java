package org.eqasim.core.simulation.analysis;

import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.legs.LegListener;
import org.eqasim.core.analysis.pt.PublicTransportLegListener;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.simulation.analysis.stuck.StuckAnalysisModule;
import org.eqasim.core.simulation.modes.drt.analysis.DrtAnalysisModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(AnalysisOutputListener.class);

		if (getConfig().getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
			install(new DrtAnalysisModule());
		} else {
			// Would be better if there was a way to add the module above as an overriding
			// module from this method.
			// That way we could simply bind the two classes below before the if clause
			bind(DefaultPersonAnalysisFilter.class);
			bind(PersonAnalysisFilter.class).to(DefaultPersonAnalysisFilter.class);
		}

		install(new StuckAnalysisModule());
	}

	@Provides
	@Singleton
	public TripListener provideTripListener(Network network, PersonAnalysisFilter personFilter) {
		return new TripListener(network, personFilter);
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
