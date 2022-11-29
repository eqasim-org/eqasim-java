package org.eqasim.ile_de_france.routing;

import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;

public class IDFRaptorModule extends AbstractModule {
	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public RaptorStaticConfig provideRaptorStaticConfig(TransitSchedule schedule) {
		return IDFRaptorUtils.createRaptorStaticConfig(getConfig(), schedule);
	}

	@Provides
	@Singleton
	public RaptorParameters provideRaptorParameters(TransitSchedule schedule) {
		return IDFRaptorUtils.createRaptorParameters(getConfig(), schedule);
	}

	@Provides
	@Singleton
	public RaptorParametersForPerson provideRaptorParametersForPerson(RaptorParameters parameters) {
		return person -> parameters;
	}
}
