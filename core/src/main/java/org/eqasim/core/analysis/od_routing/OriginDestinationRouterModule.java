package org.eqasim.core.analysis.od_routing;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class OriginDestinationRouterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;

	public OriginDestinationRouterModule(int numberOfThreads, int batchSize) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	@Override
	public void install() {
	}

	@Provides
	public OriginDestinationRouter providePopulationRouter(Provider<TripRouter> routerProvider) {
		return new OriginDestinationRouter(numberOfThreads, batchSize, routerProvider);
	}
}
