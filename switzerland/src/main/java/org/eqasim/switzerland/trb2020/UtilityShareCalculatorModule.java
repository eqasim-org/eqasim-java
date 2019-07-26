package org.eqasim.switzerland.trb2020;

import com.google.inject.Provider;
import com.google.inject.Provides;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

public class UtilityShareCalculatorModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;

	public UtilityShareCalculatorModule(int numberOfThreads, int batchSize) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}

	@Override
	public void install() {
		bind(PlanRouter.class);
	}

	@Provides
	public UtilityShareCalculator provideUtilityShareCalculator(Provider<TripRouter> routerProvider, ActivityFacilities facilities) {
		return new UtilityShareCalculator(numberOfThreads, batchSize, routerProvider, facilities);
	}
}
