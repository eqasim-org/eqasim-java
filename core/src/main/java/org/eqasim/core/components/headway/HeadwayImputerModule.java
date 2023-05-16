package org.eqasim.core.components.headway;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.TripRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provider;
import com.google.inject.Provides;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class HeadwayImputerModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingHeadways;
	private final double interval;
	private final HeadwayType type;

	public HeadwayImputerModule(int numberOfThreads, int batchSize, boolean replaceExistingHeadways, double interval,
			HeadwayType type) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.replaceExistingHeadways = replaceExistingHeadways;
		this.interval = interval;
		this.type = type;
	}

	@Override
	public void install() {
	}

	@Provides
	public HeadwayCalculator provideHeadwayCalculator(SwissRailRaptor raptor, TransitSchedule schedule) {
		if (type.equals(HeadwayType.Interval)) {
			return new IntervalHeadwayCalculator(raptor, interval, interval);
		} else if (type.equals(HeadwayType.Next)) {
			return new NextHeadwayCalculator(raptor, interval);
		} else if (type.equals(HeadwayType.Schedule)) {
			return new ScheduleHeadwayCalculator(raptor, schedule);
		} else {
			throw new IllegalStateException();
		}
	}

	@Provides
	public HeadwayImputer provideHeadwayImputer(Provider<HeadwayCalculator> calculatorProvider, Network network,
			TripRouter tripRouter) {
		return new HeadwayImputer(numberOfThreads, batchSize, replaceExistingHeadways, network, calculatorProvider);
	}
}
