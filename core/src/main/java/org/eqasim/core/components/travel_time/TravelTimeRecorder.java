package org.eqasim.core.components.travel_time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

public class TravelTimeRecorder implements LinkEnterEventHandler, LinkLeaveEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private final Map<Id<Link>, List<Double>> cumulativeTraversalTimes = new HashMap<>();
	private final Map<Id<Link>, List<Long>> traversalCounts = new HashMap<>();
	private final Map<Id<Vehicle>, Double> pendingTimes = new HashMap<>();

	private final double startTime;
	private final double endTime;
	private final double interval;
	private final int numberOfBins;
	private final RoadNetwork network;

	public TravelTimeRecorder(RoadNetwork network, double startTime, double endTime, double interval) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.interval = interval;
		this.network = network;

		this.numberOfBins = (int) Math.floor((endTime - startTime) / interval);

		for (Link link : network.getLinks().values()) {
			cumulativeTraversalTimes.put(link.getId(), new ArrayList<>(Collections.nCopies(numberOfBins, 0.0)));
			traversalCounts.put(link.getId(), new ArrayList<>(Collections.nCopies(numberOfBins, 0L)));
		}
	}

	private int getIndex(double time) {
		if (time < startTime) {
			return 0;
		} else if (time >= endTime) {
			return numberOfBins - 1;
		} else {
			return (int) Math.floor((time - startTime) / interval);
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		pendingTimes.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		pendingTimes.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Double enterTime = pendingTimes.remove(event.getVehicleId());

		if (enterTime != null) {
			processTime(event.getLinkId(), enterTime, event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Double enterTime = pendingTimes.remove(event.getVehicleId());

		if (enterTime != null) {
			processTime(event.getLinkId(), enterTime, event.getTime());
		}
	}

	private void processTime(Id<Link> linkId, double enterTime, double exitTime) {
		double travelTime = exitTime - enterTime;
		int index = getIndex(enterTime);

		cumulativeTraversalTimes.get(linkId).set(index, cumulativeTraversalTimes.get(linkId).get(index) + travelTime);
		traversalCounts.get(linkId).set(index, traversalCounts.get(linkId).get(index) + 1);
	}

	public RecordedTravelTime getTravelTime() {
		return getTravelTime(new FreeSpeedTravelTime());
	}

	public RecordedTravelTime getTravelTime(TravelTime fallback) {
		Map<Id<Link>, List<Double>> data = new HashMap<>();

		for (Link link : network.getLinks().values()) {
			List<Double> linkCumulativeTraversalTimes = cumulativeTraversalTimes.get(link.getId());
			List<Long> linkTraversalCounts = traversalCounts.get(link.getId());

			List<Double> values = new ArrayList<>(numberOfBins);
			data.put(link.getId(), values);

			for (int i = 0; i < numberOfBins; i++) {
				if (linkTraversalCounts.get(i) == 0) {
					values.add(Math.max(1.0, link.getLength() / link.getFreespeed()));
				} else {
					values.add(Math.max(1.0, linkCumulativeTraversalTimes.get(i) / linkTraversalCounts.get(i)));
				}
			}

		}

		return new RecordedTravelTime(startTime, endTime, interval, data, fallback);
	}
}
