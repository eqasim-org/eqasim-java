package org.eqasim.scenario.routing;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class PopulationRouterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;

	public PopulationRouterModule(int numberOfThreads, int batchSize) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	@Override
	public void install() {
	}

	@Provides
	public PopulationRouter providePopulationRouter(Provider<PlanRouter> routerProvider) {
		return new PopulationRouter(numberOfThreads, batchSize, routerProvider);
	}

	@Provides
	public PlanRouter providePlanRouter(TripRouter tripRouter, ActivityFacilities facilities) {
		return new PlanRouter(tripRouter, facilities);
	}
}
