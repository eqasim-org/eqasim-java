package org.eqasim.core.scenario.routing;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Activity;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class PopulationRouterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingRoutes;
	private final Set<String> modes;
	private final Map<Plan, Set<Activity>> reusedRoutes;

	public PopulationRouterModule(int numberOfThreads, int batchSize, boolean replaceExistingRoute) {
		this(numberOfThreads, batchSize, replaceExistingRoute, Collections.emptySet());
	}

	public PopulationRouterModule(int numberOfThreads, int batchSize, boolean replaceExistingRoutes,
			Set<String> modes) {
		this(numberOfThreads, batchSize, replaceExistingRoutes, modes, Map.of());
	}

	public PopulationRouterModule(int numberOfThreads, int batchSize, boolean replaceExistingRoutes,
			Set<String> modes, Map<Plan, Set<Activity>> reusedRoutes) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.replaceExistingRoutes = replaceExistingRoutes;
		this.modes = modes;
		this.reusedRoutes = reusedRoutes;
	}

	@Override
	public void install() {
		bind(PlanRouter.class);
	}

	@Provides
	public PopulationRouter providePopulationRouter(Provider<PlanRouter> routerProvider) {
		return new PopulationRouter(numberOfThreads, batchSize, replaceExistingRoutes, modes, routerProvider, reusedRoutes);
	}
}
