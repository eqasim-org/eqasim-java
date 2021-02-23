package org.eqasim.ile_de_france.travel_time;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TravelTimeComparisonModule extends AbstractModule {
	private final double minimumTravelTime;
	private final double minimumDepartureTime;
	private final double maximumDepartureTime;
	private final double convergenceThreshold;
	private final int detailedAnalysisInterval;

	public TravelTimeComparisonModule(double minimumTravelTime, double minimumDepartureTime,
			double maximumDepartureTime, double convergenceThreshold, int detailedAnalysisInterval) {
		this.minimumTravelTime = minimumTravelTime;
		this.minimumDepartureTime = minimumDepartureTime;
		this.maximumDepartureTime = maximumDepartureTime;
		this.convergenceThreshold = convergenceThreshold;
		this.detailedAnalysisInterval = detailedAnalysisInterval;
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(TravelTimeComparisonListener.class);
		addEventHandlerBinding().to(TravelTimeComparisonListener.class);
	}

	@Provides
	@Singleton
	public TravelTimeComparisonListener provideTravelTimeComparisonListener(OutputDirectoryHierarchy outputHierarchy,
			Population population) {
		return new TravelTimeComparisonListener(outputHierarchy, population, minimumTravelTime, minimumDepartureTime,
				maximumDepartureTime, convergenceThreshold, detailedAnalysisInterval);
	}
}
