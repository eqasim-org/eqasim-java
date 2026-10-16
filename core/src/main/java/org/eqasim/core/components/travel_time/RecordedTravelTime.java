package org.eqasim.core.components.travel_time;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

/**
 * This TravelTime implementation provides fixed values that can be read from an
 * events file.
 * 
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public class RecordedTravelTime implements TravelTime {
	private final Map<Id<Link>, float[]> data;

	private final double startTime;
	private final double endTime;
	private final double interval;
	private final int numberOfBins;

	private final TravelTime fallback;

	RecordedTravelTime(double startTime, double endTime, double interval, Map<Id<Link>, float[]> data,
			TravelTime fallback) {
		this.data = Map.copyOf(data);
		this.startTime = startTime;
		this.endTime = endTime;
		this.interval = interval;
		this.fallback = fallback;

		this.numberOfBins = binCount(startTime, endTime, interval);
	}

	static int binCount(double startTime, double endTime, double interval) {
        if (!Double.isFinite(startTime) || !Double.isFinite(endTime) || !Double.isFinite(interval)
                || interval <= 0 || endTime - startTime < interval
                || (endTime - startTime) / interval > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Travel-time range must contain at least one finite positive bin");
        }
        return (int) Math.floor((endTime - startTime) / interval);
    }

    /** Minimum across stored bins, for admissible routing lower bounds on fixed networks. */
    public double getMinimumLinkTravelTime(Link link) {
        float[] values = data.get(link.getId());
        if (values == null) return fallback.getLinkTravelTime(link, startTime, null, null);
        double minimum = Double.POSITIVE_INFINITY;
        for (float value : values) minimum = Math.min(minimum, value);
        return minimum;
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
			return Math.min(numberOfBins - 1, (int) Math.floor((time - startTime) / interval));
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		float[] values = data.get(link.getId());
		if (values != null) {
			return values[getIndex(time)];
		}

		return fallback.getLinkTravelTime(link, time, person, vehicle);
	}

	static public void writeBinary(String outputPath, RecordedTravelTime travelTime) throws IOException, InterruptedException {
		OutputStream outputStream = IOUtils.getOutputStream(new File(outputPath).toURI().toURL(), false);
		RecordedTravelTime.writeBinary(outputStream, travelTime);
		outputStream.close();
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

		for (Map.Entry<Id<Link>, float[]> item : travelTime.data.entrySet()) {
			writer.writeUTF(item.getKey().toString());
			float[] values = item.getValue();

			for (int k = 0; k < travelTime.numberOfBins; k++) {
				writer.writeDouble(values[k]);
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
        if (numberOfBins != binCount(startTime, endTime, interval) || numberOfLinks < 0)
            throw new IOException("Invalid recorded travel-time dimensions");

		Map<Id<Link>, float[]> data = new HashMap<>();

		ParallelProgress progress = new ParallelProgress("Reading travel time ...", numberOfLinks);
		progress.start();

		for (int i = 0; i < numberOfLinks; i++) {
			Id<Link> linkId = Id.createLinkId(reader.readUTF());
			float[] values = new float[numberOfBins];

			for (int k = 0; k < numberOfBins; k++) {
				values[k] = (float) reader.readDouble();
			}

			data.put(linkId, values);
			progress.update();
		}

        try {
            progress.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while reading travel times");
        }
		return new RecordedTravelTime(startTime, endTime, interval, data, fallback);
	}

	static public RecordedTravelTime readFromEvents(File eventsPath, RoadNetwork network, double startTime,
			double endTime, double interval) {
		return readFromEvents(eventsPath, network, new FreeSpeedTravelTime(), startTime, endTime, interval);
	}

	static public RecordedTravelTime readFromEvents(File eventsPath, RoadNetwork network, Config config) {
		double startTime = 0.0;
		double endTime = config.travelTimeCalculator().getMaxTime();
		double interval = config.travelTimeCalculator().getTraveltimeBinSize();

		return RecordedTravelTime.readFromEvents(eventsPath, network, startTime, endTime, interval);
	}

	static public RecordedTravelTime readFromEvents(File eventsPath, RoadNetwork network, TravelTime fallback,
			double startTime, double endTime, double interval) {

		EventsManager eventsManager = EventsUtils.createEventsManager();

		TravelTimeRecorder recorder = new TravelTimeRecorder(network, startTime, endTime, interval);
		eventsManager.addHandler(recorder);

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsPath.toString());
		eventsManager.finishProcessing();

		return recorder.getTravelTime(fallback);
	}
}
