package org.eqasim.core.components.headway;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouter;

import com.google.inject.Provider;
import com.google.inject.Provides;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class HeadwayImputerModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingHeadways;
	private final double interval;

	public HeadwayImputerModule(int numberOfThreads, int batchSize, boolean replaceExistingHeadways, double interval) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.replaceExistingHeadways = replaceExistingHeadways;
		this.interval = interval;
	}

	@Override
	public void install() {
	}

	@Provides
	public HeadwayCalculator provideHeadwayCalculator(SwissRailRaptor raptor) {
		return new HeadwayCalculator(raptor, interval, interval);
	}

	@Provides
	public HeadwayImputer provideHeadwayImputer(Provider<HeadwayCalculator> calculatorProvider, Network network,
			TripRouter tripRouter) {
		return new HeadwayImputer(numberOfThreads, batchSize, replaceExistingHeadways, network, calculatorProvider);
	}
}
