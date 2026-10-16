package org.eqasim.core.components.travel_time;

import java.util.HashMap;
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
	private final Map<Id<Link>, float[]> averages = new HashMap<>();
	private final Map<Id<Link>, int[]> traversalCounts = new HashMap<>();
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

		this.numberOfBins = RecordedTravelTime.binCount(startTime, endTime, interval);

		for (Link link : network.getLinks().values()) {
			averages.put(link.getId(), new float[numberOfBins]);
			traversalCounts.put(link.getId(), new int[numberOfBins]);
		}
	}

	private int getIndex(double time) {
		if (time < startTime) {
			return 0;
		} else if (time >= endTime) {
			return numberOfBins - 1;
		} else {
			return Math.min(numberOfBins - 1, (int) Math.floor((time - startTime) / interval));
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

        float[] values = averages.get(linkId);
        if (values != null) {
            int[] counts = traversalCounts.get(linkId);
            int count = counts[index];
            if (count == Integer.MAX_VALUE) throw new IllegalStateException("Too many travel-time observations");
            values[index] = (float) (((double) values[index] * count + travelTime) / (count + 1));
            counts[index] = count + 1;
        }
	}

	public RecordedTravelTime getTravelTime() {
		return getTravelTime(new FreeSpeedTravelTime());
	}

	public RecordedTravelTime getTravelTime(TravelTime fallback) {
		Map<Id<Link>, float[]> data = new HashMap<>();

		for (Link link : network.getLinks().values()) {
			float[] linkAverages = averages.get(link.getId());
			int[] linkTraversalCounts = traversalCounts.get(link.getId());

			float[] values = new float[numberOfBins];
			data.put(link.getId(), values);

			for (int i = 0; i < numberOfBins; i++) {
				if (linkTraversalCounts[i] == 0) {
					values[i] = (float) Math.max(1.0, link.getLength() / link.getFreespeed());
				} else {
					values[i] = Math.max(1.0f, linkAverages[i]);
				}
			}

		}

		return new RecordedTravelTime(startTime, endTime, interval, data, fallback);
	}
}
