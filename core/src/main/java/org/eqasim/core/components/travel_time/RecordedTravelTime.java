package org.eqasim.core.components.travel_time;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * This TravelTime implementation provides fixed values that can be read from an
 * events file.
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class RecordedTravelTime implements TravelTime {
	private final Map<Id<Link>, List<Double>> data;

	private final double startTime;
	private final double endTime;
	private final double interval;
	private final int numberOfBins;

	private final TravelTime fallback;

	RecordedTravelTime(double startTime, double endTime, double interval, Map<Id<Link>, List<Double>> data,
			TravelTime fallback) {
		this.data = data;
		this.startTime = startTime;
		this.endTime = endTime;
		this.interval = interval;
		this.fallback = fallback;

		this.numberOfBins = (int) Math.floor((endTime - startTime) / interval);
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

	static public void writeBinary(OutputStream outputStream, RecordedTravelTime travelTime)
			throws IOException, InterruptedException {
		DataOutputStream writer = new DataOutputStream(outputStream);

		writer.writeDouble(travelTime.startTime);
		writer.writeDouble(travelTime.endTime);
		writer.writeDouble(travelTime.interval);

		writer.writeInt(travelTime.numberOfBins);
		writer.writeInt(travelTime.data.size());

		int numberOfLinks = travelTime.data.size();

		ParallelProgress progress = new ParallelProgress("Writing travel time ...", numberOfLinks);
		progress.start();

		for (Map.Entry<Id<Link>, List<Double>> item : travelTime.data.entrySet()) {
			writer.writeUTF(item.getKey().toString());
			List<Double> values = item.getValue();

			for (int k = 0; k < travelTime.numberOfBins; k++) {
				writer.writeDouble(values.get(k));
			}

			progress.update();
		}

		progress.close();
	}

	static public RecordedTravelTime readBinary(InputStream inputStream) throws IOException {
		return readBinary(inputStream, new FreeSpeedTravelTime());
	}

	static public RecordedTravelTime readBinary(InputStream inputStream, TravelTime fallback) throws IOException {
		DataInputStream reader = new DataInputStream(inputStream);

		double startTime = reader.readDouble();
		double endTime = reader.readDouble();
		double interval = reader.readDouble();

		int numberOfBins = reader.readInt();
		int numberOfLinks = reader.readInt();

		Map<Id<Link>, List<Double>> data = new HashMap<>();

		ParallelProgress progress = new ParallelProgress("Writing travel time ...", numberOfLinks);
		progress.start();

		for (int i = 0; i < numberOfLinks; i++) {
			Id<Link> linkId = Id.createLinkId(reader.readUTF());
			List<Double> values = new ArrayList<>(numberOfBins);

			for (int k = 0; k < numberOfBins; k++) {
				values.add(reader.readDouble());
			}

			data.put(linkId, values);
			progress.update();
		}

		return new RecordedTravelTime(startTime, endTime, interval, data, fallback);
	}

	static public RecordedTravelTime readFromEvents(File eventsPath, RoadNetwork network, double startTime,
			double endTime, double interval) {
		return readFromEvents(eventsPath, network, new FreeSpeedTravelTime(), startTime, endTime, interval);
	}

	static public RecordedTravelTime readFromEvents(File eventsPath, RoadNetwork network, TravelTime fallback,
			double startTime, double endTime, double interval) {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		TravelTimeRecorder recorder = new TravelTimeRecorder(network, startTime, endTime, interval);
		eventsManager.addHandler(recorder);

		return recorder.getTravelTime(fallback);
	}
}
