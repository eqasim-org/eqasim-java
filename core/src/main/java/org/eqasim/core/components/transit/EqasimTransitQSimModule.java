package org.eqasim.core.components.transit;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTransitQSimModule extends AbstractQSimModule {
	public static final String COMPONENT_NAME = "EqasimTransitEngine";

	@Override
	protected void configureQSim() {
		if (EqasimConfigGroup.get(getConfig()).getUseScheduleBasedTransport()) {
			addQSimComponentBinding(COMPONENT_NAME).to(EqasimTransitEngine.class);
		}
	}

	@Provides
	@Singleton
	public EqasimTransitEngine provideBaselineTransitEngine(EventsManager eventsManager,
			TransitSchedule transitSchedule, DepartureFinder departureFinder, QSim qsim) {
		return new EqasimTransitEngine(eventsManager, transitSchedule, departureFinder, qsim.getAgentCounter());
	}

	static public void configure(QSimComponentsConfig components, Config config) {
		if (EqasimConfigGroup.get(config).getUseScheduleBasedTransport()) {
			components.removeNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME);
			components.addNamedComponent(COMPONENT_NAME);
		}
	}
}
