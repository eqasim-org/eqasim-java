package org.eqasim.san_francisco.analysis;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * This TravelTime implementation provides fixed values that can be read from an
 * events file.
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class SerializableTravelTime implements TravelTime {
	private final Map<Id<Link>, List<Double>> data;

	private final double startTime;
	private final double endTime;
	private final double interval;
	private final int numberOfBins;

	private final TravelTime fallback;

	public SerializableTravelTime(double startTime, double endTime, double interval, int numberOfBins,
			Map<Id<Link>, List<Double>> data, TravelTime fallback) {
		this.data = data;
		this.startTime = startTime;
		this.endTime = endTime;
		this.interval = interval;
		this.numberOfBins = numberOfBins;
		this.fallback = fallback;
	}
	
	public double getStartTime() {
		return startTime;
	}
	
	public double getEndTime() {
		return endTime;
	}
	
	public double getInterval() {
		return interval;
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
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		if (data.containsKey(link.getId())) {
			return data.get(link.getId()).get(getIndex(time));
		}

		return fallback.getLinkTravelTime(link, time, person, vehicle);
	}

	static private void printProgress(String subject, int current, int total) {
		System.out.println(String.format("%s %d/%d (%.2f%%)", subject, current, total, 100.0 * current / total));
	}	

	static public SerializableTravelTime readBinary(InputStream inputStream) throws IOException {
		DataInputStream reader = new DataInputStream(inputStream);

		double startTime = reader.readDouble();
		double endTime = reader.readDouble();
		double interval = reader.readDouble();

		int numberOfBins = reader.readInt();
		int numberOfLinks = reader.readInt();

		Map<Id<Link>, List<Double>> data = new HashMap<>();

		int numberOfProcessedLinks = 0;

		for (int i = 0; i < numberOfLinks; i++) {
			Id<Link> linkId = Id.createLinkId(reader.readUTF());
			List<Double> values = new ArrayList<>(numberOfBins);

			for (int k = 0; k < numberOfBins; k++) {
				values.add(reader.readDouble());
			}

			data.put(linkId, values);
			printProgress("Reading binary travel times", ++numberOfProcessedLinks, numberOfLinks);
		}

		return new SerializableTravelTime(startTime, endTime, interval, numberOfBins, data, new FreeSpeedTravelTime());
	}

	static public SerializableTravelTime readFromEvents(double startTime, double endTime, double interval,
			Network network, File eventsPath) {
		int numberOfBins = (int) Math.floor((endTime - startTime) / interval);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		Listener listener = new Listener(network, startTime, endTime, interval, numberOfBins);
		eventsManager.addHandler(listener);

		new MatsimEventsReader(eventsManager).readFile(eventsPath.toString());
		return new SerializableTravelTime(startTime, endTime, interval, numberOfBins, listener.getData(),
				new FreeSpeedTravelTime());
	}

	static public class Listener implements LinkEnterEventHandler, LinkLeaveEventHandler,
			VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
		private final Map<Id<Link>, List<Double>> cumulativeTraversalTimes = new HashMap<>();
		private final Map<Id<Link>, List<Long>> traversalCounts = new HashMap<>();
		private final Map<Id<Vehicle>, Double> pendingTimes = new HashMap<>();

		private final double startTime;
		private final double endTime;
		private final double interval;
		private final int numberOfBins;
		private final Network network;

		public Listener(Network network, double startTime, double endTime, double interval, int numberOfBins) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.interval = interval;
			this.numberOfBins = numberOfBins;
			this.network = network;

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

			cumulativeTraversalTimes.get(linkId).set(index,
					cumulativeTraversalTimes.get(linkId).get(index) + travelTime);
			traversalCounts.get(linkId).set(index, traversalCounts.get(linkId).get(index) + 1);
		}

		public Map<Id<Link>, List<Double>> getData() {
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

			return data;
		}
	}
}
