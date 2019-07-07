package org.eqasim.core.scenario.cutter.transit;

import java.util.List;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public interface StopSequenceCrossingPointFinder {
	List<StopSequenceCrossingPoint> findCrossingPoints(List<TransitRouteStop> stopSequence);
}
