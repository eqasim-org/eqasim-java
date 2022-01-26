package org.eqasim.core.scenario.cutter;

import java.util.Optional;

import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;

public class CutterTravelTimeModule extends AbstractModule {
	private final Optional<RecordedTravelTime> travelTime;

	public CutterTravelTimeModule(Optional<RecordedTravelTime> travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public void install() {
		if (travelTime.isPresent()) {
			bind(TravelTime.class).toInstance(travelTime.get());
		}
	}
}