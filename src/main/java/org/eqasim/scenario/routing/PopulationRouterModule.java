package org.eqasim.scenario.routing;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class PopulationRouterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingRoutes;

	public PopulationRouterModule(int numberOfThreads, int batchSize, boolean replaceExistingRoutes) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.replaceExistingRoutes = replaceExistingRoutes;
	}

	@Override
	public void install() {
		bind(PlanRouter.class);
	}

	@Provides
	public PopulationRouter providePopulationRouter(Provider<PlanRouter> routerProvider) {
		return new PopulationRouter(numberOfThreads, batchSize, replaceExistingRoutes, routerProvider);
	}
}
