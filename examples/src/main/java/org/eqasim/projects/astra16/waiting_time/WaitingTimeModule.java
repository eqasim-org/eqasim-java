package org.eqasim.projects.astra16.waiting_time;

import java.util.Map;

import org.eqasim.projects.astra16.convergence.ConvergenceManager;
import org.eqasim.projects.astra16.service_area.ServiceArea;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.data.AVOperator;
import ch.ethz.matsim.av.waiting_time.WaitingTime;
import ch.ethz.matsim.av.waiting_time.WaitingTimeFactory;

public class WaitingTimeModule extends AbstractModule {
	@Override
	public void install() {
		bind(WaitingTimeFactory.class).to(AstraWaitingTimeFactory.class);

		addControlerListenerBinding().to(WaitingTimeAnalysisListener.class);
		addControlerListenerBinding().to(WaitingTimeComparisonListener.class);
		addEventHandlerBinding().to(WaitingTimeComparisonListener.class);
	}

	@Provides
	@Singleton
	public WaitingTimeWriter provideWaitingTimeWriter(Map<Id<AVOperator>, WaitingTime> waitingTimes,
			ServiceArea serviceArea, Map<Id<AVOperator>, Network> networks, AVConfigGroup config) {
		WaitingTime waitingTime = waitingTimes.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		Network network = networks.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		WaitingTimeConfig waitingTimeConfig = config.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID)
				.getWaitingTimeConfig();

		return new WaitingTimeWriter(waitingTime, serviceArea, network, waitingTimeConfig);
	}

	@Provides
	@Singleton
	public WaitingTimeComparisonListener provideWaitingTimeComparisonListener(
			Map<Id<AVOperator>, WaitingTime> waitingTimes, OutputDirectoryHierarchy outputHierarchy,
			ConvergenceManager convergenceManager) {
		WaitingTime waitingTime = waitingTimes.get(OperatorConfig.DEFAULT_OPERATOR_ID);
		return new WaitingTimeComparisonListener(outputHierarchy, waitingTime, convergenceManager);
	}
}
