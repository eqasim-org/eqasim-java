package org.eqasim.projects.astra16.waiting_time;

import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.matsim.amodeus.config.AmodeusConfigGroup;
import org.matsim.amodeus.config.modal.WaitingTimeConfig;
import org.matsim.amodeus.waiting_time.WaitingTime;
import org.matsim.amodeus.waiting_time.WaitingTimeFactory;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class WaitingTimeModule extends AbstractDvrpModeModule {
	public WaitingTimeModule() {
		super("av");
	}

	@Override
	public void install() {
		bind(WaitingTimeFactory.class).to(AstraWaitingTimeFactory.class);

		addControlerListenerBinding().to(WaitingTimeAnalysisListener.class);
		addControlerListenerBinding().to(WaitingTimeComparisonListener.class);
		addEventHandlerBinding().to(WaitingTimeComparisonListener.class);
	}

	@Provides
	@Singleton
	public WaitingTimeWriter provideWaitingTimeWriter(@DvrpMode("av") WaitingTime waitingTime, ServiceArea serviceArea,
			@DvrpMode("av") Network network, AmodeusConfigGroup config) {
		WaitingTimeConfig waitingTimeConfig = config.getMode("av").getWaitingTimeEstimationConfig();

		return new WaitingTimeWriter(waitingTime, serviceArea, network, waitingTimeConfig);
	}

	@Provides
	@Singleton
	public WaitingTimeComparisonListener provideWaitingTimeComparisonListener(@DvrpMode("av") WaitingTime waitingTime,
			OutputDirectoryHierarchy outputHierarchy) {
		return new WaitingTimeComparisonListener(outputHierarchy, waitingTime);
	}
}
