package org.eqasim.core.scenario.cutter;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.components.travel_time.TravelTimeRecorder;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingRegistry;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingRegistryHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public final class CutterEventData {
	private static final Logger log = LogManager.getLogger(CutterEventData.class);

	private final Optional<RecordedTravelTime> travelTime;
	private final LinkTimingRegistry linkTimingRegistry;

	private CutterEventData(Optional<RecordedTravelTime> travelTime, LinkTimingRegistry linkTimingRegistry) {
		this.travelTime = travelTime;
		this.linkTimingRegistry = linkTimingRegistry;
	}

	public Optional<RecordedTravelTime> getTravelTime() {
		return travelTime;
	}

	public LinkTimingRegistry getLinkTimingRegistry() {
		return linkTimingRegistry;
	}

	public static CutterEventData read(Optional<String> eventsPath, RoadNetwork roadNetwork, Network network,
			Config config) {
		LinkTimingRegistry linkTimingRegistry = new LinkTimingRegistry();

		if (eventsPath.isEmpty()) {
			return new CutterEventData(Optional.empty(), linkTimingRegistry);
		}

		double startTime = 0.0;
		double endTime = config.travelTimeCalculator().getMaxTime();
		double interval = config.travelTimeCalculator().getTraveltimeBinSize();
		TravelTimeRecorder travelTimeRecorder = new TravelTimeRecorder(roadNetwork, startTime, endTime, interval);

		log.info("Reading cutter travel times and boundary-crossing times in one event-file pass ...");
		EventsManager eventsManager = EventsUtils.createParallelEventsManager();
		eventsManager.addHandler(travelTimeRecorder);
		eventsManager.addHandler(new LinkTimingRegistryHandler(network, linkTimingRegistry));

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsPath.get());
		eventsManager.finishProcessing();
		log.info("Finished reading cutter event data.");

		return new CutterEventData(Optional.of(travelTimeRecorder.getTravelTime()), linkTimingRegistry);
	}
}
