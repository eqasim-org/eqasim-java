package org.eqasim.core.components.raptor;

import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;

public class EqasimRaptorModule extends AbstractModule {
	@Override
	public void install() {
	}

	@Provides
	@Singleton
	RaptorParametersForPerson provideRaptorParametersForPerson(EqasimRaptorConfigGroup raptorConfig,
			TransitSchedule schedule) {
		RaptorParameters parameters = EqasimRaptorUtils.createParameters(getConfig(), raptorConfig, schedule);
		return person -> parameters;
	}
}
